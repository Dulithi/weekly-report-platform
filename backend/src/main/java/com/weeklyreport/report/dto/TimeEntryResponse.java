package com.weeklyreport.report.dto;

import java.util.UUID;

import com.weeklyreport.report.TaskType;

public record TimeEntryResponse(
        UUID id,
        TaskType taskType,
        int minutes
) {
}