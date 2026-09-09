package com.weeklyreport.dashboard.repository;

import java.time.LocalDate;

public interface CompletedTaskTrendProjection {

    LocalDate getWeekStart();

    long getCompletedTasks();
}
