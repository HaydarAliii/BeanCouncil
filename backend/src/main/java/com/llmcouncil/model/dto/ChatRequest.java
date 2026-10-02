package com.llmcouncil.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code threadId} boşsa yeni bir konuşma başlatılır; doluysa o thread'e follow-up eklenir.
 * {@code webSearch} true ve Tavily key kayıtlıysa, konsey üyeleri soruyu yanıtlamadan önce
 * güncel web sonuçlarını bağlam olarak görür.
 */
public record ChatRequest(@NotBlank String prompt, Long threadId, Boolean webSearch) {
}
