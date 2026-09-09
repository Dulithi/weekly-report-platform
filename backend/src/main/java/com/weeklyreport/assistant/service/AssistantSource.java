package com.weeklyreport.assistant.service;

import java.time.LocalDate;
import java.util.UUID;

record AssistantSource(
        String sourceKey,
        UUID reportId,
        int versionNumber,
        String memberName,
        LocalDate weekStart
) {
}
