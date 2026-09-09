package com.weeklyreport.assistant.dto;

import java.time.LocalDate;
import java.util.List;

public record AssistantResponse(
        String answer,
        LocalDate fromWeek,
        LocalDate throughWeek,
        int reportsConsidered,
        List<AssistantSourceResponse> sources
) {
}
