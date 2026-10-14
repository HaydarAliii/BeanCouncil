package com.llmcouncil.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Single-row settings store for a single-user local setup. {@code id} is fixed (always
 * {@value #SINGLETON_ID}) — NOT IDENTITY, so {@code save()} always upserts the same row.
 * The model list is stored as a JSON string (same pattern as ConversationEntity's transcript
 * field).
 */
@Entity
@Table(name = "app_settings")
public class AppSettingsEntity {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    /** Base64(IV ‖ ciphertext ‖ tag) — encrypted with SecretCipher. Plaintext is never stored. */
    @Column(columnDefinition = "TEXT")
    private String openRouterKeyEncrypted;

    /** Optional key for web search (Tavily), encrypted the same way. If blank, the feature is disabled. */
    @Column(columnDefinition = "TEXT")
    private String tavilyKeyEncrypted;

    /** JSON array, e.g. ["anthropic/claude-sonnet-5", "openai/gpt-5"] */
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

    public String getOpenRouterKeyEncrypted() {
        return openRouterKeyEncrypted;
    }

    public void setOpenRouterKeyEncrypted(String openRouterKeyEncrypted) {
        this.openRouterKeyEncrypted = openRouterKeyEncrypted;
    }

    public String getTavilyKeyEncrypted() {
        return tavilyKeyEncrypted;
    }

    public void setTavilyKeyEncrypted(String tavilyKeyEncrypted) {
        this.tavilyKeyEncrypted = tavilyKeyEncrypted;
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

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** Keeps the key out of logs/accidental leaks — never includes the raw key. */
    @Override
    public String toString() {
        return "AppSettingsEntity{id=%d, hasKey=%s, hasTavilyKey=%s, selectedModelIdsJson=%s, presidentModelId=%s, updatedAt=%s}"
                .formatted(id, openRouterKeyEncrypted != null, tavilyKeyEncrypted != null, selectedModelIdsJson, presidentModelId, updatedAt);
    }
}
