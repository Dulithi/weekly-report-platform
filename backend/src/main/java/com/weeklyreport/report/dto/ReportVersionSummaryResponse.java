package com.weeklyreport.report.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportVersionSummaryResponse(
        UUID id,
        int versionNumber,
        Instant createdAt,
        Instant submittedAt,
        boolean submitted,
        boolean current
) {
}
