package com.llmcouncil.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PUT /api/settings body. {@code openRouterKey}/{@code tavilyKey} are optional: if sent
 * null/blank, the existing (already saved) key is kept — both are write-only, so a user who
 * doesn't want to change them never has to re-enter them. {@code tavilyKey} is entirely
 * optional — if omitted, the web search feature is disabled and the council keeps working normally.
 */
public record SettingsRequest(
        String openRouterKey,
        String tavilyKey,
        @NotEmpty(message = "Select at least 2 models") @Size(min = 2, message = "Select at least 2 models")
        List<String> selectedModelIds,
        @NotBlank(message = "A council president must be selected")
        String presidentModelId) {
}
