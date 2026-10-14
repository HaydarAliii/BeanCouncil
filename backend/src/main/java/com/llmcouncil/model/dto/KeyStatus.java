package com.llmcouncil.model.dto;

/** Credit/limit status derived from the OpenRouter /key response. */
public record KeyStatus(
        String label,
        Double limit,
        Double usage,
        boolean isFreeTier,
        Double limitRemaining) {
}
