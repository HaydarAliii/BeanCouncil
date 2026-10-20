package com.llmcouncil.service;

import com.llmcouncil.config.WebClientConfig;
import com.llmcouncil.model.dto.ModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Lists models installed on a local Ollama server ({@code GET /api/tags}), exposed to the
 * frontend's model picker alongside the OpenRouter catalog. Ids are prefixed with {@code
 * "ollama/"} (e.g. {@code "ollama/llama3.1"}) — see {@link CouncilMemberFactory} for the
 * matching routing on the other end. Ollama being unreachable (not installed/not running) is a
 * normal, expected condition, not an error: any failure is swallowed and an empty list is
 * returned so the rest of the picker keeps working.
 */
@Service
public class OllamaCatalogService {

    private static final Logger log = LoggerFactory.getLogger(OllamaCatalogService.class);
    private static final String ID_PREFIX = "ollama/";

    /** Short on purpose — an absent local server should fail fast, not stall the Settings screen. */
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final WebClient client;

    public OllamaCatalogService(WebClient.Builder webClientBuilder,
                                 @Value("${llm.providers.ollama.base-url}") String baseUrl) {
        this.client = WebClientConfig.authenticatedClient(webClientBuilder, baseUrl, null);
    }

    @SuppressWarnings("unchecked")
    public List<ModelInfo> listModels() {
        try {
            Map<String, Object> body = client.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(TIMEOUT);

            List<Map<String, Object>> models = (List<Map<String, Object>>) body.get("models");
            return models.stream().map(this::toModelInfo).toList();
        } catch (Exception ex) {
            log.info("Ollama server not reachable at configured base URL ({}); skipping local models.", ex.getMessage());
            return List.of();
        }
    }

    private ModelInfo toModelInfo(Map<String, Object> raw) {
        String name = (String) raw.get("name");
        return new ModelInfo(ID_PREFIX + name, name, null, "0", "0", true);
    }
}
