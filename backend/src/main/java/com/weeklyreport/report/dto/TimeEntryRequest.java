package com.weeklyreport.report.dto;

import com.weeklyreport.report.TaskType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TimeEntryRequest(

        @NotNull
        TaskType taskType,

        @Min(0)
        int minutes
) {
}