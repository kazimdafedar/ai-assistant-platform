package com.kazim.aiassistant.config;

import dev.ai4j.openai4j.OpenAiHttpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OpenAiHttpException.class)
    public ResponseEntity<Map<String, String>> handleOpenAiHttpException(OpenAiHttpException ex) {
        if (ex.code() == 401 || ex.code() == 403) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "OpenAI authentication failed",
                            "message", "Invalid or missing OPENAI_API_KEY. Leave APP_DEMO_MODE=true (default) or set a real API key."
                    ));
        }
        if (isInsufficientQuota(ex)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "OpenAI quota exceeded",
                            "message", "Your OpenAI account has no remaining quota. Enable billing and add credits at https://platform.openai.com/account/billing, or set APP_DEMO_MODE=true to use the local demo LLM without OpenAI."
                    ));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "OpenAI request failed",
                        "message", "OpenAI returned HTTP " + ex.code() + ". Leave APP_DEMO_MODE=true (default) or check your API key and billing."
                ));
    }

    private static boolean isInsufficientQuota(OpenAiHttpException ex) {
        if (ex.code() == 429) {
            return true;
        }
        String message = ex.getMessage();
        return message != null && message.toLowerCase().contains("insufficient_quota");
    }
}
