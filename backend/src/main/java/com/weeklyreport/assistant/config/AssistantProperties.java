package com.weeklyreport.assistant.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.assistant")
public record AssistantProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        String reasoningEffort,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout,
        @Min(1) @Max(5000) int maxQuestionCharacters,
        @Min(0) @Max(20) int maxHistoryMessages,
        @Min(0) @Max(10000) int maxHistoryCharacters,
        @Min(1) @Max(52) int maxWeeks,
        @Min(1) @Max(100) int maxReports,
        @Min(1000) @Max(200000) int maxContextCharacters,
        @Min(64) @Max(4000) int maxOutputTokens,
        @Valid @NotNull RateLimit rateLimit
) {
    public AssistantProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Assistant base URL is required");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Assistant model is required");
        }
        if (!List.of("none", "low", "medium", "high", "xhigh", "max")
                .contains(reasoningEffort)) {
            throw new IllegalArgumentException("Unsupported assistant reasoning effort");
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || connectTimeout.compareTo(Duration.ofSeconds(30)) > 0) {
            throw new IllegalArgumentException("Assistant connect timeout must be between 1ms and 30s");
        }
        if (requestTimeout == null || requestTimeout.compareTo(connectTimeout) < 0
                || requestTimeout.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalArgumentException(
                    "Assistant request timeout must include connect timeout and be at most 2m"
            );
        }
    }

    public record RateLimit(
            @Min(1) @Max(1000) int maxRequests,
            @NotNull Duration window
    ) {
        public RateLimit {
            if (window != null && (window.compareTo(Duration.ofSeconds(1)) < 0
                    || window.compareTo(Duration.ofDays(1)) > 0 || window.getNano() != 0)) {
                throw new IllegalArgumentException(
                        "Assistant request window must be whole seconds between 1 second and 1 day"
                );
            }
        }
    }
}
