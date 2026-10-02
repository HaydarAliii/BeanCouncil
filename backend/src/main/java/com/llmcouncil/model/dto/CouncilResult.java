package com.llmcouncil.model.dto;

import java.util.List;

public record CouncilResult(
        String prompt,
        List<LlmResponse> firstOpinions,
        List<LlmResponse> reviews,
        String presidentProvider,
        String finalAnswer,
        Long threadId,
        List<WebSearchResult> webSearchResults) {

    /** webSearchResults alanı eski (bu özellikten önceki) transcript JSON'larında hiç yok — eksikse boş listeye normalize eder. */
    public CouncilResult {
        if (webSearchResults == null) {
            webSearchResults = List.of();
        }
    }
}
