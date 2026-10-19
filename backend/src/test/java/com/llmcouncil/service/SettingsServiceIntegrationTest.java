package com.llmcouncil.service;

import com.llmcouncil.model.dto.SettingsRequest;
import com.llmcouncil.repository.AppSettingsRepository;
import com.llmcouncil.support.AbstractPostgresTest;
import com.llmcouncil.util.SecretCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SettingsService}'s single-row upsert against a real Postgres
 * (Testcontainers) — complements the pure-Mockito {@code SettingsServiceTest}, which verifies
 * the same logic against a mocked repository.
 */
class SettingsServiceIntegrationTest extends AbstractPostgresTest {

    @Autowired
    AppSettingsRepository repository;

    private SettingsService service;

    @BeforeEach
    void setUp() {
        SecretCipher cipher = new SecretCipher("test-crypto-secret", "0011223344556677");
        service = new SettingsService(repository, cipher, JsonMapper.builder().build());
    }

    @Test
    void save_newSettings_persistsEncryptedKeyAndOneRow() {
        SettingsRequest request = new SettingsRequest(
                "sk-or-real-key", null, List.of("openai/gpt-5", "anthropic/claude-sonnet-5"), "openai/gpt-5");

        service.save(request);

        assertThat(repository.count()).isEqualTo(1);
        String storedCiphertext = repository.findSingleton().orElseThrow().getOpenRouterKeyEncrypted();
        assertThat(storedCiphertext).isNotNull().isNotEqualTo("sk-or-real-key");
        assertThat(service.getDecryptedKey()).contains("sk-or-real-key");
    }

    @Test
    void save_blankKey_preservesExistingKey() {
        service.save(new SettingsRequest(
                "sk-or-original-key", null, List.of("openai/gpt-5", "anthropic/claude-sonnet-5"), "openai/gpt-5"));

        service.save(new SettingsRequest(
                null, null, List.of("openai/gpt-5", "google/gemini-3.5-pro"), "google/gemini-3.5-pro"));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(service.getDecryptedKey()).contains("sk-or-original-key");
        assertThat(service.getResponse().selectedModelIds()).containsExactly("openai/gpt-5", "google/gemini-3.5-pro");
    }

    @Test
    void save_withTavilyKey_persistsBothEncrypted() {
        service.save(new SettingsRequest(
                "sk-or-key", "tvly-key", List.of("openai/gpt-5", "anthropic/claude-sonnet-5"), "openai/gpt-5"));

        var entity = repository.findSingleton().orElseThrow();
        assertThat(entity.getOpenRouterKeyEncrypted()).isNotEqualTo("sk-or-key");
        assertThat(entity.getTavilyKeyEncrypted()).isNotEqualTo("tvly-key");
        assertThat(service.getResponse().hasTavilyKey()).isTrue();
    }
}
