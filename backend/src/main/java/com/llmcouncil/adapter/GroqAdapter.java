package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LlmProviderAdapter'ın Groq (OpenAI uyumlu chat completions) için örnek implementasyonu.
 * Diğer sağlayıcılar (Gemini, ReverseApiAdapter, ...) aynı şablonu izleyerek eklenir.
 */
@Component
public class GroqAdapter implements LlmProviderAdapter {

    private static final String PROVIDER_NAME = "groq";

    private final WebClient webClient;
    private final String model;

    public GroqAdapter(WebClient.Builder webClientBuilder,
                        @Value("${llm.providers.groq.base-url}") String baseUrl,
                        @Value("${llm.providers.groq.api-key}") String apiKey,
                        @Value("${llm.providers.groq.model}") String model) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
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
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        return webClient.post()
                .uri("/chat/completions")
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
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
