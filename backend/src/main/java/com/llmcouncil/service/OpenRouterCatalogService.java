package com.llmcouncil.service;

import com.llmcouncil.config.WebClientConfig;
import com.llmcouncil.exception.OpenRouterUnauthorizedException;
import com.llmcouncil.exception.SettingsNotConfiguredException;
import com.llmcouncil.model.dto.KeyStatus;
import com.llmcouncil.model.dto.ModelInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenRouter'ın {@code /models} (katalog, public) ve {@code /key} (kayıtlı key'in kredi/limit
 * durumu) endpoint'lerini frontend'in model seçici ekranı için sadeleştirip proxy'ler.
 */
@Service
public class OpenRouterCatalogService {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final WebClient.Builder webClientBuilder;
    private final SettingsService settingsService;
    private final String baseUrl;

    public OpenRouterCatalogService(WebClient.Builder webClientBuilder,
                                     SettingsService settingsService,
                                     @Value("${llm.providers.openrouter.base-url}") String baseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.settingsService = settingsService;
        this.baseUrl = baseUrl;
    }

    @SuppressWarnings("unchecked")
    public List<ModelInfo> listModels() {
        Map<String, Object> body = client(null).get()
                .uri("/models")
                .retrieve()
                .bodyToMono(Map.class)
                .block(TIMEOUT);

        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        return data.stream().map(this::toModelInfo).toList();
    }

    @SuppressWarnings("unchecked")
    public KeyStatus keyStatus() {
        String key = settingsService.getDecryptedKey()
                .orElseThrow(() -> new SettingsNotConfiguredException("OpenRouter key girilmemiş. Lütfen Ayarlar'dan ekleyin."));

        Map<String, Object> body;
        try {
            body = client(key).get()
                    .uri("/key")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(TIMEOUT);
        } catch (WebClientResponseException.Unauthorized ex) {
            throw new OpenRouterUnauthorizedException("Kayıtlı OpenRouter key geçersiz veya süresi dolmuş.");
        }

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Double limit = asDouble(data.get("limit"));
        Double usage = asDouble(data.get("usage"));
        Double remaining = (limit != null && usage != null) ? limit - usage : null;
        boolean isFreeTier = Boolean.TRUE.equals(data.get("is_free_tier"));
        return new KeyStatus((String) data.get("label"), limit, usage, isFreeTier, remaining);
    }

    @SuppressWarnings("unchecked")
    private ModelInfo toModelInfo(Map<String, Object> raw) {
        Map<String, Object> pricing = (Map<String, Object>) raw.getOrDefault("pricing", Map.of());
        String promptPrice = String.valueOf(pricing.get("prompt"));
        String completionPrice = String.valueOf(pricing.get("completion"));
        boolean isFree = isZero(promptPrice) && isZero(completionPrice);
        Object contextLength = raw.get("context_length");
        return new ModelInfo(
                (String) raw.get("id"),
                (String) raw.get("name"),
                contextLength == null ? null : ((Number) contextLength).longValue(),
                promptPrice,
                completionPrice,
                isFree);
    }

    private boolean isZero(String price) {
        try {
            return price != null && Double.parseDouble(price) == 0.0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private Double asDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    /** OpenRouter'a tek seferlik, request-scope'lu bir client. key null ise Authorization header eklenmez (public /models için yeterli). */
    private WebClient client(String key) {
        return WebClientConfig.authenticatedClient(webClientBuilder, baseUrl, key);
    }
}
