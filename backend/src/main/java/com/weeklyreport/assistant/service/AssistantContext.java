package com.weeklyreport.assistant.service;

import java.time.LocalDate;
import java.util.List;

record AssistantContext(
        LocalDate fromWeek,
        LocalDate throughWeek,
        String json,
        List<AssistantSource> sources
) {
}
