package com.llmcouncil.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Tek kullanıcılık yerel kurulum için tek-satır ayar deposu. {@code id} sabittir (her zaman
 * {@value #SINGLETON_ID}) — IDENTITY DEĞİL, böylece {@code save()} her zaman aynı satırı
 * upsert eder. Model listesi JSON string olarak saklanır (ConversationEntity'nin transcript
 * alanıyla aynı desen).
 */
@Entity
@Table(name = "app_settings")
public class AppSettingsEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    /** Base64(IV ‖ ciphertext ‖ tag) — SecretCipher ile şifreli. Asla plaintext saklanmaz. */
    @Column(columnDefinition = "TEXT")
    private String openRouterKeyEncrypted;

    /** JSON array, örn. ["anthropic/claude-sonnet-5", "openai/gpt-5"] */
    @Column(columnDefinition = "TEXT")
    private String selectedModelIdsJson;

    @Column(columnDefinition = "TEXT")
    private String presidentModelId;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected AppSettingsEntity() {
    }

    public AppSettingsEntity(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getOpenRouterKeyEncrypted() {
        return openRouterKeyEncrypted;
    }

    public void setOpenRouterKeyEncrypted(String openRouterKeyEncrypted) {
        this.openRouterKeyEncrypted = openRouterKeyEncrypted;
    }

    public String getSelectedModelIdsJson() {
        return selectedModelIdsJson;
    }

    public void setSelectedModelIdsJson(String selectedModelIdsJson) {
        this.selectedModelIdsJson = selectedModelIdsJson;
    }

    public String getPresidentModelId() {
        return presidentModelId;
    }

    public void setPresidentModelId(String presidentModelId) {
        this.presidentModelId = presidentModelId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Key'in loglara/istem dışı sızmasını önler — asla ham key'i içermez. */
    @Override
    public String toString() {
        return "AppSettingsEntity{id=%d, hasKey=%s, selectedModelIdsJson=%s, presidentModelId=%s, updatedAt=%s}"
                .formatted(id, openRouterKeyEncrypted != null, selectedModelIdsJson, presidentModelId, updatedAt);
    }
}
