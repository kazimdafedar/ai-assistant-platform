package com.kazim.aiassistant.support;

import com.kazim.aiassistant.support.dto.SupportChatRequest;
import com.kazim.aiassistant.support.dto.SupportChatResponse;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SupportBotService {

    private static final Pattern ORDER_PATTERN = Pattern.compile("ORD-\\d{4}", Pattern.CASE_INSENSITIVE);
    private static final String ESCALATION_KEYWORD = "speak to human";

    private final ChatLanguageModel chatLanguageModel;
    private final SupportTools supportTools;

    public SupportBotService(ChatLanguageModel chatLanguageModel, SupportTools supportTools) {
        this.chatLanguageModel = chatLanguageModel;
        this.supportTools = supportTools;
    }

    public SupportChatResponse chat(SupportChatRequest request) {
        String message = request.message();

        if (message.toLowerCase().contains(ESCALATION_KEYWORD)) {
            return new SupportChatResponse(
                    "I've escalated this conversation to a human support agent. You will receive an email update shortly.",
                    true,
                    "Escalated because the customer requested a human agent."
            );
        }

        Matcher orderMatcher = ORDER_PATTERN.matcher(message);
        if (orderMatcher.find()) {
            String orderId = orderMatcher.group().toUpperCase();
            String status = supportTools.lookupOrderStatus(orderId);
            return new SupportChatResponse(
                    "Order %s status: %s".formatted(orderId, status),
                    false,
                    "Resolved using order lookup tool."
            );
        }

        if (message.toLowerCase().contains("ticket") || message.toLowerCase().contains("complaint")) {
            String ticket = supportTools.createSupportTicket("customer@example.com", message);
            return new SupportChatResponse(ticket, false, "Created support ticket via tool.");
        }

        String prompt = """
                You are a concise customer support assistant.
                Answer this customer message in 2-3 sentences.
                Customer message: %s
                """.formatted(message);

        String reply = chatLanguageModel.generate(UserMessage.from(prompt)).content().text();
        return new SupportChatResponse(reply, false, null);
    }
}
