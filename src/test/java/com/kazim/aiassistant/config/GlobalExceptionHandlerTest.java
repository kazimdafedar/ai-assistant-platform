package com.kazim.aiassistant.config;

import dev.ai4j.openai4j.OpenAiHttpException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void maps401ToServiceUnavailableWithAuthMessage() {
        ResponseEntity<Map<String, String>> response = handler.handleOpenAiHttpException(
                new OpenAiHttpException(401, "Incorrect API key provided")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "OpenAI authentication failed");
        assertThat(response.getBody().get("message")).contains("APP_DEMO_MODE=true");
    }

    @Test
    void mapsInsufficientQuotaToServiceUnavailable() {
        ResponseEntity<Map<String, String>> response = handler.handleOpenAiHttpException(
                new OpenAiHttpException(429, "{\"error\":{\"type\":\"insufficient_quota\",\"message\":\"You exceeded your current quota\"}}")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "OpenAI quota exceeded");
        assertThat(response.getBody().get("message")).contains("billing");
    }

    @Test
    void mapsOtherOpenAiErrorsToServiceUnavailable() {
        ResponseEntity<Map<String, String>> response = handler.handleOpenAiHttpException(
                new OpenAiHttpException(500, "Internal server error")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "OpenAI request failed");
        assertThat(response.getBody().get("message")).contains("HTTP 500");
    }
}
