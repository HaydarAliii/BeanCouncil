package com.llmcouncil.model.dto;

public record LlmResponse(String providerName, String content, boolean success, String errorMessage) {

    public static LlmResponse ok(String providerName, String content) {
        return new LlmResponse(providerName, content, true, null);
    }

    public static LlmResponse failed(String providerName, String errorMessage) {
        return new LlmResponse(providerName, null, false, errorMessage);
    }
}
