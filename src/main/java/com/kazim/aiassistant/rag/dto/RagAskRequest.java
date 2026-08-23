package com.kazim.aiassistant.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record RagAskRequest(
        @NotBlank String question
) {
}
