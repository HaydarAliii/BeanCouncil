package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LlmProviderAdapter'ın OpenAI-uyumlu chat completions formatını (Groq, xAI, g4f, ...)
 * konuşan herhangi bir sağlayıcı için genel implementasyonu. apiKey null/boş verilirse
 * Authorization header hiç eklenmez (g4f gibi auth gerektirmeyen sağlayıcılar için).
 */
public class OpenAiCompatibleAdapter implements LlmProviderAdapter {

    private final String providerName;
    private final WebClient webClient;
    private final String model;

    public OpenAiCompatibleAdapter(WebClient.Builder webClientBuilder, String baseUrl, String model,
                                    String providerName, String apiKey) {
        WebClient.Builder builder = webClientBuilder.baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder = builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }
        this.webClient = builder.build();
        this.model = model;
        this.providerName = providerName;
    }

    @Override
    public String getProviderName() {
        return providerName;
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
                .map(content -> LlmResponse.ok(providerName, content))
                .onErrorResume(ex -> reactor.core.publisher.Mono.just(
                        LlmResponse.failed(providerName, ex.getMessage())))
                .toFuture();
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> body) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}
