package com.kazim.aiassistant.chat;

import com.kazim.aiassistant.chat.dto.ChatMessageDto;
import com.kazim.aiassistant.chat.dto.ChatRequest;
import com.kazim.aiassistant.chat.dto.ChatResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "ChatGPT-like conversational chat with session memory")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/{sessionId}/history")
    public List<ChatMessageDto> history(@PathVariable String sessionId) {
        return chatService.history(sessionId);
    }
}
