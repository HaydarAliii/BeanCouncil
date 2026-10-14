package com.llmcouncil.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * If {@code threadId} is empty, a new conversation is started; if set, a follow-up is appended
 * to that thread. When {@code webSearch} is true and a Tavily key is configured, council members
 * see current web results as context before answering the question.
 */
public record ChatRequest(@NotBlank String prompt, Long threadId, Boolean webSearch) {
}
