package com.llmcouncil.controller;

import com.llmcouncil.model.dto.KeyStatus;
import com.llmcouncil.model.dto.ModelsResponse;
import com.llmcouncil.service.OpenRouterCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OpenRouter model kataloğunu ve kayıtlı key'in kredi/limit durumunu frontend'e sunar. */
@RestController
@RequestMapping("/api/models")
public class ModelsController {

    private final OpenRouterCatalogService catalogService;

    public ModelsController(OpenRouterCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ModelsResponse listModels() {
        return new ModelsResponse(catalogService.listModels());
    }

    @GetMapping("/key")
    public KeyStatus keyStatus() {
        return catalogService.keyStatus();
    }
}
