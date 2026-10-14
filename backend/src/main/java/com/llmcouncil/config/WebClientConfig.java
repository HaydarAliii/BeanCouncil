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
     * Reverse API providers (g4f in particular) can sometimes hang without ever responding;
     * without this limit the request would wait forever. A shared upper bound is applied to
     * every provider call so that @Retry/@RateLimiter actually get a chance to kick in.
     */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    /** Spring's default 256KB in-memory buffer limit isn't enough for OpenRouter's /models response (464+ models). */
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024;

    @Bean
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create().responseTimeout(RESPONSE_TIMEOUT);
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE));
    }

    /**
     * Builds a single-use {@link WebClient} from the shared {@code webClientBuilder} bean, pointed
     * at the given base URL with a Bearer token if one is supplied. Without {@code clone()}, the
     * baseUrl/header calls would mutate the shared builder in place and affect every other caller —
     * so it's cloned first on every call. If {@code bearerToken} is null/blank, no Authorization
     * header is added at all.
     */
    public static WebClient authenticatedClient(WebClient.Builder sharedBuilder, String baseUrl, String bearerToken) {
        WebClient.Builder builder = sharedBuilder.clone().baseUrl(baseUrl);
        if (bearerToken != null && !bearerToken.isBlank()) {
            builder = builder.defaultHeader("Authorization", "Bearer " + bearerToken);
        }
        return builder.build();
    }
}
