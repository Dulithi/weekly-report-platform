package com.weeklyreport.report.repository;

import java.time.Instant;
import java.util.UUID;

public interface ReportFirstSubmissionProjection {

    UUID getReportId();

    Instant getFirstSubmittedAt();
}
