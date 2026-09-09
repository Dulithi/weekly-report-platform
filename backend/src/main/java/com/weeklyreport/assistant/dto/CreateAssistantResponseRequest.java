package com.weeklyreport.assistant.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAssistantResponseRequest(
        @NotBlank @Size(max = 500) String question,
        LocalDate weekStart,
        @Min(1) @Max(12) Integer weekCount,
        @Size(max = 6) List<@Valid AssistantChatMessageRequest> history
) {
    public CreateAssistantResponseRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
