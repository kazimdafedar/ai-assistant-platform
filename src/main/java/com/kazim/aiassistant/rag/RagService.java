package com.kazim.aiassistant.rag;

import com.kazim.aiassistant.rag.dto.IngestDocumentRequest;
import com.kazim.aiassistant.rag.dto.IngestDocumentResponse;
import com.kazim.aiassistant.rag.dto.RagAskRequest;
import com.kazim.aiassistant.rag.dto.RagAskResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RagService {

    private static final int MAX_RESULTS = 3;
    private static final double MIN_SCORE = 0.5;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatLanguageModel;
    private final DocumentSplitter splitter = DocumentSplitters.recursive(300, 50);

    public RagService(
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            ChatLanguageModel chatLanguageModel
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatLanguageModel = chatLanguageModel;
    }

    public IngestDocumentResponse ingest(IngestDocumentRequest request) {
        String documentId = UUID.randomUUID().toString();
        Metadata metadata = new Metadata();
        metadata.put("title", request.title());
        metadata.put("documentId", documentId);
        Document document = Document.from(request.content(), metadata);

        List<TextSegment> segments = splitter.split(document).stream()
                .map(segment -> {
                    Metadata segmentMetadata = segment.metadata().copy();
                    segmentMetadata.put("title", request.title());
                    segmentMetadata.put("documentId", documentId);
                    return TextSegment.from(segment.text(), segmentMetadata);
                })
                .toList();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        return new IngestDocumentResponse(documentId, request.title(), segments.size());
    }

    public RagAskResponse ask(RagAskRequest request) {
        Embedding queryEmbedding = embeddingModel.embed(request.question()).content();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, MAX_RESULTS, MIN_SCORE);

        String context = matches.stream()
                .map(match -> match.embedded().text())
                .reduce((left, right) -> left + "\n---\n" + right)
                .orElse("");

        String prompt = """
                You are a helpful assistant. Answer the question using ONLY the context below.
                If the answer is not in the context, say you do not have enough information.

                Context:
                %s

                Question: %s
                """.formatted(context, request.question());

        Response<dev.langchain4j.data.message.AiMessage> response =
                chatLanguageModel.generate(List.of(UserMessage.from(prompt)));

        List<RagAskResponse.SourceCitation> sources = matches.stream()
                .map(match -> new RagAskResponse.SourceCitation(
                        segmentTitle(match.embedded()),
                        excerpt(match.embedded().text()),
                        match.score()
                ))
                .toList();

        return new RagAskResponse(response.content().text(), sources);
    }

    private static String excerpt(String text) {
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }

    private static String segmentTitle(TextSegment segment) {
        Metadata metadata = segment.metadata();
        String title = metadata.getString("title");
        if (title != null && !title.isBlank()) {
            return title;
        }
        String documentId = metadata.getString("documentId");
        return documentId != null ? documentId : "Untitled";
    }
}
