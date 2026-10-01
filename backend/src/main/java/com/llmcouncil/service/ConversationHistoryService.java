package com.llmcouncil.service;

import com.llmcouncil.exception.ConversationNotFoundException;
import com.llmcouncil.model.dto.ConversationSummary;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.model.entity.ConversationEntity;
import com.llmcouncil.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Geçmişte sorulan konuşmaları listeler/detaylandırır. Detay, {@code CouncilService.persist}'in
 * {@code transcript} kolonuna yazdığı tam {@link CouncilResult} JSON'ını geri çözer — konsey
 * akışıyla aynı DTO olduğu için frontend'deki FinalAnswer/ProcessDetails bileşenleri aynen
 * yeniden kullanılabilir.
 */
@Service
public class ConversationHistoryService {

    private final ConversationRepository repository;
    private final ObjectMapper objectMapper;

    public ConversationHistoryService(ConversationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<ConversationSummary> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(entity -> new ConversationSummary(entity.getId(), entity.getPrompt(), entity.getCreatedAt()))
                .toList();
    }

    public CouncilResult getDetail(Long id) {
        ConversationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ConversationNotFoundException("Konuşma bulunamadı: " + id));
        return objectMapper.readValue(entity.getTranscript(), CouncilResult.class);
    }
}
