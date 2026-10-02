package com.llmcouncil.service;

import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.adapter.OpenAiCompatibleAdapter;
import com.llmcouncil.model.dto.AppSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Konsey üyelerini artık startup'ta sabit @Bean'ler olarak değil, kullanıcının kayıtlı ayarlarına
 * (seçili model id'leri + OpenRouter key) göre istek zamanında kurar. Tüm üyeler tek bir sağlayıcı
 * (OpenRouter) üzerinden gider; üyenin kimliği doğrudan OpenRouter model id'sidir (ör.
 * "anthropic/claude-sonnet-5"), böylece transcript ve başkan eşleştirmesi model id üzerinden yapılır.
 */
@Component
public class CouncilMemberFactory {

    private final WebClient.Builder webClientBuilder;
    private final String openRouterBaseUrl;

    public CouncilMemberFactory(WebClient.Builder webClientBuilder,
                                 @Value("${llm.providers.openrouter.base-url}") String openRouterBaseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.openRouterBaseUrl = openRouterBaseUrl;
    }

    /** Verilen ayarlardaki tüm seçili üyeleri (başkan dahil) kurar. */
    public List<LlmProviderAdapter> buildAllMembers(AppSettings settings) {
        return settings.selectedModelIds().stream()
                .map(modelId -> buildMember(modelId, settings.openRouterKey()))
                .toList();
    }

    private LlmProviderAdapter buildMember(String modelId, String decryptedKey) {
        return new OpenAiCompatibleAdapter(webClientBuilder, openRouterBaseUrl, modelId, modelId, decryptedKey);
    }
}
