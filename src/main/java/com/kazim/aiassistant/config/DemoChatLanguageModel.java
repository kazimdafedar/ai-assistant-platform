package com.kazim.aiassistant.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Offline-friendly chat model for local demos and CI.
 * Uses retrieved context when the prompt includes "Context:".
 */
public class DemoChatLanguageModel implements ChatLanguageModel {

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        String userText = messages.stream()
                .filter(message -> message.type() == dev.langchain4j.data.message.ChatMessageType.USER)
                .map(Object::toString)
                .collect(Collectors.joining("\n"));

        if (userText.contains("Context:")) {
            String context = userText.substring(userText.indexOf("Context:") + "Context:".length(),
                    userText.indexOf("\n\nQuestion:")).trim();
            String question = userText.substring(userText.indexOf("Question:") + "Question:".length()).trim();
            String answer = """
                    [Demo mode — set OPENAI_API_KEY and APP_DEMO_MODE=false for real LLM answers]

                    Based on the provided documents, here is a concise answer to "%s":

                    %s
                    """.formatted(question, summarizeContext(context));
            return Response.from(AiMessage.from(answer.trim()));
        }

        String lastUserMessage = messages.isEmpty() ? "" : messages.get(messages.size() - 1).toString();
        return Response.from(AiMessage.from("""
                [Demo mode — set OPENAI_API_KEY and APP_DEMO_MODE=false for ChatGPT-like responses]

                I received your message and can help with general chat, RAG document Q&A, and customer support scenarios.
                Try POST /api/rag/documents then POST /api/rag/ask, or POST /api/support/chat.

                Your message: %s
                """.formatted(lastUserMessage).trim()));
    }

    private static String summarizeContext(String context) {
        if (context.isBlank()) {
            return "No relevant context was found in the knowledge base.";
        }
        String trimmed = context.length() > 500 ? context.substring(0, 500) + "..." : context;
        return trimmed.replace("\n", " ").trim();
    }
}
