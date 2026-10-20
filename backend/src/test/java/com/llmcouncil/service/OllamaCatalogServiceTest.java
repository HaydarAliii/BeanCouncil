package com.llmcouncil.service;

import com.llmcouncil.model.dto.ModelInfo;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the Ollama /api/tags contract and its failure handling (without hitting the real network). */
class OllamaCatalogServiceTest {

    MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() {
        // The "server unreachable" test closes the server itself to simulate Ollama being down;
        // closing an already-closed server here is harmless, so any exception is ignored.
        try {
            server.close();
        } catch (Exception ignored) {
        }
    }

    private OllamaCatalogService catalogService() {
        return new OllamaCatalogService(WebClient.builder(), server.url("/").toString());
    }

    @Test
    void listModels_tagsResponse_mapsToOllamaPrefixedFreeModels() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"models":[{"name":"llama3.1:latest"},{"name":"qwen2.5:7b"}]}""")
                .build());

        List<ModelInfo> models = catalogService().listModels();

        assertThat(models).hasSize(2);
        assertThat(models.get(0).id()).isEqualTo("ollama/llama3.1:latest");
        assertThat(models.get(0).name()).isEqualTo("llama3.1:latest");
        assertThat(models.get(0).isFree()).isTrue();
        assertThat(models.get(1).id()).isEqualTo("ollama/qwen2.5:7b");
    }

    @Test
    void listModels_serverUnreachable_returnsEmptyListInsteadOfThrowing() throws IOException {
        OllamaCatalogService catalogService = catalogService();
        server.close();

        List<ModelInfo> models = catalogService.listModels();

        assertThat(models).isEmpty();
    }

    @Test
    void listModels_httpError_returnsEmptyListInsteadOfThrowing() {
        server.enqueue(new MockResponse.Builder().code(500).build());

        List<ModelInfo> models = catalogService().listModels();

        assertThat(models).isEmpty();
    }
}
