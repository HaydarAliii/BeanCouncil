package com.llmcouncil.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String prompt) {
}
