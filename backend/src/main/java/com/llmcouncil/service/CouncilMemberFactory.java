package com.llmcouncil.service;

import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.adapter.OpenAiCompatibleAdapter;
import com.llmcouncil.model.dto.AppSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Builds council members at request time based on the user's saved settings (selected model
 * ids + OpenRouter key), rather than as fixed @Beans at startup. All members go through a single
 * provider (OpenRouter); a member's identity is directly its OpenRouter model id (e.g.
 * "anthropic/claude-sonnet-5"), so transcript and president matching are done by model id.
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

    /** Builds all members selected in the given settings (including the president). */
    public List<LlmProviderAdapter> buildAllMembers(AppSettings settings) {
        return settings.selectedModelIds().stream()
                .map(modelId -> buildMember(modelId, settings.openRouterKey()))
                .toList();
    }

    private LlmProviderAdapter buildMember(String modelId, String decryptedKey) {
        return new OpenAiCompatibleAdapter(webClientBuilder, openRouterBaseUrl, modelId, modelId, decryptedKey);
    }
}
