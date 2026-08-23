package com.kazim.aiassistant.rag;

import com.kazim.aiassistant.rag.dto.IngestDocumentRequest;
import com.kazim.aiassistant.rag.dto.IngestDocumentResponse;
import com.kazim.aiassistant.rag.dto.RagAskRequest;
import com.kazim.aiassistant.rag.dto.RagAskResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG", description = "Retrieval-Augmented Generation over ingested documents")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public IngestDocumentResponse ingest(@Valid @RequestBody IngestDocumentRequest request) {
        return ragService.ingest(request);
    }

    @PostMapping("/ask")
    public RagAskResponse ask(@Valid @RequestBody RagAskRequest request) {
        return ragService.ask(request);
    }
}
