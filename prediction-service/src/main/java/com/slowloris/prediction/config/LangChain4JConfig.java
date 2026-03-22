package com.slowloris.prediction.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4JConfig {

    @Value("${deepseek.api.key}")
    private String deepSeekApiKey;

    @Value("${deepseek.api.model}")
    private String deepSeekModel;

    @Value("${deepseek.api.base-url}")
    private String deepSeekBaseUrl;

    @Value("${deepseek.api.temperature}")
    private double deepSeekTemperature;

    @Bean
    public OpenAiChatModel deepSeekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(deepSeekApiKey)
                .modelName(deepSeekModel)
                .baseUrl(deepSeekBaseUrl)
                .temperature(deepSeekTemperature)
                .build();
    }
}