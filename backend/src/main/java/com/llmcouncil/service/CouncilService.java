package com.llmcouncil.service;

import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.model.dto.LlmResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Konsey iş akışının 1. aşaması: soruyu tüm kayıtlı LLM sağlayıcılarına paralel gönderir.
 * Review (aşama 2) ve final sentez (aşama 3) sonraki adımlarda eklenecektir.
 */
@Service
public class CouncilService {

    private final List<LlmProviderAdapter> providers;

    public CouncilService(List<LlmProviderAdapter> providers) {
        this.providers = providers;
    }

    public List<LlmResponse> collectFirstOpinions(String prompt) {
        List<CompletableFuture<LlmResponse>> futures = providers.stream()
                .map(provider -> provider.generateResponse(prompt))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}
