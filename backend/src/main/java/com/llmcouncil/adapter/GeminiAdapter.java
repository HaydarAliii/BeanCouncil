package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LlmProviderAdapter'ın Google Gemini (generateContent) için implementasyonu.
 */
public class GeminiAdapter implements LlmProviderAdapter {

    private static final String PROVIDER_NAME = "gemini";

    private final WebClient webClient;
    private final String model;

    public GeminiAdapter(WebClient.Builder webClientBuilder, String baseUrl, String apiKey, String model) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
        this.model = model;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    @Retry(name = "llmProvider")
    @RateLimiter(name = "llmProvider")
    public CompletableFuture<LlmResponse> generateResponse(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        return webClient.post()
                .uri("/models/{model}:generateContent", model)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractContent)
                .map(content -> LlmResponse.ok(PROVIDER_NAME, content))
                .onErrorResume(ex -> reactor.core.publisher.Mono.just(
                        LlmResponse.failed(PROVIDER_NAME, ex.getMessage())))
                .toFuture();
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> body) {
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
