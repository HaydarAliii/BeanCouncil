package com.llmcouncil.controller;

import com.llmcouncil.model.dto.ConversationSummary;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.service.ConversationHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Geçmişte sorulan konuşmaları listeler ve tam transcript detayını döner. */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationHistoryService historyService;

    public ConversationController(ConversationHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<ConversationSummary> list() {
        return historyService.listAll();
    }

    @GetMapping("/{id}")
    public CouncilResult detail(@PathVariable Long id) {
        return historyService.getDetail(id);
    }
}
