package com.llmcouncil.service;

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
 * Tavily arama API'si üzerinden konseye güncel/gerçek web sonuçları sağlar. Tamamen opsiyonel —
 * key yoksa veya çağrı başarısız olursa boş liste döner, konsey akışını ASLA bozmaz (arama,
 * cevap kalitesini artıran bir ek; olmazsa olmaz değil).
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

            Map<String, Object> response = webClientBuilder.clone()
                    .baseUrl(baseUrl)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build()
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
            log.warn("Tavily web araması başarısız oldu, aramasız devam ediliyor: {}", ex.getMessage());
            return List.of();
        }
    }
}
