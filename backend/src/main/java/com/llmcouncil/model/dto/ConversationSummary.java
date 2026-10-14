package com.llmcouncil.model.dto;

import java.time.Instant;

/** Lightweight thread view for the history list — does not include full transcripts. */
public record ConversationSummary(Long id, String title, Instant createdAt, Instant updatedAt, long turnCount) {
}
