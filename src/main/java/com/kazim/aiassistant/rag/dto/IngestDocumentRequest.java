package com.kazim.aiassistant.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record IngestDocumentRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
