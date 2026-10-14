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
 * Verifies CouncilService's most critical business logic (president impartiality, follow-up
 * context, web search integration, graceful degradation on failures, thread resolution) with
 * fully mocked dependencies, no real HTTP/DB involved.
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

    /** Shared stub for the new-conversation (threadId=null) scenario — returns a new thread with id=42. */
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
     * Figures out which stage (first opinion / review / synthesis) a call is in based on the
     * content of the prompt the adapter received, and returns the matching stub response — the
     * 3 distinct prompt templates CouncilService produces each contain distinguishable text.
     */
    private LlmProviderAdapter member(String name, LlmResponse firstOpinion, LlmResponse review, LlmResponse synthesis) {
        LlmProviderAdapter adapter = mock(LlmProviderAdapter.class);
        lenient().when(adapter.getProviderName()).thenReturn(name);
        // lenient: in some tests (e.g. "president never called") this member's generateResponse
        // is deliberately never triggered — strict stubbing would flag that as a false positive.
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

    // --- (a) President impartiality ---

    @Test
    void president_onlyParticipatesInSynthesis_neverGivesFirstOpinionOrReview() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "opinion A"), LlmResponse.ok("A", "review A"), null);
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "opinion B"), LlmResponse.ok("B", "review B"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "synthesis"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("question", null, false);

        verify(c, times(1)).generateResponse(org.mockito.ArgumentMatchers.anyString()); // synthesis only
        verify(a, times(2)).generateResponse(org.mockito.ArgumentMatchers.anyString()); // opinion + review
        verify(b, times(2)).generateResponse(org.mockito.ArgumentMatchers.anyString());
        assertThat(result.presidentProvider()).isEqualTo("C");
        assertThat(result.firstOpinions()).extracting(LlmResponse::providerName).containsExactlyInAnyOrder("A", "B");
        assertThat(result.reviews()).extracting(LlmResponse::providerName).containsExactlyInAnyOrder("A", "B");
    }

    // --- (b) Follow-up context ---

    @Test
    void followUp_previousTurnsAreInEffectivePrompt_butPersistedPromptStaysRaw() {
        when(threadRepository.findById(7L)).thenAnswer(inv -> {
            ConversationThreadEntity thread = new ConversationThreadEntity("previous question");
            ReflectionTestUtils.setField(thread, "id", 7L);
            return Optional.of(thread);
        });
        ConversationEntity previousTurn = new ConversationEntity("Previous question");
        previousTurn.setFinalAnswer("Previous answer");
        when(repository.findByThreadIdOrderByCreatedAtAsc(7L)).thenReturn(List.of(previousTurn));

        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "opinion"), LlmResponse.ok("A", "review"), null);
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "opinion"), LlmResponse.ok("B", "review"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "synthesis"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("New question", 7L, false);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(a, times(2)).generateResponse(captor.capture());
        String firstOpinionPrompt = captor.getAllValues().get(0);
        assertThat(firstOpinionPrompt).contains("Previous question").contains("Previous answer").contains("Previous turn");
        assertThat(firstOpinionPrompt).contains("New question");

        assertThat(result.prompt()).isEqualTo("New question"); // raw question, not the context blob
        assertThat(result.threadId()).isEqualTo(7L);
    }

    // --- (c) Web search integration ---

    @Test
    void webSearch_enabledWithKey_resultsInPromptAndResponse_butPersistedPromptStaysRaw() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "opinion"), LlmResponse.ok("A", "review"), null);
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "opinion"), LlmResponse.ok("B", "review"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "synthesis"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settingsWithTavily("C", "tav-key", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));
        when(webSearchService.search("raw question", "tav-key"))
                .thenReturn(List.of(new WebSearchResult("T1", "http://u", "snippet-content")));

        CouncilResult result = service.deliberate("raw question", null, true);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(a, times(2)).generateResponse(captor.capture());
        assertThat(captor.getAllValues().get(0)).contains("T1").contains("snippet-content").contains("web search");

        assertThat(result.webSearchResults()).hasSize(1);
        assertThat(result.prompt()).isEqualTo("raw question");
        verify(webSearchService).search("raw question", "tav-key");
    }

    @Test
    void webSearch_disabled_searchNeverCalled() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settingsWithTavily("C", "tav-key", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        service.deliberate("question", null, false);

        verify(webSearchService, never()).search(any(), any());
    }

    @Test
    void webSearch_enabledButNoTavilyKey_searchNeverCalled() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C")); // tavilyKey=null
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        service.deliberate("question", null, true);

        verify(webSearchService, never()).search(any(), any());
    }

    // --- (d) All members fail ---

    @Test
    void allFirstOpinionsFail_returnsFallbackMessage_presidentNeverCalled() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.failed("A", "timeout"), null, null);
        LlmProviderAdapter b = member("B", LlmResponse.failed("B", "rate limit"), null, null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "synthesis"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("question", null, false);

        assertThat(result.finalAnswer()).isEqualTo("The council could not produce an answer because all members failed.");
        assertThat(result.reviews()).isEmpty();
        assertThat(result.presidentProvider()).isEqualTo("C");
        verify(c, never()).generateResponse(any());
    }

    // --- (e) President synthesis fallback ---

    @Test
    void presidentSynthesisFails_fallsBackToFirstSuccessfulCouncilor() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "opinion"), LlmResponse.ok("A", "review"),
                LlmResponse.ok("A", "A-synthesis"));
        LlmProviderAdapter b = member("B", LlmResponse.ok("B", "opinion"), LlmResponse.ok("B", "review"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.failed("C", "president crashed"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "B", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, b, c));

        CouncilResult result = service.deliberate("question", null, false);

        assertThat(result.presidentProvider()).isEqualTo("A");
        assertThat(result.finalAnswer()).isEqualTo("A-synthesis");
    }

    @Test
    void allSynthesisAttemptsFail_returnsPresidentFailedMessage() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "opinion"), LlmResponse.ok("A", "review"),
                LlmResponse.failed("A", "A crashed too"));
        LlmProviderAdapter c = member("C", null, null, LlmResponse.failed("C", "president crashed"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        CouncilResult result = service.deliberate("question", null, false);

        assertThat(result.presidentProvider()).isEqualTo("C");
        assertThat(result.finalAnswer()).startsWith("President provider failed:");
    }

    // --- (f) Thread resolution ---

    @Test
    void resolveThread_nullThreadId_createsNewThread() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        CouncilResult result = service.deliberate("first question", null, false);

        ArgumentCaptor<ConversationThreadEntity> captor = ArgumentCaptor.forClass(ConversationThreadEntity.class);
        verify(threadRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("first question");
        assertThat(result.threadId()).isEqualTo(42L);
    }

    @Test
    void resolveThread_existingThreadId_touchesAndSaves() {
        ConversationThreadEntity existing = new ConversationThreadEntity("old title");
        ReflectionTestUtils.setField(existing, "id", 7L);
        var originalUpdatedAt = existing.getUpdatedAt();
        when(threadRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.findByThreadIdOrderByCreatedAtAsc(7L)).thenReturn(List.of());

        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "s"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        CouncilResult result = service.deliberate("follow-up question", 7L, false);

        verify(threadRepository).save(existing);
        assertThat(existing.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        assertThat(result.threadId()).isEqualTo(7L);
    }

    @Test
    void resolveThread_unknownThreadId_throwsConversationNotFound() {
        when(threadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deliberate("question", 99L, false))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    // --- Persist sanity ---

    @Test
    void persist_savesTranscriptThatRoundTripsBackToSameResult() {
        stubNewThread();
        LlmProviderAdapter a = member("A", LlmResponse.ok("A", "g"), LlmResponse.ok("A", "r"), null);
        LlmProviderAdapter c = member("C", null, null, LlmResponse.ok("C", "final answer"));
        when(settingsService.getDecryptedSettingsOrThrow()).thenReturn(settings("C", "A", "C"));
        when(memberFactory.buildAllMembers(any())).thenReturn(List.of(a, c));

        service.deliberate("persist test", null, false);

        ArgumentCaptor<ConversationEntity> captor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(repository).save(captor.capture());
        ConversationEntity saved = captor.getValue();
        assertThat(saved.getFinalAnswer()).isEqualTo("final answer");

        CouncilResult roundTripped = objectMapper.readValue(saved.getTranscript(), CouncilResult.class);
        assertThat(roundTripped.prompt()).isEqualTo("persist test");
        assertThat(roundTripped.finalAnswer()).isEqualTo("final answer");
    }
}
