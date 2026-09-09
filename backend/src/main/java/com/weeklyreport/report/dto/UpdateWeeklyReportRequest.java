package com.weeklyreport.report.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWeeklyReportRequest(

        @Size(max = 5000)
        String notes,

        List<@Valid CompletedTaskRequest> completedTasks,

        List<@Valid PlannedTaskRequest> plannedTasks,

        List<@Valid BlockerRequest> blockers,

        List<@Valid AchievementRequest> achievements,

        List<@Valid TimeEntryRequest> timeEntries,

        @NotNull
        Long entityVersion
) {

    public UpdateWeeklyReportRequest {

        completedTasks =
                completedTasks == null
                        ? List.of()
                        : List.copyOf(completedTasks);

        plannedTasks =
                plannedTasks == null
                        ? List.of()
                        : List.copyOf(plannedTasks);

        blockers =
                blockers == null
                        ? List.of()
                        : List.copyOf(blockers);

        achievements =
                achievements == null
                        ? List.of()
                        : List.copyOf(achievements);

        timeEntries =
                timeEntries == null
                        ? List.of()
                        : List.copyOf(timeEntries);
    }
}
