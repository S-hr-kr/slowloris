package com.slowloris.monitor.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.openai.api-key:your-api-key-here}")
    private String apiKey;

    @Value("${langchain4j.openai.model:gpt-3.5-turbo}")
    private String modelName;

    @Value("${langchain4j.openai.temperature:0.3}")
    private Double temperature;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .build();
    }
}
