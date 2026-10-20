package com.llmcouncil.service;

import com.llmcouncil.model.dto.ModelInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Verifies the OpenRouter+Ollama merge and that either catalog failing never breaks the other. */
@ExtendWith(MockitoExtension.class)
class ModelCatalogServiceTest {

    @Mock
    OpenRouterCatalogService openRouterCatalogService;
    @Mock
    OllamaCatalogService ollamaCatalogService;

    ModelCatalogService catalogService;

    private static final ModelInfo OPENROUTER_MODEL =
            new ModelInfo("openai/gpt-5", "GPT-5", 128000L, "0.000002", "0.00001", false);
    private static final ModelInfo OLLAMA_MODEL =
            new ModelInfo("ollama/llama3.1", "llama3.1", null, "0", "0", true);

    @BeforeEach
    void setUp() {
        catalogService = new ModelCatalogService(openRouterCatalogService, ollamaCatalogService);
    }

    @Test
    void listAllModels_bothAvailable_mergesOpenRouterFirst() {
        when(openRouterCatalogService.listModels()).thenReturn(List.of(OPENROUTER_MODEL));
        when(ollamaCatalogService.listModels()).thenReturn(List.of(OLLAMA_MODEL));

        List<ModelInfo> models = catalogService.listAllModels();

        assertThat(models).containsExactly(OPENROUTER_MODEL, OLLAMA_MODEL);
    }

    @Test
    void listAllModels_openRouterThrows_stillReturnsOllamaModels() {
        when(openRouterCatalogService.listModels()).thenThrow(new RuntimeException("OpenRouter unreachable"));
        when(ollamaCatalogService.listModels()).thenReturn(List.of(OLLAMA_MODEL));

        List<ModelInfo> models = catalogService.listAllModels();

        assertThat(models).containsExactly(OLLAMA_MODEL);
    }

    @Test
    void listAllModels_ollamaEmpty_stillReturnsOpenRouterModels() {
        when(openRouterCatalogService.listModels()).thenReturn(List.of(OPENROUTER_MODEL));
        when(ollamaCatalogService.listModels()).thenReturn(List.of());

        List<ModelInfo> models = catalogService.listAllModels();

        assertThat(models).containsExactly(OPENROUTER_MODEL);
    }
}
