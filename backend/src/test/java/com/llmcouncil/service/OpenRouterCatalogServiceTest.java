package com.llmcouncil.service;

import com.llmcouncil.exception.OpenRouterUnauthorizedException;
import com.llmcouncil.exception.SettingsNotConfiguredException;
import com.llmcouncil.model.dto.KeyStatus;
import com.llmcouncil.model.dto.ModelInfo;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** OpenRouter'ın /models ve /key sözleşmesini gerçek ağa çıkmadan doğrular (MockWebServer). */
@ExtendWith(MockitoExtension.class)
class OpenRouterCatalogServiceTest {

    @Mock
    SettingsService settingsService;

    MockWebServer server;
    OpenRouterCatalogService catalogService;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        catalogService = new OpenRouterCatalogService(WebClient.builder(), settingsService, server.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.close();
    }

    @Test
    void listModels_zeroPricing_isFreeTrue() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"data":[{"id":"a/b","name":"B","context_length":128000,\
                        "pricing":{"prompt":"0","completion":"0"}}]}""")
                .build());

        List<ModelInfo> models = catalogService.listModels();

        assertThat(models).hasSize(1);
        ModelInfo model = models.get(0);
        assertThat(model.id()).isEqualTo("a/b");
        assertThat(model.contextLength()).isEqualTo(128000L);
        assertThat(model.isFree()).isTrue();
    }

    @Test
    void listModels_nonZeroPricing_isFreeFalse() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"data":[{"id":"a/b","name":"B","pricing":{"prompt":"0.000002","completion":"0.00001"}}]}""")
                .build());

        List<ModelInfo> models = catalogService.listModels();

        assertThat(models.get(0).isFree()).isFalse();
    }

    @Test
    void listModels_unparseablePrice_treatedAsNotFree() {
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"data":[{"id":"a/b","name":"B","pricing":{"prompt":"abc","completion":"0"}}]}""")
                .build());

        List<ModelInfo> models = catalogService.listModels();

        assertThat(models.get(0).isFree()).isFalse();
    }

    @Test
    void keyStatus_computesRemainingFromLimitAndUsage() throws InterruptedException {
        when(settingsService.getDecryptedKey()).thenReturn(Optional.of("sk-or-test"));
        server.enqueue(new MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body("""
                        {"data":{"label":"L","limit":10.0,"usage":3.0,"is_free_tier":false}}""")
                .build());

        KeyStatus status = catalogService.keyStatus();

        assertThat(status.limitRemaining()).isEqualTo(7.0);
        assertThat(status.isFreeTier()).isFalse();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getTarget()).isEqualTo("/key");
        assertThat(request.getHeaders().get("Authorization")).isEqualTo("Bearer sk-or-test");
    }

    @Test
    void keyStatus_401_throwsOpenRouterUnauthorized() {
        when(settingsService.getDecryptedKey()).thenReturn(Optional.of("sk-or-bad"));
        server.enqueue(new MockResponse.Builder().code(401).build());

        assertThatThrownBy(() -> catalogService.keyStatus())
                .isInstanceOf(OpenRouterUnauthorizedException.class);
    }

    @Test
    void keyStatus_noKeyConfigured_throwsSettingsNotConfigured() {
        when(settingsService.getDecryptedKey()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.keyStatus())
                .isInstanceOf(SettingsNotConfiguredException.class);
    }
}
