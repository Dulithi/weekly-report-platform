package com.weeklyreport.review.dto;

import java.time.Instant;
import java.util.UUID;

import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.dto.ManagerReportMemberResponse;
import com.weeklyreport.review.ReviewAction;

public record ReviewResponse(
        UUID id,
        UUID reportId,
        UUID reviewedVersionId,
        int reviewedVersionNumber,
        ReviewAction action,
        String comment,
        ManagerReportMemberResponse reviewer,
        Instant createdAt,
        ReportStatus reportStatus,
        UUID editableVersionId,
        Integer editableVersionNumber
) {
}
