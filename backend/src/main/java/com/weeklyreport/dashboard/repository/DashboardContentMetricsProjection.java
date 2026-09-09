package com.weeklyreport.dashboard.repository;

public interface DashboardContentMetricsProjection {

    long getNeedsCorrectionReports();

    long getOpenBlockers();
}
