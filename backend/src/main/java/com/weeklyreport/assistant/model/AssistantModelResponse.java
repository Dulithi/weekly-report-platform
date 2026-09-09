package com.weeklyreport.assistant.model;

import java.util.List;

public record AssistantModelResponse(
        String answer,
        List<String> sourceKeys,
        Integer inputTokens,
        Integer outputTokens
) {
    public AssistantModelResponse {
        sourceKeys = sourceKeys == null ? List.of() : List.copyOf(sourceKeys);
    }
}
