package com.llmcouncil.model.dto;

import java.util.List;

/** GET/PUT /api/settings yanıtı — gerçek key'ler ASLA bu DTO üzerinden dönmez. */
public record SettingsResponse(
        boolean hasKey,
        String keyPreview,
        boolean hasTavilyKey,
        String tavilyKeyPreview,
        List<String> selectedModelIds,
        String presidentModelId) {
}
