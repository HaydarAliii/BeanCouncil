package com.llmcouncil.service;

import com.llmcouncil.exception.SettingsNotConfiguredException;
import com.llmcouncil.model.dto.AppSettings;
import com.llmcouncil.model.dto.SettingsRequest;
import com.llmcouncil.model.dto.SettingsResponse;
import com.llmcouncil.model.entity.AppSettingsEntity;
import com.llmcouncil.repository.AppSettingsRepository;
import com.llmcouncil.util.SecretCipher;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes the single-row settings store (key + selected models + president). The key
 * is always written to the DB encrypted with {@link SecretCipher}; the decrypted form is only
 * ever held in memory, returned via this service's {@link AppSettings}, and only for the
 * lifetime of the calling request.
 */
@Service
public class SettingsService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final AppSettingsRepository repository;
    private final SecretCipher cipher;
    private final ObjectMapper objectMapper;

    public SettingsService(AppSettingsRepository repository, SecretCipher cipher, ObjectMapper objectMapper) {
        this.repository = repository;
        this.cipher = cipher;
        this.objectMapper = objectMapper;
    }

    public SettingsResponse getResponse() {
        return toResponse(repository.findSingleton().orElse(null));
    }

    /**
     * Read used by council/model proxy flows, where a model selection is always required but an
     * OpenRouter key is only required if at least one selected model actually routes through
     * OpenRouter (an all-{@code ollama/}-prefixed selection can run fully local, key-free).
     */
    public AppSettings getDecryptedSettingsOrThrow() {
        AppSettingsEntity entity = repository.findSingleton()
                .orElseThrow(() -> new SettingsNotConfiguredException(
                        "Settings haven't been configured yet. Select council members first."));

        String key = cipher.decrypt(entity.getOpenRouterKeyEncrypted());

        List<String> modelIds = parseModelIds(entity.getSelectedModelIdsJson());
        if (modelIds.size() < 2) {
            throw new SettingsNotConfiguredException("At least 2 council members must be selected. Please finish setup in Settings.");
        }
        String presidentId = entity.getPresidentModelId();
        if (presidentId == null || presidentId.isBlank() || !modelIds.contains(presidentId)) {
            throw new SettingsNotConfiguredException("The council president must be chosen from among the selected models.");
        }

        boolean needsOpenRouterKey = modelIds.stream().anyMatch(id -> !id.startsWith("ollama/"));
        if (needsOpenRouterKey && (key == null || key.isBlank())) {
            throw new SettingsNotConfiguredException("OpenRouter key is not set. Please add it in Settings.");
        }

        String tavilyKey = cipher.decrypt(entity.getTavilyKeyEncrypted());
        return new AppSettings(key, modelIds, presidentId, tavilyKey);
    }

    /** Used only to check whether a key exists (e.g. before calling /api/models/key). */
    public Optional<String> getDecryptedKey() {
        return repository.findSingleton()
                .map(AppSettingsEntity::getOpenRouterKeyEncrypted)
                .map(cipher::decrypt)
                .filter(k -> !k.isBlank());
    }

    public SettingsResponse save(SettingsRequest request) {
        if (!request.selectedModelIds().contains(request.presidentModelId())) {
            throw new IllegalArgumentException("The president must be chosen from among the selected models.");
        }

        AppSettingsEntity entity = repository.findSingleton()
                .orElseGet(() -> new AppSettingsEntity(AppSettingsEntity.SINGLETON_ID));

        if (request.openRouterKey() != null && !request.openRouterKey().isBlank()) {
            entity.setOpenRouterKeyEncrypted(cipher.encrypt(request.openRouterKey()));
        }
        if (request.tavilyKey() != null && !request.tavilyKey().isBlank()) {
            entity.setTavilyKeyEncrypted(cipher.encrypt(request.tavilyKey()));
        }
        // If either is sent blank/null, the existing encrypted keys are kept (write-only fields).

        entity.setSelectedModelIdsJson(objectMapper.writeValueAsString(request.selectedModelIds()));
        entity.setPresidentModelId(request.presidentModelId());
        entity.setUpdatedAt(Instant.now());

        return toResponse(repository.save(entity));
    }

    private List<String> parseModelIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, STRING_LIST);
    }

    private SettingsResponse toResponse(AppSettingsEntity entity) {
        if (entity == null) {
            return new SettingsResponse(false, null, false, null, List.of(), null);
        }
        boolean hasKey = entity.getOpenRouterKeyEncrypted() != null;
        boolean hasTavilyKey = entity.getTavilyKeyEncrypted() != null;
        return new SettingsResponse(
                hasKey, maskKey(entity.getOpenRouterKeyEncrypted()),
                hasTavilyKey, maskKey(entity.getTavilyKeyEncrypted()),
                parseModelIds(entity.getSelectedModelIdsJson()), entity.getPresidentModelId());
    }

    /** @return a preview with the last 4 digits masked, or {@code null} if there's no key. */
    private String maskKey(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        String decrypted = cipher.decrypt(encrypted);
        return decrypted != null && decrypted.length() >= 4
                ? "••••" + decrypted.substring(decrypted.length() - 4)
                : "••••";
    }
}
