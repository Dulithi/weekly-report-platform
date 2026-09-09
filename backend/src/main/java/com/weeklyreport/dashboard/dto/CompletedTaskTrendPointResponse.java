package com.weeklyreport.dashboard.dto;

import java.time.LocalDate;

public record CompletedTaskTrendPointResponse(
        LocalDate weekStart,
        long completedTasks
) {
}
