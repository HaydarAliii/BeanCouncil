package com.llmcouncil.config;

import com.llmcouncil.adapter.ClaudeAdapter;
import com.llmcouncil.adapter.FallbackAdapter;
import com.llmcouncil.adapter.GeminiAdapter;
import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.adapter.OpenAiCompatibleAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Konseyin 4 sabit kimliğini (gpt, gemini, claude, grok) kurar. Her kimlik önce kendi resmi
 * sağlayıcısını (key varsa) dener; o da yoksa OpenRouter üzerinden aynı modele ulaşmayı dener
 * (key varsa). İkisi de yoksa o kimlik hiç kayıt olmaz — hiçbir reverse-engineering/key'siz
 * yedek kullanılmaz, tamamen resmi API'ler üzerinden çalışılır.
 */
@Configuration
public class CouncilProvidersConfig {

    @Bean
    public LlmProviderAdapter gptCouncilMember(
            WebClient.Builder webClientBuilder,
            @Value("${llm.providers.groq.base-url}") String groqBaseUrl,
            @Value("${llm.providers.groq.api-key:}") String groqApiKey,
            @Value("${llm.providers.groq.model}") String groqModel,
            @Value("${llm.providers.openrouter.base-url}") String orBaseUrl,
            @Value("${llm.providers.openrouter.api-key:}") String orApiKey,
            @Value("${llm.providers.openrouter.model-gpt}") String orModel) {
        LlmProviderAdapter official = groqApiKey.isBlank() ? null
                : new OpenAiCompatibleAdapter(webClientBuilder, groqBaseUrl, groqModel, "gpt", groqApiKey);
        LlmProviderAdapter openRouter = orApiKey.isBlank() ? null
                : new OpenAiCompatibleAdapter(webClientBuilder, orBaseUrl, orModel, "gpt", orApiKey);
        return member("gpt", official, openRouter);
    }

    @Bean
    public LlmProviderAdapter geminiCouncilMember(
            WebClient.Builder webClientBuilder,
            @Value("${llm.providers.gemini.base-url}") String geminiBaseUrl,
            @Value("${llm.providers.gemini.api-key:}") String geminiApiKey,
            @Value("${llm.providers.gemini.model}") String geminiModel,
            @Value("${llm.providers.openrouter.base-url}") String orBaseUrl,
            @Value("${llm.providers.openrouter.api-key:}") String orApiKey,
            @Value("${llm.providers.openrouter.model-gemini}") String orModel) {
        LlmProviderAdapter official = geminiApiKey.isBlank() ? null
                : new GeminiAdapter(webClientBuilder, geminiBaseUrl, geminiApiKey, geminiModel);
        LlmProviderAdapter openRouter = orApiKey.isBlank() ? null
                : new OpenAiCompatibleAdapter(webClientBuilder, orBaseUrl, orModel, "gemini", orApiKey);
        return member("gemini", official, openRouter);
    }

    @Bean
    public LlmProviderAdapter claudeCouncilMember(
            WebClient.Builder webClientBuilder,
            @Value("${llm.providers.claude.base-url}") String claudeBaseUrl,
            @Value("${llm.providers.claude.api-key:}") String claudeApiKey,
            @Value("${llm.providers.claude.model}") String claudeModel,
            @Value("${llm.providers.openrouter.base-url}") String orBaseUrl,
            @Value("${llm.providers.openrouter.api-key:}") String orApiKey,
            @Value("${llm.providers.openrouter.model-claude}") String orModel) {
        LlmProviderAdapter official = claudeApiKey.isBlank() ? null
                : new ClaudeAdapter(webClientBuilder, claudeBaseUrl, claudeApiKey, claudeModel);
        LlmProviderAdapter openRouter = orApiKey.isBlank() ? null
                : new OpenAiCompatibleAdapter(webClientBuilder, orBaseUrl, orModel, "claude", orApiKey);
        return member("claude", official, openRouter);
    }

    @Bean
    public LlmProviderAdapter grokCouncilMember(
            WebClient.Builder webClientBuilder,
            @Value("${llm.providers.grok.base-url}") String grokBaseUrl,
            @Value("${llm.providers.grok.api-key:}") String grokApiKey,
            @Value("${llm.providers.grok.model}") String grokModel,
            @Value("${llm.providers.openrouter.base-url}") String orBaseUrl,
            @Value("${llm.providers.openrouter.api-key:}") String orApiKey,
            @Value("${llm.providers.openrouter.model-grok}") String orModel) {
        LlmProviderAdapter official = grokApiKey.isBlank() ? null
                : new OpenAiCompatibleAdapter(webClientBuilder, grokBaseUrl, grokModel, "grok", grokApiKey);
        LlmProviderAdapter openRouter = orApiKey.isBlank() ? null
                : new OpenAiCompatibleAdapter(webClientBuilder, orBaseUrl, orModel, "grok", orApiKey);
        return member("grok", official, openRouter);
    }

    /**
     * @return official+openRouter varsa official birincil; sadece openRouter varsa o birincil;
     * ikisi de yoksa {@code null} (Spring bu @Bean'i hiç kaydetmez, kimlik council'da görünmez).
     */
    private LlmProviderAdapter member(String identity, LlmProviderAdapter official, LlmProviderAdapter openRouter) {
        if (official != null) {
            return new FallbackAdapter(identity, official, openRouter);
        }
        if (openRouter != null) {
            return new FallbackAdapter(identity, openRouter, null);
        }
        return null;
    }
}
