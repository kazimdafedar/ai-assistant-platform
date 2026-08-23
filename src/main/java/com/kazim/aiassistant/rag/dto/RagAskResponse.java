package com.kazim.aiassistant.rag.dto;

import java.util.List;

public record RagAskResponse(
        String answer,
        List<SourceCitation> sources
) {
    public record SourceCitation(String title, String excerpt, double score) {
    }
}
