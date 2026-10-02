package com.llmcouncil.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * PUT /api/settings gövdesi. {@code openRouterKey}/{@code tavilyKey} opsiyoneldir: null/boş
 * gönderilirse mevcut (zaten kayıtlı) key korunur — her ikisi de write-only'dir, güncellemek
 * istemeyen kullanıcı tekrar girmek zorunda kalmaz. {@code tavilyKey} tamamen opsiyoneldir —
 * girilmezse web araştırması özelliği devre dışı kalır, konsey normal çalışmaya devam eder.
 */
public record SettingsRequest(
        String openRouterKey,
        String tavilyKey,
        @NotEmpty(message = "En az 2 model seçilmeli") @Size(min = 2, message = "En az 2 model seçilmeli")
        List<String> selectedModelIds,
        @NotBlank(message = "Konsey başkanı seçilmeli")
        String presidentModelId) {
}
