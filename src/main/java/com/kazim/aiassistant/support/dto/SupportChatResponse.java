package com.kazim.aiassistant.support.dto;

public record SupportChatResponse(
        String reply,
        boolean escalated,
        String note
) {
}
