package com.llmcouncil.model.dto;

import java.util.List;

/**
 * Çözülmüş (decrypted) ayar görünümü — sadece request ömrü boyunca bellekte tutulur, asla
 * DB'ye ya da API response'una bu haliyle yazılmaz/dönülmez.
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
