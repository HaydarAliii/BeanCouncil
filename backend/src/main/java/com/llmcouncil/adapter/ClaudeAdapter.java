package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LlmProviderAdapter'ın Anthropic (Claude, Messages API) için implementasyonu.
 * Groq/Gemini'den farklı olarak istek/cevap şekli OpenAI formatında değildir.
 */
public class ClaudeAdapter implements LlmProviderAdapter {

    private static final String PROVIDER_NAME = "claude";

    private final WebClient webClient;
    private final String model;

    public ClaudeAdapter(WebClient.Builder webClientBuilder, String baseUrl, String apiKey, String model) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
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
                "model", model,
                "max_tokens", 1024,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
                .uri("/messages")
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
        List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
        return (String) content.get(0).get("text");
    }
}
