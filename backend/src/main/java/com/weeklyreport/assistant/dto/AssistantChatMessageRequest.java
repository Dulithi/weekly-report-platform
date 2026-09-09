package com.weeklyreport.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssistantChatMessageRequest(
        @NotNull AssistantChatRole role,
        @NotBlank @Size(max = 1000) String content
) {
}
