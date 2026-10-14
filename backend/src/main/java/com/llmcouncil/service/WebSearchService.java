package com.llmcouncil.service;

import com.llmcouncil.config.WebClientConfig;
import com.llmcouncil.model.dto.WebSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Supplies current, real web results to the council via the Tavily search API. Entirely
 * optional — if there's no key or the call fails, it returns an empty list and NEVER breaks
 * the council flow (search is a quality-of-answer bonus, not a hard requirement).
 */
@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_RESULTS = 5;

    private final WebClient.Builder webClientBuilder;
    private final String baseUrl;

    public WebSearchService(WebClient.Builder webClientBuilder,
                             @Value("${llm.providers.tavily.base-url}") String baseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.baseUrl = baseUrl;
    }

    @SuppressWarnings("unchecked")
    public List<WebSearchResult> search(String query, String apiKey) {
        try {
            Map<String, Object> body = Map.of(
                    "query", query,
                    "max_results", MAX_RESULTS,
                    "search_depth", "basic");

            Map<String, Object> response = WebClientConfig.authenticatedClient(webClientBuilder, baseUrl, apiKey)
                    .post()
                    .uri("/search")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(TIMEOUT);

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getOrDefault("results", List.of());
            return results.stream()
                    .map(r -> new WebSearchResult((String) r.get("title"), (String) r.get("url"), (String) r.get("content")))
                    .toList();
        } catch (Exception ex) {
            log.warn("Tavily web search failed, continuing without search results: {}", ex.getMessage());
            return List.of();
        }
    }
}
