package com.weeklyreport.report.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.weeklyreport.project.entity.Project;
import com.weeklyreport.report.content.Achievement;
import com.weeklyreport.report.content.Blocker;
import com.weeklyreport.report.content.CompletedTask;
import com.weeklyreport.report.content.PlannedTask;
import com.weeklyreport.report.content.TimeEntry;
import com.weeklyreport.report.dto.AchievementResponse;
import com.weeklyreport.report.dto.BlockerResponse;
import com.weeklyreport.report.dto.CompletedTaskResponse;
import com.weeklyreport.report.dto.PlannedTaskResponse;
import com.weeklyreport.report.dto.ReportVersionResponse;
import com.weeklyreport.report.dto.TimeEntryResponse;
import com.weeklyreport.report.dto.WeeklyReportResponse;
import com.weeklyreport.report.dto.WeeklyReportSummaryResponse;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;

@Component
public class ReportMapper {

    public WeeklyReportSummaryResponse toSummary(
            WeeklyReport report
    ) {

        return new WeeklyReportSummaryResponse(
                report.getId(),
                report.getWeekStart(),
                report.getWeekEnd(),
                report.getStatus(),
                report.getCurrentVersion()
                        .getVersionNumber(),
                report.getSubmittedAt(),
                report.getUpdatedAt()
        );
    }

    public WeeklyReportResponse toResponse(
            WeeklyReport report,
            List<CompletedTask> completedTasks,
            List<PlannedTask> plannedTasks,
            List<Blocker> blockers,
            List<Achievement> achievements,
            List<TimeEntry> timeEntries
    ) {

        ReportVersion version =
                report.getCurrentVersion();

        return new WeeklyReportResponse(
                report.getId(),
                report.getUser().getId(),
                report.getWeekStart(),
                report.getWeekEnd(),
                report.getStatus(),
                report.getSubmittedAt(),
                report.getApprovedAt(),
                report.getEntityVersion(),

                new ReportVersionResponse(
                        version.getId(),
                        version.getVersionNumber(),
                        version.getNotes(),
                        version.getCreatedAt(),
                        version.getSubmittedAt(),
                        version.getEntityVersion(),
                        completedTasks.stream()
                                .map(this::toResponse)
                                .toList(),
                        plannedTasks.stream()
                                .map(this::toResponse)
                                .toList(),
                        blockers.stream()
                                .map(this::toResponse)
                                .toList(),
                        achievements.stream()
                                .map(this::toResponse)
                                .toList(),
                        timeEntries.stream()
                                .map(this::toResponse)
                                .toList()
                )
        );
    }

    private CompletedTaskResponse toResponse(
            CompletedTask task
    ) {

        Project project =
                task.getProject();

        return new CompletedTaskResponse(
                task.getId(),
                project == null
                        ? null
                        : project.getId(),
                project == null
                        ? null
                        : project.getName(),
                task.getTaskName(),
                task.getDescription(),
                task.getPriority(),
                task.getPlannedPercentage(),
                task.getActualPercentage(),
                task.getStatus(),
                task.getPlannedMinutes(),
                task.getSpentMinutes(),
                task.getDeliverable(),
                task.getSortOrder()
        );
    }

    private PlannedTaskResponse toResponse(
            PlannedTask task
    ) {

        Project project =
                task.getProject();

        return new PlannedTaskResponse(
                task.getId(),
                project == null
                        ? null
                        : project.getId(),
                project == null
                        ? null
                        : project.getName(),
                task.getTaskName(),
                task.getDescription(),
                task.getPriority(),
                task.getEstimatedMinutes(),
                task.getSortOrder()
        );
    }

    private BlockerResponse toResponse(
            Blocker blocker
    ) {

        return new BlockerResponse(
                blocker.getId(),
                blocker.getDescription(),
                blocker.isKeyBlocker(),
                blocker.isResolved(),
                blocker.getSortOrder()
        );
    }

    private AchievementResponse toResponse(
            Achievement achievement
    ) {

        return new AchievementResponse(
                achievement.getId(),
                achievement.getDescription(),
                achievement.isKeyAchievement(),
                achievement.getSortOrder()
        );
    }

    private TimeEntryResponse toResponse(
            TimeEntry entry
    ) {

        return new TimeEntryResponse(
                entry.getId(),
                entry.getTaskType(),
                entry.getMinutes()
        );
    }
}