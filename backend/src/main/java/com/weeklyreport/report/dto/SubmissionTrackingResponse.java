package com.weeklyreport.report.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.weeklyreport.report.SubmissionTiming;
import com.weeklyreport.report.SubmissionTrackingStatus;

public record SubmissionTrackingResponse(
        ManagerReportMemberResponse member,
        UUID reportId,
        LocalDate weekStart,
        LocalDate weekEnd,
        SubmissionTrackingStatus status,
        SubmissionTiming timing,
        Instant dueAt,
        Instant firstSubmittedAt
) {
}
