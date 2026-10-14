package com.llmcouncil.model.dto;

import java.util.List;

/** GET/PUT /api/settings response — the actual keys are NEVER returned through this DTO. */
public record SettingsResponse(
        boolean hasKey,
        String keyPreview,
        boolean hasTavilyKey,
        String tavilyKeyPreview,
        List<String> selectedModelIds,
        String presidentModelId) {
}
