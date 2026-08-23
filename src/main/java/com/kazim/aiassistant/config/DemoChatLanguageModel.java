package com.kazim.aiassistant.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * Offline-friendly chat model for local demos and CI.
 * Uses retrieved context when the prompt includes "Context:".
 */
public class DemoChatLanguageModel implements ChatLanguageModel {

    private static final String RAG_QUESTION_MARKER = "\n\nQuestion: ";

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        String lastUserText = messages.stream()
                .filter(message -> message.type() == ChatMessageType.USER)
                .reduce((first, second) -> second)
                .map(DemoChatLanguageModel::messageText)
                .orElse("");

        int questionMarker = lastUserText.lastIndexOf(RAG_QUESTION_MARKER);
        if (questionMarker >= 0 && lastUserText.contains("Context:")) {
            int contextStart = lastUserText.indexOf("Context:") + "Context:".length();
            String context = lastUserText.substring(contextStart, questionMarker).trim();
            String question = lastUserText.substring(questionMarker + RAG_QUESTION_MARKER.length()).trim();
            String answer = """
                    [Demo mode — set OPENAI_API_KEY and APP_DEMO_MODE=false for real LLM answers]

                    Based on the provided documents, here is a concise answer to "%s":

                    %s
                    """.formatted(question, summarizeContext(context));
            return Response.from(AiMessage.from(answer.trim()));
        }

        return Response.from(AiMessage.from("""
                [Demo mode — set OPENAI_API_KEY and APP_DEMO_MODE=false for ChatGPT-like responses]

                I received your message and can help with general chat, RAG document Q&A, and customer support scenarios.
                Try POST /api/rag/documents then POST /api/rag/ask, or POST /api/support/chat.

                Your message: %s
                """.formatted(lastUserText).trim()));
    }

    private static String messageText(ChatMessage message) {
        return switch (message.type()) {
            case USER -> ((UserMessage) message).singleText();
            case AI -> ((AiMessage) message).text();
            default -> message.toString();
        };
    }

    private static String summarizeContext(String context) {
        if (context.isBlank()) {
            return "No relevant context was found in the knowledge base.";
        }
        String trimmed = context.length() > 500 ? context.substring(0, 500) + "..." : context;
        return trimmed.replace("\n", " ").trim();
    }
}
