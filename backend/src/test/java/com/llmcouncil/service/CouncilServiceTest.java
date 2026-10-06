package com.llmcouncil.service;

import com.llmcouncil.adapter.LlmProviderAdapter;
import com.llmcouncil.exception.ConversationNotFoundException;
import com.llmcouncil.model.dto.AppSettings;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.model.dto.LlmResponse;
import com.llmcouncil.model.dto.WebSearchResult;
import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.repository.ConversationRepository;
import com.llmcouncil.repository.ConversationThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CouncilService'in en kritik iş mantığını (başkan tarafsızlığı, follow-up bağlamı, web arama
 * entegrasyonu, hata durumlarında zarif bozulma, thread çözümleme) gerçek HTTP/DB olmadan,
 * tamamen mock'lanmış bağımlılıklarla doğrular.
 */
@ExtendWith(MockitoExtension.class)
class CouncilServiceTest {

    @Mock
    CouncilMemberFactory memberFactory;
    @Mock
    SettingsService settingsService;
    @Mock
    WebSearchService webSearchService;
    @Mock
    ConversationRepository repository;
    @Mock
    ConversationThreadRepository threadRepository;

    final ObjectMapper objectMapper = JsonMapper.builder().build();

    CouncilService service;

    @BeforeEach
    void setUp() {
        service = new CouncilService(memberFactory, settingsService, webSearchService,
                repository, threadRepository, objectMapper);
    }

    /** Yeni bir konuşma (threadId=null) senaryosu için ortak stub — id=42 olan yeni thread döner. */
    private void stubNewThread() {
        when(threadRepository.save(any(ConversationThreadEntity.class))).thenAnswer(inv -> {
            ConversationThreadEntity thread = inv.getArgument(0);
            ReflectionTestUtils.setField(thread, "id", 42L);
            return thread;
        });
        when(repository.findByThreadIdOrderByCreatedAtAsc(anyLong())).thenReturn(List.of());
    }

    private AppSettings settings(String president, String... models) {
        return new AppSettings("or-key", List.of(models), president, null);
    }

    private AppSettings settingsWithTavily(String president, String tavilyKey, String... models) {
        return new AppSettings("or-key", List.of(models), president, tavilyKey);
    }

