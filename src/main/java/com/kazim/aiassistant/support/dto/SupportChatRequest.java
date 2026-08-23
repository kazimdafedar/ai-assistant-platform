package com.kazim.aiassistant.support.dto;

import jakarta.validation.constraints.NotBlank;

public record SupportChatRequest(
        @NotBlank String message
) {
}
