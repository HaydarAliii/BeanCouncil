package com.llmcouncil.service;

import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.model.dto.AppSettings;
import com.llmcouncil.model.dto.LlmResponse;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@code ollama/}-prefixed model ids are routed to the Ollama server (prefix
 * stripped, no Authorization header) while every other id keeps going to OpenRouter unchanged —
 * without hitting the real network (two separate MockWebServers stand in for each provider).
 */
class CouncilMemberFactoryTest {

    MockWebServer openRouterServer;
    MockWebServer ollamaServer;
    CouncilMemberFactory factory;

    @BeforeEach
    void setUp() throws IOException {
        openRouterServer = new MockWebServer();
        openRouterServer.start();
        ollamaServer = new MockWebServer();
        ollamaServer.start();
        factory = new CouncilMemberFactory(WebClient.builder(),
                openRouterServer.url("/").toString(), ollamaServer.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        openRouterServer.close();
        ollamaServer.close();
    }

    @Test
    void buildAllMembers_mixedSelection_routesEachIdToItsOwnProvider() throws InterruptedException {
        AppSettings settings = new AppSettings("sk-or-test",
                List.of("ollama/llama3.1", "openai/gpt-5"), "openai/gpt-5", null);

        List<LlmProviderAdapter> members = factory.buildAllMembers(settings);

        assertThat(members).hasSize(2);
        LlmProviderAdapter ollamaMember = memberNamed(members, "ollama/llama3.1");
        LlmProviderAdapter openRouterMember = memberNamed(members, "openai/gpt-5");

        ollamaServer.enqueue(okResponse());
        LlmResponse ollamaResponse = ollamaMember.generateResponse("hi").join();
        assertThat(ollamaResponse.success()).isTrue();
        assertThat(ollamaMember.getProviderName()).isEqualTo("ollama/llama3.1");

        RecordedRequest ollamaRequest = ollamaServer.takeRequest();
        assertThat(ollamaRequest.getTarget()).isEqualTo("/v1/chat/completions");
        assertThat(ollamaRequest.getHeaders().get("Authorization")).isNull();
        assertThat(ollamaRequest.getBody().utf8()).contains("\"model\":\"llama3.1\"");

        openRouterServer.enqueue(okResponse());
        LlmResponse openRouterResponse = openRouterMember.generateResponse("hi").join();
        assertThat(openRouterResponse.success()).isTrue();
        assertThat(openRouterMember.getProviderName()).isEqualTo("openai/gpt-5");

        RecordedRequest openRouterRequest = openRouterServer.takeRequest();
        assertThat(openRouterRequest.getTarget()).isEqualTo("/chat/completions");
        assertThat(openRouterRequest.getHeaders().get("Authorization")).isEqualTo("Bearer sk-or-test");
        assertThat(openRouterRequest.getBody().utf8()).contains("\"model\":\"openai/gpt-5\"");
    }

    @Test
    void buildAllMembers_allOllamaNullKey_neverAddsAuthorizationHeader() throws InterruptedException {
        AppSettings settings = new AppSettings(null,
                List.of("ollama/llama3.1", "ollama/qwen2.5"), "ollama/llama3.1", null);

        List<LlmProviderAdapter> members = factory.buildAllMembers(settings);
        ollamaServer.enqueue(okResponse());
        members.get(0).generateResponse("hi").join();

        assertThat(ollamaServer.takeRequest().getHeaders().get("Authorization")).isNull();
    }

    private MockResponse okResponse() {
        return new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"choices":[{"message":{"role":"assistant","content":"hello"}}]}""")
                .build();
    }

    private LlmProviderAdapter memberNamed(List<LlmProviderAdapter> members, String providerName) {
        return members.stream()
                .filter(member -> member.getProviderName().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No member named " + providerName));
    }
}
