package com.llmcouncil.model.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code threadId} boşsa yeni bir konuşma başlatılır; doluysa o thread'e follow-up eklenir. */
public record ChatRequest(@NotBlank String prompt, Long threadId) {
}
