package com.llmcouncil.model.dto;

/** View reduced from OpenRouter /models down to the fields the frontend's model picker needs. */
public record ModelInfo(
        String id,
        String name,
        Long contextLength,
        String promptPrice,
        String completionPrice,
        boolean isFree) {
}
