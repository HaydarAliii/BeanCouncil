package com.llmcouncil.service;

import com.llmcouncil.exception.SettingsNotConfiguredException;
import com.llmcouncil.model.dto.AppSettings;
import com.llmcouncil.model.dto.SettingsRequest;
import com.llmcouncil.model.dto.SettingsResponse;
import com.llmcouncil.model.entity.AppSettingsEntity;
import com.llmcouncil.repository.AppSettingsRepository;
import com.llmcouncil.util.SecretCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    AppSettingsRepository repository;
    @Mock
    SecretCipher cipher;

    final ObjectMapper objectMapper = JsonMapper.builder().build();

    SettingsService service;

    @BeforeEach
    void setUp() {
        service = new SettingsService(repository, cipher, objectMapper);
    }

    private AppSettingsEntity entityWith(String keyEncrypted, List<String> models, String president) {
        AppSettingsEntity entity = new AppSettingsEntity(AppSettingsEntity.SINGLETON_ID);
        entity.setOpenRouterKeyEncrypted(keyEncrypted);
        entity.setSelectedModelIdsJson(objectMapper.writeValueAsString(models));
        entity.setPresidentModelId(president);
        return entity;
    }

    @Test
    void getDecryptedSettingsOrThrow_noRow_throwsSettingsNotConfigured() {
        when(repository.findSingleton()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDecryptedSettingsOrThrow())
                .isInstanceOf(SettingsNotConfiguredException.class);
    }

    @Test
    void getDecryptedSettingsOrThrow_blankKey_throwsSettingsNotConfigured() {
        AppSettingsEntity entity = entityWith("enc-key", List.of("a", "b"), "a");
        when(repository.findSingleton()).thenReturn(Optional.of(entity));
        when(cipher.decrypt("enc-key")).thenReturn("");

        assertThatThrownBy(() -> service.getDecryptedSettingsOrThrow())
                .isInstanceOf(SettingsNotConfiguredException.class);
    }

    @Test
    void getDecryptedSettingsOrThrow_fewerThanTwoModels_throwsSettingsNotConfigured() {
        AppSettingsEntity entity = entityWith("enc-key", List.of("a"), "a");
        when(repository.findSingleton()).thenReturn(Optional.of(entity));
        when(cipher.decrypt("enc-key")).thenReturn("real-key");

        assertThatThrownBy(() -> service.getDecryptedSettingsOrThrow())
                .isInstanceOf(SettingsNotConfiguredException.class);
    }

    @Test
    void getDecryptedSettingsOrThrow_presidentNotInSelectedModels_throwsSettingsNotConfigured() {
        AppSettingsEntity entity = entityWith("enc-key", List.of("a", "b"), "c");
        when(repository.findSingleton()).thenReturn(Optional.of(entity));
        when(cipher.decrypt("enc-key")).thenReturn("real-key");

        assertThatThrownBy(() -> service.getDecryptedSettingsOrThrow())
                .isInstanceOf(SettingsNotConfiguredException.class);
    }

    @Test
    void getDecryptedSettingsOrThrow_valid_returnsDecryptedSettingsIncludingTavily() {
        AppSettingsEntity entity = entityWith("enc-key", List.of("a", "b"), "a");
        entity.setTavilyKeyEncrypted("enc-tavily");
        when(repository.findSingleton()).thenReturn(Optional.of(entity));
        when(cipher.decrypt("enc-key")).thenReturn("real-key");
        when(cipher.decrypt("enc-tavily")).thenReturn("real-tavily");

        AppSettings settings = service.getDecryptedSettingsOrThrow();

        assertThat(settings.openRouterKey()).isEqualTo("real-key");
        assertThat(settings.selectedModelIds()).containsExactly("a", "b");
        assertThat(settings.presidentModelId()).isEqualTo("a");
        assertThat(settings.tavilyKey()).isEqualTo("real-tavily");
    }

    @Test
    void save_blankOpenRouterKey_keepsExistingEncryptedKey() {
        AppSettingsEntity existing = entityWith("existing-enc", List.of("a", "b"), "a");
        when(repository.findSingleton()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SettingsRequest request = new SettingsRequest(null, null, List.of("a", "b"), "a");
        service.save(request);

        ArgumentCaptor<AppSettingsEntity> captor = ArgumentCaptor.forClass(AppSettingsEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOpenRouterKeyEncrypted()).isEqualTo("existing-enc");
        verify(cipher, never()).encrypt(any());
    }

    @Test
    void save_newKeyProvided_encryptsAndStoresIt() {
        when(repository.findSingleton()).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cipher.encrypt("new-key")).thenReturn("new-enc");

        SettingsRequest request = new SettingsRequest("new-key", null, List.of("a", "b"), "a");
        service.save(request);

        ArgumentCaptor<AppSettingsEntity> captor = ArgumentCaptor.forClass(AppSettingsEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOpenRouterKeyEncrypted()).isEqualTo("new-enc");
    }

    @Test
    void save_presidentNotInSelectedModels_throwsIllegalArgumentWithoutTouchingRepository() {
        SettingsRequest request = new SettingsRequest("key", null, List.of("a", "b"), "c");

        assertThatThrownBy(() -> service.save(request))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void getResponse_neverReturnsFullKey_onlyLast4Masked() {
        AppSettingsEntity entity = entityWith("enc-key", List.of("a", "b"), "a");
        when(repository.findSingleton()).thenReturn(Optional.of(entity));
        when(cipher.decrypt("enc-key")).thenReturn("sk-or-v1-abcdef1234");

        SettingsResponse response = service.getResponse();

        assertThat(response.hasKey()).isTrue();
        assertThat(response.keyPreview()).isEqualTo("••••1234");
        assertThat(response.keyPreview()).doesNotContain("sk-or-v1-abcdef1234");
    }

    @Test
    void getResponse_noRow_returnsAllFalseEmptyDefaults() {
        when(repository.findSingleton()).thenReturn(Optional.empty());

        SettingsResponse response = service.getResponse();

        assertThat(response.hasKey()).isFalse();
        assertThat(response.hasTavilyKey()).isFalse();
        assertThat(response.selectedModelIds()).isEmpty();
        assertThat(response.presidentModelId()).isNull();
    }
}
