package com.llmcouncil.model.dto;

import java.time.Instant;

/** Geçmiş listesi için hafif thread görünümü — tam transcript'leri içermez. */
public record ConversationSummary(Long id, String title, Instant createdAt, Instant updatedAt, long turnCount) {
}
