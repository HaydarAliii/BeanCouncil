package com.llmcouncil.model.dto;

import java.util.List;

/** GET/PUT /api/settings yanıtı — gerçek key ASLA bu DTO üzerinden dönmez. */
public record SettingsResponse(
        boolean hasKey,
        String keyPreview,
        List<String> selectedModelIds,
        String presidentModelId) {
}
