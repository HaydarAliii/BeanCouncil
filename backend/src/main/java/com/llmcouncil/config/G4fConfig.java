package com.llmcouncil.config;

import com.llmcouncil.adapter.G4fAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * gpt4free (g4f) reverse API'sini iki farklı model altında ayrı birer konsey üyesi
 * olarak kaydeder; böylece resmi API key'i olmadan da peer review için en az iki
 * bağımsız ses elde edilir.
 */
@Configuration
public class G4fConfig {

    @Bean
    public G4fAdapter g4fModelA(WebClient.Builder webClientBuilder,
                                 @Value("${llm.providers.g4f.base-url}") String baseUrl,
                                 @Value("${llm.providers.g4f.model-a}") String model) {
        return new G4fAdapter(webClientBuilder, baseUrl, model, "g4f:" + model);
    }

    @Bean
    public G4fAdapter g4fModelB(WebClient.Builder webClientBuilder,
                                 @Value("${llm.providers.g4f.base-url}") String baseUrl,
                                 @Value("${llm.providers.g4f.model-b}") String model) {
        return new G4fAdapter(webClientBuilder, baseUrl, model, "g4f:" + model);
    }
}
