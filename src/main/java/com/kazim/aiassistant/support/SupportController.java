package com.kazim.aiassistant.support;

import com.kazim.aiassistant.support.dto.SupportChatRequest;
import com.kazim.aiassistant.support.dto.SupportChatResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
@Tag(name = "Support Bot", description = "Customer support AI chatbot with tools and escalation")
public class SupportController {

    private final SupportBotService supportBotService;

    public SupportController(SupportBotService supportBotService) {
        this.supportBotService = supportBotService;
    }

    @PostMapping("/chat")
    public SupportChatResponse chat(@Valid @RequestBody SupportChatRequest request) {
        return supportBotService.chat(request);
    }
}
