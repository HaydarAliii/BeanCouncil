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
 * Tek-satır ayar deposunu (key + seçili modeller + başkan) okuyup yazar. Key her zaman
 * {@link SecretCipher} ile şifreli DB'ye yazılır, çözülmüş hali sadece bu servisten dönen
 * {@link AppSettings} üzerinden ve sadece çağıran request'in ömrü boyunca bellekte tutulur.
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

    /** Konsey/model proxy akışlarının çağırdığı, key+model seçimi zorunlu olan okuma. */
    public AppSettings getDecryptedSettingsOrThrow() {
        AppSettingsEntity entity = repository.findSingleton()
                .orElseThrow(() -> new SettingsNotConfiguredException(
                        "Ayarlar henüz yapılandırılmamış. Önce OpenRouter key'inizi girin ve konsey üyelerini seçin."));

        String key = cipher.decrypt(entity.getOpenRouterKeyEncrypted());
        if (key == null || key.isBlank()) {
            throw new SettingsNotConfiguredException("OpenRouter key girilmemiş. Lütfen Ayarlar'dan ekleyin.");
        }

        List<String> modelIds = parseModelIds(entity.getSelectedModelIdsJson());
        if (modelIds.size() < 2) {
            throw new SettingsNotConfiguredException("En az 2 konsey üyesi seçilmeli. Lütfen Ayarlar'dan tamamlayın.");
        }
        String presidentId = entity.getPresidentModelId();
        if (presidentId == null || presidentId.isBlank() || !modelIds.contains(presidentId)) {
            throw new SettingsNotConfiguredException("Konsey başkanı seçili modeller arasından seçilmeli.");
        }

        String tavilyKey = cipher.decrypt(entity.getTavilyKeyEncrypted());
        return new AppSettings(key, modelIds, presidentId, tavilyKey);
    }

    /** Sadece key'in var olup olmadığını kontrol etmek için (ör. /api/models/key çağrısından önce). */
    public Optional<String> getDecryptedKey() {
        return repository.findSingleton()
                .map(AppSettingsEntity::getOpenRouterKeyEncrypted)
                .map(cipher::decrypt)
                .filter(k -> !k.isBlank());
    }

    public SettingsResponse save(SettingsRequest request) {
        if (!request.selectedModelIds().contains(request.presidentModelId())) {
            throw new IllegalArgumentException("Başkan, seçili modeller arasından seçilmeli.");
        }

        AppSettingsEntity entity = repository.findSingleton()
                .orElseGet(() -> new AppSettingsEntity(AppSettingsEntity.SINGLETON_ID));

        if (request.openRouterKey() != null && !request.openRouterKey().isBlank()) {
            entity.setOpenRouterKeyEncrypted(cipher.encrypt(request.openRouterKey()));
        }
        if (request.tavilyKey() != null && !request.tavilyKey().isBlank()) {
            entity.setTavilyKeyEncrypted(cipher.encrypt(request.tavilyKey()));
        }
        // İkisi de boş/null gönderildiyse mevcut şifreli key'ler korunur (write-only alanlar).

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

    /** @return son 4 hane maskelenmiş önizleme, ya da key yoksa {@code null}. */
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
