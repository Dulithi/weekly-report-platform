package com.weeklyreport.assistant.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.Timeout;
import com.weeklyreport.assistant.model.AssistantModelClient;
import com.weeklyreport.assistant.model.OpenAiAssistantModelClient;
import com.weeklyreport.assistant.model.UnavailableAssistantModelClient;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
public class AssistantConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.assistant", name = "enabled", havingValue = "true")
    AssistantModelClient openAiAssistantModelClient(
            AssistantProperties properties,
            ObjectMapper objectMapper
    ) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is required when the AI assistant is enabled"
            );
        }
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .baseUrl(properties.baseUrl())
                .timeout(Timeout.builder()
                        .connect(properties.connectTimeout())
                        .request(properties.requestTimeout())
                        .build())
                .maxRetries(0)
                .build();
        return new OpenAiAssistantModelClient(client, objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "app.assistant",
            name = "enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    AssistantModelClient unavailableAssistantModelClient() {
        return new UnavailableAssistantModelClient();
    }
}
