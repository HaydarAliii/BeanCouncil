package com.llmcouncil.service;

import com.llmcouncil.exception.ConversationNotFoundException;
import com.llmcouncil.model.dto.ConversationSummary;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.model.entity.ConversationThreadEntity;
import com.llmcouncil.repository.ConversationRepository;
import com.llmcouncil.repository.ConversationThreadRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Geçmişteki konuşma thread'lerini listeler/detaylandırır. Detay, her turun {@code transcript}
 * kolonuna yazılmış tam {@link CouncilResult} JSON'ını geri çözer — konsey akışıyla aynı DTO
 * olduğu için frontend'deki FinalAnswer/ProcessDetails bileşenleri aynen yeniden kullanılabilir.
 */
@Service
public class ConversationHistoryService {

    private final ConversationRepository conversationRepository;
    private final ConversationThreadRepository threadRepository;
    private final ObjectMapper objectMapper;

    public ConversationHistoryService(ConversationRepository conversationRepository,
                                       ConversationThreadRepository threadRepository,
                                       ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.threadRepository = threadRepository;
        this.objectMapper = objectMapper;
    }

    public List<ConversationSummary> listThreads() {
        return threadRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    /** @return thread'deki tüm turlar, eskiden yeniye sıralı. */
    public List<CouncilResult> getThreadTurns(Long threadId) {
        List<CouncilResult> turns = conversationRepository.findByThreadIdOrderByCreatedAtAsc(threadId).stream()
                .map(entity -> objectMapper.readValue(entity.getTranscript(), CouncilResult.class))
                .toList();
        if (turns.isEmpty()) {
            throw new ConversationNotFoundException("Konuşma bulunamadı: " + threadId);
        }
        return turns;
    }

    private ConversationSummary toSummary(ConversationThreadEntity thread) {
        long turnCount = conversationRepository.countByThreadId(thread.getId());
        return new ConversationSummary(thread.getId(), thread.getTitle(), thread.getCreatedAt(), thread.getUpdatedAt(), turnCount);
    }
}
