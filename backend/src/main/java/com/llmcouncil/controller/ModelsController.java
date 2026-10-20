package com.llmcouncil.controller;

import com.llmcouncil.model.dto.KeyStatus;
import com.llmcouncil.model.dto.ModelsResponse;
import com.llmcouncil.service.ModelCatalogService;
import com.llmcouncil.service.OpenRouterCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the combined OpenRouter + Ollama model catalog and the configured OpenRouter key's
 * credit/limit status to the frontend. Key status stays OpenRouter-only — local models have no
 * credit/limit concept.
 */
@RestController
@RequestMapping("/api/models")
public class ModelsController {

    private final ModelCatalogService modelCatalogService;
    private final OpenRouterCatalogService openRouterCatalogService;

    public ModelsController(ModelCatalogService modelCatalogService, OpenRouterCatalogService openRouterCatalogService) {
        this.modelCatalogService = modelCatalogService;
        this.openRouterCatalogService = openRouterCatalogService;
    }

    @GetMapping
    public ModelsResponse listModels() {
        return new ModelsResponse(modelCatalogService.listAllModels());
    }

    @GetMapping("/key")
    public KeyStatus keyStatus() {
        return openRouterCatalogService.keyStatus();
    }
}
