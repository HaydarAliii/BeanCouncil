package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Common contract every LLM provider (Groq, Gemini, reverse API, ...) must implement.
 * The council service calls all registered implementations through this type (Strategy pattern).
 */
public interface LlmProviderAdapter {

    String getProviderName();

    CompletableFuture<LlmResponse> generateResponse(String prompt);
}
