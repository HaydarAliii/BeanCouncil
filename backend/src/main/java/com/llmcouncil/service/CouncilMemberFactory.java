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
 * ids + OpenRouter key), rather than as fixed @Beans at startup. A member's provider is decided
 * by its model id: ids prefixed with {@code "ollama/"} are routed to the local Ollama server
 * (no key, {@link #OLLAMA_PREFIX} stripped before the request), everything else goes through
 * OpenRouter with the id used as-is. Either way, a member's identity (for transcript and
 * president matching) stays the full model id as selected in Settings.
 */
@Component
public class CouncilMemberFactory {

    private static final String OLLAMA_PREFIX = "ollama/";

    private final WebClient.Builder webClientBuilder;
    private final String openRouterBaseUrl;
    private final String ollamaBaseUrl;

    public CouncilMemberFactory(WebClient.Builder webClientBuilder,
                                 @Value("${llm.providers.openrouter.base-url}") String openRouterBaseUrl,
                                 @Value("${llm.providers.ollama.base-url}") String ollamaBaseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.openRouterBaseUrl = openRouterBaseUrl;
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    /** Builds all members selected in the given settings (including the president). */
    public List<LlmProviderAdapter> buildAllMembers(AppSettings settings) {
        return settings.selectedModelIds().stream()
                .map(modelId -> buildMember(modelId, settings.openRouterKey()))
                .toList();
    }

    private LlmProviderAdapter buildMember(String modelId, String decryptedKey) {
        if (modelId.startsWith(OLLAMA_PREFIX)) {
            String ollamaModel = modelId.substring(OLLAMA_PREFIX.length());
            return new OpenAiCompatibleAdapter(webClientBuilder, ollamaBaseUrl + "/v1", ollamaModel, modelId, null);
        }
        return new OpenAiCompatibleAdapter(webClientBuilder, openRouterBaseUrl, modelId, modelId, decryptedKey);
    }
}
