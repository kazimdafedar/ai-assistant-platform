package com.kazim.aiassistant.config;

import java.util.Locale;
import java.util.Set;

/**
 * Detects whether an OpenAI API key looks usable or is a placeholder/example value.
 */
final class OpenAiApiKeyValidator {

    private static final Set<String> PLACEHOLDER_KEYS = Set.of(
            "sk-your-key",
            "sk-your-openai-api-key",
            "sk-xxx",
            "sk-placeholder",
            "sk-test",
            "sk-fake",
            "sk-dummy",
            "sk-example"
    );

    private OpenAiApiKeyValidator() {
    }

    static boolean isUsableOpenAiApiKey(String apiKey) {
        if (apiKey == null) {
            return false;
        }
        String trimmed = apiKey.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (!trimmed.startsWith("sk-")) {
            return false;
        }
        if (trimmed.length() <= 20) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("sk-your")) {
            return false;
        }
        if (PLACEHOLDER_KEYS.contains(lower)) {
            return false;
        }
        if (lower.contains("placeholder") || lower.contains("your-key") || lower.contains("your_api_key")) {
            return false;
        }
        return true;
    }
}
