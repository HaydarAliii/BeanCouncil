package com.llmcouncil.service;

import com.llmcouncil.exception.ConversationNotFoundException;
import com.llmcouncil.model.dto.ConversationSummary;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.repository.ConversationRepository;
import com.llmcouncil.repository.ConversationThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryServiceTest {

    @Mock
    ConversationRepository conversationRepository;
    @Mock
    ConversationThreadRepository threadRepository;

    final ObjectMapper objectMapper = JsonMapper.builder().build();

    ConversationHistoryService service;

    @BeforeEach
    void setUp() {
        service = new ConversationHistoryService(conversationRepository, threadRepository, objectMapper);
    }

    private ConversationThreadEntity thread(long id, String title) {
        ConversationThreadEntity thread = new ConversationThreadEntity(title);
        ReflectionTestUtils.setField(thread, "id", id);
        return thread;
    }

    private ConversationEntity turnWithTranscript(CouncilResult result) {
        ConversationEntity entity = new ConversationEntity(result.prompt());
        entity.setFinalAnswer(result.finalAnswer());
        entity.setTranscript(objectMapper.writeValueAsString(result));
        return entity;
    }

    @Test
    void listThreads_mapsTurnCountFromRepository() {
        when(threadRepository.findAllByOrderByUpdatedAtDesc())
                .thenReturn(List.of(thread(1L, "soru A"), thread(2L, "soru B")));
        when(conversationRepository.countByThreadId(1L)).thenReturn(3L);
        when(conversationRepository.countByThreadId(2L)).thenReturn(1L);

        List<ConversationSummary> summaries = service.listThreads();

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).title()).isEqualTo("soru A");
        assertThat(summaries.get(0).turnCount()).isEqualTo(3L);
        assertThat(summaries.get(1).turnCount()).isEqualTo(1L);
    }

    @Test
    void getThreadTurns_deserializesTranscriptsInOrder() {
        CouncilResult first = new CouncilResult("soru 1", List.of(), List.of(), "pres", "cevap 1", 7L, List.of());
        CouncilResult second = new CouncilResult("soru 2", List.of(), List.of(), "pres", "cevap 2", 7L, List.of());
        when(conversationRepository.findByThreadIdOrderByCreatedAtAsc(7L))
                .thenReturn(List.of(turnWithTranscript(first), turnWithTranscript(second)));

        List<CouncilResult> turns = service.getThreadTurns(7L);

        assertThat(turns).hasSize(2);
        assertThat(turns.get(0).prompt()).isEqualTo("soru 1");
        assertThat(turns.get(1).prompt()).isEqualTo("soru 2");
    }

    @Test
    void getThreadTurns_legacyTranscriptWithoutWebSearchResultsField_normalizesToEmptyList() {
        // webSearchResults alanı hiç olmayan, bu özellikten önceki bir transcript JSON'ını simüle eder.
        String legacyJson = """
                {"prompt":"eski soru","firstOpinions":[],"reviews":[],\
                "presidentProvider":"gpt","finalAnswer":"eski cevap","threadId":5}""";
        ConversationEntity entity = new ConversationEntity("eski soru");
        entity.setTranscript(legacyJson);
        when(conversationRepository.findByThreadIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(entity));

        List<CouncilResult> turns = service.getThreadTurns(5L);

        assertThat(turns).hasSize(1);
        assertThat(turns.get(0).webSearchResults()).isNotNull().isEmpty();
    }

    @Test
    void getThreadTurns_noTurnsFound_throwsConversationNotFound() {
        when(conversationRepository.findByThreadIdOrderByCreatedAtAsc(123L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.getThreadTurns(123L))
                .isInstanceOf(ConversationNotFoundException.class);
    }
}
