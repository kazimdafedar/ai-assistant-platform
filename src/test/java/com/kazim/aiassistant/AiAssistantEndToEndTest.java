package com.kazim.aiassistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class AiAssistantEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @Order(2)
    @DisplayName("Actuator")
    class Actuator {

        @Test
        void healthEndpointIsUp() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Nested
    @Order(1)
    @DisplayName("RAG API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Rag {

        @Test
        @Order(1)
        void askWithNoDocumentsReturnsEmptySources() throws Exception {
            mockMvc.perform(post("/api/rag/ask")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question":"What is the refund window?"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").isNotEmpty())
                    .andExpect(jsonPath("$.sources", empty()));
        }

        @Test
        @Order(2)
        void ingestAndAskReturnsCitations() throws Exception {
            mockMvc.perform(post("/api/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Refund Policy",
                                      "content": "Customers can request a refund within 30 days of purchase. Contact support with order id ORD-1001."
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.documentId").isNotEmpty())
                    .andExpect(jsonPath("$.chunksCreated").value(1));

            mockMvc.perform(post("/api/rag/ask")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question":"What is the refund window?"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").isNotEmpty())
                    .andExpect(jsonPath("$.sources", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.sources[0].title").value("Refund Policy"))
                    .andExpect(jsonPath("$.sources[0].excerpt").isNotEmpty())
                    .andExpect(jsonPath("$.sources[0].score").isNumber());
        }

        @Test
        void rejectsIngestWithBlankTitle() throws Exception {
            mockMvc.perform(post("/api/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"","content":"Some policy text"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsIngestWithBlankContent() throws Exception {
            mockMvc.perform(post("/api/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Policy","content":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsIngestWithMissingFields() throws Exception {
            mockMvc.perform(post("/api/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsAskWithBlankQuestion() throws Exception {
            mockMvc.perform(post("/api/rag/ask")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsAskWithMissingQuestion() throws Exception {
            mockMvc.perform(post("/api/rag/ask")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @Order(3)
    @DisplayName("Chat API")
    class Chat {

        @Test
        void startsNewSessionWhenSessionIdOmitted() throws Exception {
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"Hello, what can you do?"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").isNotEmpty())
                    .andExpect(jsonPath("$.reply").isNotEmpty())
                    .andExpect(jsonPath("$.history", hasSize(2)));
        }

        @Test
        void supportsMultiTurnConversationWithSameSessionId() throws Exception {
            MvcResult firstTurn = mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"What modules does this platform offer?"}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn();

            String sessionId = objectMapper.readTree(firstTurn.getResponse().getContentAsString())
                    .get("sessionId").asText();

            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"sessionId":"%s","message":"Tell me more about RAG"}
                                    """.formatted(sessionId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId))
                    .andExpect(jsonPath("$.reply").isNotEmpty())
                    .andExpect(jsonPath("$.history", hasSize(4)));

            mockMvc.perform(get("/api/chat/" + sessionId + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[0].role").value("USER"))
                    .andExpect(jsonPath("$[1].role").value("AI"))
                    .andExpect(jsonPath("$[2].role").value("USER"))
                    .andExpect(jsonPath("$[3].role").value("AI"));
        }

        @Test
        void historyReturnsEmptyListForUnknownSession() throws Exception {
            mockMvc.perform(get("/api/chat/unknown-session-id/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", empty()));
        }

        @Test
        void rejectsChatWithBlankMessage() throws Exception {
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsChatWithMissingMessage() throws Exception {
            mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @Order(4)
    @DisplayName("Support API")
    class Support {

        @Test
        void looksUpOrderOrd1001() throws Exception {
            mockMvc.perform(post("/api/support/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"What is the status of order ORD-1001?"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.escalated").value(false))
                    .andExpect(jsonPath("$.reply").value("Order ORD-1001 status: Shipped — expected delivery Aug 28"))
                    .andExpect(jsonPath("$.note").value("Resolved using order lookup tool."));
        }

        @Test
        void createsSupportTicketForComplaint() throws Exception {
            mockMvc.perform(post("/api/support/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"I have a complaint about a delayed shipment"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.escalated").value(false))
                    .andExpect(jsonPath("$.reply").value(containsString("Support ticket TCK-")))
                    .andExpect(jsonPath("$.reply").value(containsString("customer@example.com")))
                    .andExpect(jsonPath("$.note").value("Created support ticket via tool."));
        }

        @Test
        void escalatesWhenCustomerRequestsHuman() throws Exception {
            mockMvc.perform(post("/api/support/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"Please speak to human about my billing issue"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.escalated").value(true))
                    .andExpect(jsonPath("$.reply", notNullValue()))
                    .andExpect(jsonPath("$.note").value("Escalated because the customer requested a human agent."));
        }

        @Test
        void rejectsChatWithBlankMessage() throws Exception {
            mockMvc.perform(post("/api/support/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":""}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsChatWithMissingMessage() throws Exception {
            mockMvc.perform(post("/api/support/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @Order(5)
    @DisplayName("Full platform flow")
    class FullPlatformFlow {

        @Test
        void exercisesAllModulesEndToEnd() throws Exception {
            mockMvc.perform(post("/api/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Shipping FAQ",
                                      "content": "Standard shipping takes 5-7 business days. Express shipping is available at checkout."
                                    }
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/api/rag/ask")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question":"How long does standard shipping take?"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").isNotEmpty())
                    .andExpect(jsonPath("$.sources", hasSize(greaterThanOrEqualTo(1))));

            MvcResult chatResult = mockMvc.perform(post("/api/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"Summarize the platform capabilities"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").isNotEmpty())
                    .andReturn();

            String sessionId = objectMapper.readTree(chatResult.getResponse().getContentAsString())
                    .get("sessionId").asText();

            mockMvc.perform(get("/api/chat/" + sessionId + "/history"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            mockMvc.perform(post("/api/support/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"Please speak to human about my billing issue"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.escalated").value(true));

            mockMvc.perform(post("/api/support/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"What is the status of order ORD-1001?"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reply", notNullValue()));

            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }
}
