package com.weeklyreport.dashboard.dto;

import com.weeklyreport.report.TaskType;

public record TaskTypeTimeResponse(
        TaskType taskType,
        long minutes
) {
}
