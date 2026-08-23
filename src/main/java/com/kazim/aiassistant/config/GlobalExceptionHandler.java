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
        if (ex.code() != 401 && ex.code() != 403) {
            throw ex;
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "OpenAI authentication failed",
                        "message", "Invalid or missing OPENAI_API_KEY. Leave APP_DEMO_MODE=true (default) or set a real API key."
                ));
    }
}
