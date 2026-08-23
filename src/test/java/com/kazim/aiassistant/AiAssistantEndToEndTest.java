package com.kazim.aiassistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiAssistantEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullPlatformFlow() throws Exception {
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
                .andExpect(jsonPath("$.sources", hasSize(1)));

        MvcResult chatResult = mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Hello, what can you do?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.reply").isNotEmpty())
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
    }
}
