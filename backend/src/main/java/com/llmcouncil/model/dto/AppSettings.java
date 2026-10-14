package com.llmcouncil.model.dto;

import java.util.List;

/**
 * Decrypted settings view — held in memory only for the lifetime of the request, never
 * written to the DB or returned in an API response as-is.
 */
public record AppSettings(String openRouterKey, List<String> selectedModelIds, String presidentModelId,
                           String tavilyKey) {

    public boolean hasKey() {
        return openRouterKey != null && !openRouterKey.isBlank();
    }

    public boolean hasTavilyKey() {
        return tavilyKey != null && !tavilyKey.isBlank();
    }
}
