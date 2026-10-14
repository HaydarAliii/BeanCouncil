package com.llmcouncil.model.dto;

/** A single record from the Tavily search results. */
public record WebSearchResult(String title, String url, String content) {
}
