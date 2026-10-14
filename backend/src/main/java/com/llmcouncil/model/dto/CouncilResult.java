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

    /** The webSearchResults field doesn't exist in older transcript JSON (predating this feature) — normalizes a missing value to an empty list. */
    public CouncilResult {
        if (webSearchResults == null) {
            webSearchResults = List.of();
        }
    }
}
