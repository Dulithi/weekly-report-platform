package com.weeklyreport.assistant.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AssistantSourceResponse(
        String sourceKey,
        UUID reportId,
        int versionNumber,
        String memberName,
        LocalDate weekStart,
        String href
) {
}
