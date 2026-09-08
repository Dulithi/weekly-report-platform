package com.weeklyreport.report.repository;

public interface TeamMemberReportStatisticsProjection {

    long getTotalReports();

    long getDraftReports();

    long getSubmittedReports();

    long getNeedsCorrectionReports();

    long getApprovedReports();
}
