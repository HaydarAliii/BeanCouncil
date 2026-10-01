package com.llmcouncil.model.dto;

import java.time.Instant;

/** Geçmiş listesi için hafif görünüm — tam transcript'i içermez. */
public record ConversationSummary(Long id, String prompt, Instant createdAt) {
}
