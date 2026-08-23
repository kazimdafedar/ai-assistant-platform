package com.kazim.aiassistant.chat;

import com.kazim.aiassistant.chat.dto.ChatMessageDto;
import com.kazim.aiassistant.chat.dto.ChatRequest;
import com.kazim.aiassistant.chat.dto.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatLanguageModel chatLanguageModel;
    private final Map<String, ChatMemory> sessions = new ConcurrentHashMap<>();

    public ChatService(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId();

        ChatMemory memory = sessions.computeIfAbsent(sessionId, id -> MessageWindowChatMemory.withMaxMessages(20));
        memory.add(UserMessage.from(request.message()));

        Response<AiMessage> response = chatLanguageModel.generate(memory.messages());
        memory.add(response.content());

        List<ChatMessageDto> history = memory.messages().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new ChatResponse(sessionId, response.content().text(), history);
    }

    public List<ChatMessageDto> history(String sessionId) {
        ChatMemory memory = sessions.get(sessionId);
        if (memory == null) {
            return List.of();
        }
        return memory.messages().stream().map(this::toDto).collect(Collectors.toList());
    }

    private ChatMessageDto toDto(ChatMessage message) {
        String content = switch (message.type()) {
            case USER -> ((UserMessage) message).singleText();
            case AI -> ((AiMessage) message).text();
            default -> message.toString();
        };
        return new ChatMessageDto(message.type().name(), content);
    }
}
