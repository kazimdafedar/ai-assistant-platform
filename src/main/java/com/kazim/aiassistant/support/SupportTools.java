package com.kazim.aiassistant.support;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class SupportTools {

    private static final Map<String, String> ORDERS = Map.of(
            "ORD-1001", "Shipped — expected delivery Aug 28",
            "ORD-1002", "Processing — payment confirmed",
            "ORD-1003", "Delivered on Aug 20"
    );

    @Tool("Look up the status of a customer order by order id, for example ORD-1001")
    public String lookupOrderStatus(String orderId) {
        return Optional.ofNullable(ORDERS.get(orderId.toUpperCase()))
                .orElse("Order not found. Please verify the order id.");
    }

    @Tool("Create a support ticket for a customer issue")
    public String createSupportTicket(String customerEmail, String issueSummary) {
        String ticketId = "TCK-" + (1000 + Math.abs(issueSummary.hashCode() % 9000));
        return "Support ticket %s created for %s. Our team will respond within 24 hours."
                .formatted(ticketId, customerEmail);
    }
}
