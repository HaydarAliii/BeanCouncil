package com.llmcouncil.adapter;

import com.llmcouncil.model.dto.LlmResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Decorator: bir konsey kimliğini (örn. "gpt", "claude") sabit bir isim altında temsil eder,
 * ama arka planda birincil sağlayıcıyı dener, o başarısız olursa otomatik olarak ikincil
 * sağlayıcıya (varsa) düşer. Böylece resmi API kotası dolsa/hata verse bile üye aktif kalmaya
 * çalışır. Dışa dönük providerName her zaman kimlik adıdır, hangi sağlayıcının yanıtladığı gizlenir.
 */
public class FallbackAdapter implements LlmProviderAdapter {

    private final String identityName;
    private final LlmProviderAdapter primary;
    private final LlmProviderAdapter secondary;

    public FallbackAdapter(String identityName, LlmProviderAdapter primary, LlmProviderAdapter secondary) {
        this.identityName = identityName;
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public String getProviderName() {
        return identityName;
    }

    @Override
    public CompletableFuture<LlmResponse> generateResponse(String prompt) {
        return primary.generateResponse(prompt).thenCompose(response -> {
            if (response.success() || secondary == null) {
                return CompletableFuture.completedFuture(relabel(response));
            }
            return secondary.generateResponse(prompt).thenApply(this::relabel);
        });
    }

    private LlmResponse relabel(LlmResponse response) {
        return response.success()
                ? LlmResponse.ok(identityName, response.content())
                : LlmResponse.failed(identityName, response.errorMessage());
    }
}
