package com.weeklyreport.assistant.model;

import java.util.List;

public record AssistantModelRequest(
        String instructions,
        String question,
        List<AssistantModelMessage> history,
        String contextJson,
        int maxOutputTokens
) {
}
