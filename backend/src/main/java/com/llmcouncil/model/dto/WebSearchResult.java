package com.llmcouncil.model.dto;

/** Tavily arama sonuçlarından tek bir kayıt. */
public record WebSearchResult(String title, String url, String content) {
}
