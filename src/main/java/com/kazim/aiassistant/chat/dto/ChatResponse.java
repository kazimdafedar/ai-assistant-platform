package com.kazim.aiassistant.chat.dto;

import java.util.List;

public record ChatResponse(
        String sessionId,
        String reply,
        List<ChatMessageDto> history
) {
}
