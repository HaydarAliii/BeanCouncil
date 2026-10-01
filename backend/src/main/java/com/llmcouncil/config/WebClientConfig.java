package com.llmcouncil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Reverse API sağlayıcıları (özellikle g4f) bazen hiç yanıt vermeden asılı kalabilir;
     * bu sınır olmadan istek sonsuza kadar bekler. @Retry/@RateLimiter'ın devreye girebilmesi
     * için tüm sağlayıcı çağrılarına ortak bir üst sınır konur.
     */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    /** Spring'in varsayılan 256KB bellek-içi tampon sınırı OpenRouter'ın /models yanıtı (464+ model) için yetersiz. */
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024;

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create().responseTimeout(RESPONSE_TIMEOUT);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE));
    }
}
