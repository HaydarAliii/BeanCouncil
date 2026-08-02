package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Her LLM sağlayıcısının (Groq, Gemini, Reverse API, ...) implemente etmesi gereken ortak sözleşme.
 * Konsey servisi, kayıtlı tüm implementasyonları bu tip üzerinden çağırır (Strategy pattern).
 */
public interface LlmProviderAdapter {

    String getProviderName();

    CompletableFuture<LlmResponse> generateResponse(String prompt);
}
