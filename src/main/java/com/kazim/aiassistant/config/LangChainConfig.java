package com.kazim.aiassistant.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    @Bean
    EmbeddingStore store() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    ChatLanguageModel chatLanguageModel(
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.chat-model:gpt-4o-mini}") String modelName,
            @Value("${app.demo-mode:true}") boolean demoMode
    ) {
        if (!demoMode && OpenAiApiKeyValidator.isUsableOpenAiApiKey(apiKey)) {
            return OpenAiChatModel.builder()
                    .apiKey(apiKey.trim())
                    .modelName(modelName)
                    .temperature(0.3)
                    .build();
        }
        return new DemoChatLanguageModel();
    }
}
