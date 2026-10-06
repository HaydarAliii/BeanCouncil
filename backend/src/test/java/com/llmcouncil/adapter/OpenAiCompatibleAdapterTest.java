package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** OpenRouter'ın chat/completions sözleşmesini gerçek ağa çıkmadan doğrular (MockWebServer). */
class OpenAiCompatibleAdapterTest {

    MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    private OpenAiCompatibleAdapter adapter(String apiKey) {
        return new OpenAiCompatibleAdapter(WebClient.builder(), server.url("/").toString(),
                "openai/gpt-5", "openai/gpt-5", apiKey);
    }

    @Test
    void success_extractsChoiceContentAndSendsAuthHeader() throws InterruptedException {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"choices":[{"message":{"role":"assistant","content":"merhaba"}}]}""")
                .build());

        LlmResponse response = adapter("sk-test").generateResponse("soru").join();

        assertThat(response.success()).isTrue();
        assertThat(response.content()).isEqualTo("merhaba");
        assertThat(response.providerName()).isEqualTo("openai/gpt-5");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getTarget()).isEqualTo("/chat/completions");
        assertThat(request.getHeaders().get("Authorization")).isEqualTo("Bearer sk-test");
        String body = request.getBody().utf8();
        assertThat(body).contains("\"model\":\"openai/gpt-5\"").contains("\"content\":\"soru\"");
    }

    @Test
    void httpError_returnsFailedResponse_neverThrows() {
        server.enqueue(new MockResponse.Builder().code(500).body("internal error").build());

        LlmResponse response = adapter("sk-test").generateResponse("soru").join();

        assertThat(response.success()).isFalse();
        assertThat(response.errorMessage()).isNotNull();
        assertThat(response.content()).isNull();
    }

    @Test
    void malformedBody_returnsFailedResponse_neverThrows() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("{}")
                .build());

        LlmResponse response = adapter("sk-test").generateResponse("soru").join();

        assertThat(response.success()).isFalse();
    }

    @Test
    void noApiKey_omitsAuthorizationHeader() throws InterruptedException {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"choices":[{"message":{"role":"assistant","content":"ok"}}]}""")
                .build());

        adapter(null).generateResponse("soru").join();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeaders().get("Authorization")).isNull();
    }
}
