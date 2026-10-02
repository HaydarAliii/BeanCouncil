package com.llmcouncil.controller;

import com.llmcouncil.model.dto.ChatRequest;
import com.llmcouncil.model.dto.CouncilResult;
import com.llmcouncil.service.CouncilService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/council")
public class CouncilController {

    private final CouncilService councilService;

    public CouncilController(CouncilService councilService) {
        this.councilService = councilService;
    }

    @PostMapping("/ask")
    public CouncilResult ask(@Valid @RequestBody ChatRequest request) {
        return councilService.deliberate(request.prompt(), request.threadId());
    }
}