    /**
     * Adapter'ın gördüğü prompt'un içeriğine göre hangi aşamada (ilk görüş / review / sentez)
     * olduğunu ayırt edip doğru stub yanıtını döner — CouncilService'in ürettiği 3 farklı prompt
     * şablonu birbirinden ayırt edilebilir metinler içerir.
     */
    private LlmProviderAdapter member(String name, LlmResponse firstOpinion, LlmResponse review, LlmResponse synthesis) {
        LlmProviderAdapter adapter = mock(LlmProviderAdapter.class);
        lenient().when(adapter.getProviderName()).thenReturn(name);
        // lenient: bazı testlerde (ör. "başkan hiç çağrılmadı") bu üyenin generateResponse'u
        // bilinçli olarak hiç tetiklenmiyor — strict stubbing bunu false-positive olarak işaretler.
        lenient().when(adapter.generateResponse(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> {
            String prompt = inv.getArgument(0);
            if (prompt.contains("You are the president")) {
                return CompletableFuture.completedFuture(synthesis);
            }
            if (prompt.contains("critique their strengths")) {
                return CompletableFuture.completedFuture(review);
            }
            return CompletableFuture.completedFuture(firstOpinion);
        });
        return adapter;
    }

    // --- (a) Başkan tarafsızlığı ---

    @Test
    void president_onlyParticipatesInSynthesis_neverGivesFirstOpinionOrReview() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "görüş A"), LlmResponse.ok("A", "review A"), null);
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "görüş B"), LlmResponse.ok("B", "review B"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "sentez"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("soru", null, false);

        verify(c, times(1)).generateResponse(org.mockito.ArgumentMatchers.anyString()); // sadece sentez
        verify(a, times(2)).generateResponse(org.mockito.ArgumentMatchers.anyString()); // görüş + review
        verify(b, times(2)).generateResponse(org.mockito.ArgumentMatchers.anyString());
        assertThat(result.presidentProvider()).isEqualTo("C");
        assertThat(result.firstOpinions()).extracting(LlmResponse::providerName).containsExactlyInAnyOrder("A", "B");
        assertThat(result.reviews()).extracting(LlmResponse::providerName).containsExactlyInAnyOrder("A", "B");
    }

    // --- (b) Follow-up bağlamı ---

    @Test
    void followUp_previousTurnsAreInEffectivePrompt_butPersistedPromptStaysRaw() {
        when(threadRepository.findById(7L)).thenAnswer(inv -> {
            ConversationThreadEntity thread = new ConversationThreadEntity("önceki soru");
            ReflectionTestUtils.setField(thread, "id", 7L);
            return Optional.of(thread);
        });
        ConversationEntity previousTurn = new ConversationEntity("Önceki soru");
        previousTurn.setFinalAnswer("Önceki cevap");
        when(repository.findByThreadIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(previousTurn));

        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "görüş"), LlmResponse.ok("A", "review"), null);
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "görüş"), LlmResponse.ok("B", "review"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "sentez"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("Yeni soru", 7L, false);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(a, times(2)).generateResponse(captor.capture());
        String firstOpinionPrompt = captor.getAllValues().get(0);
        assertThat(firstOpinionPrompt).contains("Önceki soru").contains("Önceki cevap").contains("Önceki tur");
        assertThat(firstOpinionPrompt).contains("Yeni soru");

        assertThat(result.prompt()).isEqualTo("Yeni soru"); // ham soru, bağlam blobu değil
        assertThat(result.threadId()).isEqualTo(7L);
    }

    // --- (c) Web arama entegrasyonu ---

    @Test
    void webSearch_enabledWithKey_resultsInPromptAndResponse_butPersistedPromptStaysRaw() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "görüş"), LlmResponse.ok("A", "review"), null);
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "görüş"), LlmResponse.ok("B", "review"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "sentez"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settingsWithTavily("C", "tav-key", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));
        when(webSearchService.search("ham soru", "tav-key"))
                .thenReturn(List.of(new WebSearchResult("T1", "http://u", "snippet-içerik")));

        CouncilResult result = service.deliberate("ham soru", null, true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(a, times(2)).generateResponse(captor.capture());
        assertThat(captor.getAllValues().get(0)).contains("T1").contains("snippet-içerik").contains("web araması");

        assertThat(result.webSearchResults()).hasSize(1);
        assertThat(result.prompt()).isEqualTo("ham soru");
        verify(webSearchService).search("ham soru", "tav-key");
    }

    @Test
    void webSearch_disabled_searchNeverCalled() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settingsWithTavily("C", "tav-key", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        service.deliberate("soru", null, false);

        verify(webSearchService, never()).search(any(), any());
    }

    @Test
    void webSearch_enabledButNoTavilyKey_searchNeverCalled() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C")); // tavilyKey=null
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        service.deliberate("soru", null, true);

        verify(webSearchService, never()).search(any(), any());
    }

    // --- (d) Tüm üyeler başarısız ---

    @Test
    void allFirstOpinionsFail_returnsFallbackMessage_presidentNeverCalled() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.failed("A", "zaman aşımı"), null, null);
        LlmProviderAdapter b = member("B", LlmResponse.failed("B", "rate limit"), null, null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "sentez"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("soru", null, false);

        assertThat(result.finalAnswer()).isEqualTo("The council could not produce an answer because all members failed.");
        assertThat(result.reviews()).isEmpty();
        assertThat(result.presidentProvider()).isEqualTo("C");
        verify(c, never()).generateResponse(any());
    }

    // --- (e) Başkan sentez fallback'i ---

    @Test
    void presidentSynthesisFails_fallsBackToFirstSuccessfulCouncilor() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "görüş"), LlmResponse.ok("A", "review"),
                LlmResponse.ok("A", "A-sentez"));
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "görüş"), LlmResponse.ok("B", "review"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.failed("C", "başkan çöktü"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("soru", null, false);

        assertThat(result.presidentProvider()).isEqualTo("A");
        assertThat(result.finalAnswer()).isEqualTo("A-sentez");
    }

    @Test
    void allSynthesisAttemptsFail_returnsPresidentFailedMessage() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "görüş"), LlmResponse.ok("A", "review"),
                LlmResponse.failed("A", "A da çöktü"));
        LlmProviderAdapter c = member("C", null, null, LlmResponse.failed("C", "başkan çöktü"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        CouncilResult result = service.deliberate("soru", null, false);

        assertThat(result.presidentProvider()).isEqualTo("C");
        assertThat(result.finalAnswer()).startsWith("President provider failed:");
    }

    // --- (f) Thread çözümleme ---

    @Test
    void resolveThread_nullThreadId_createsNewThread() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        CouncilResult result = service.deliberate("ilk soru", null, false);

        ArgumentCaptor<ConversationThreadEntity> captor = ArgumentCaptor.forClass(ConversationThreadEntity.class);
        verify(threadRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("ilk soru");
        assertThat(result.threadId()).isEqualTo(42L);
    }

    @Test
    void resolveThread_existingThreadId_touchesAndSaves() {
        ConversationThreadEntity existing = new ConversationThreadEntity("eski başlık");
        ReflectionTestUtils.setField(existing, "id", 7L);
        var originalUpdatedAt = existing.getUpdatedAt();
        when(threadRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.findByThreadIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());

        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        CouncilResult result = service.deliberate("takip sorusu", 7L, false);

        verify(threadRepository).save(existing);
        assertThat(existing.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        assertThat(result.threadId()).isEqualTo(7L);
    }

    @Test
    void resolveThread_unknownThreadId_throwsConversationNotFound() {
        when(threadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deliberate("soru", 99L, false))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    // --- Persist sanity ---

    @Test
    void persist_savesTranscriptThatRoundTripsBackToSameResult() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "final cevap"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        service.deliberate("persist testi", null, false);

        ArgumentCaptor<ConversationEntity> captor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(repository).save(captor.capture());
        ConversationEntity saved = captor.getValue();
        assertThat(saved.getFinalAnswer()).isEqualTo("final cevap");

        CouncilResult roundTripped = objectMapper.readValue(saved.getTranscript(), CouncilResult.class);
        assertThat(roundTripped.prompt()).isEqualTo("persist testi");
        assertThat(roundTripped.finalAnswer()).isEqualTo("final cevap");
    }
}
