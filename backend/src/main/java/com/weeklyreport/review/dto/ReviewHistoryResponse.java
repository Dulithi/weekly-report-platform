package com.weeklyreport.review.dto;

import java.time.Instant;
import java.util.UUID;

import com.weeklyreport.report.dto.ManagerReportMemberResponse;
import com.weeklyreport.review.ReviewAction;

public record ReviewHistoryResponse(
        UUID id,
        UUID reportVersionId,
        int reportVersionNumber,
        ReviewAction action,
        String comment,
        ManagerReportMemberResponse reviewer,
        Instant createdAt
) {
}
