package com.weeklyreport.report.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CreateWeeklyReportRequest(

        @NotNull
        LocalDate weekStart
) {
}