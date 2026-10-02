package com.llmcouncil.model.dto;

import java.util.List;

public record CouncilResult(
        String prompt,
        List<LlmResponse> firstOpinions,
        List<LlmResponse> reviews,
        String presidentProvider,
        String finalAnswer,
        Long threadId) {
}
