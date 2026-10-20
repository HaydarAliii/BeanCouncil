package com.llmcouncil.service;

import com.llmcouncil.model.dto.ModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Merges the OpenRouter and Ollama catalogs into the single list the frontend's model picker
 * renders. OpenRouter is listed first; either source failing (unreachable OpenRouter, no local
 * Ollama installed) never breaks the other — a user running a fully local council shouldn't be
 * blocked by OpenRouter being unreachable, and vice versa.
 */
@Service
public class ModelCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ModelCatalogService.class);

    private final OpenRouterCatalogService openRouterCatalogService;
    private final OllamaCatalogService ollamaCatalogService;

    public ModelCatalogService(OpenRouterCatalogService openRouterCatalogService,
                                OllamaCatalogService ollamaCatalogService) {
        this.openRouterCatalogService = openRouterCatalogService;
        this.ollamaCatalogService = ollamaCatalogService;
    }

    public List<ModelInfo> listAllModels() {
        List<ModelInfo> models = new ArrayList<>();
        try {
            models.addAll(openRouterCatalogService.listModels());
        } catch (Exception ex) {
            log.warn("Could not load OpenRouter model catalog; continuing with local models only.", ex);
        }
        // OllamaCatalogService already swallows its own failures and returns an empty list.
        models.addAll(ollamaCatalogService.listModels());
        return models;
    }
}
