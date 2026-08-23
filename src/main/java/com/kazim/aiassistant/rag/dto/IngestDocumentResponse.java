package com.kazim.aiassistant.rag.dto;

public record IngestDocumentResponse(
        String documentId,
        String title,
        int chunksCreated
) {
}
