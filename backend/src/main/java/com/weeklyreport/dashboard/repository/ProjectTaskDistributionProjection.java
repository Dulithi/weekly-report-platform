package com.weeklyreport.dashboard.repository;

import java.util.UUID;

public interface ProjectTaskDistributionProjection {

    UUID getProjectId();

    String getProjectName();

    long getTaskCount();
}
