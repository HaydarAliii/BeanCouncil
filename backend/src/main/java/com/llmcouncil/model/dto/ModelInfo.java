package com.llmcouncil.model.dto;

/** OpenRouter /models'tan frontend'in model seçici için ihtiyaç duyduğu alanlara indirgenmiş görünüm. */
public record ModelInfo(
        String id,
        String name,
        Long contextLength,
        String promptPrice,
        String completionPrice,
        boolean isFree) {
}
