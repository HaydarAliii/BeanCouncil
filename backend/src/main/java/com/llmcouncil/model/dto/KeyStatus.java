package com.llmcouncil.model.dto;

/** OpenRouter /key yanıtından türetilmiş kredi/limit durumu. */
public record KeyStatus(
        String label,
        Double limit,
        Double usage,
        boolean isFreeTier,
        Double limitRemaining) {
}
