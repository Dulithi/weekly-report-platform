package com.weeklyreport.report.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.report.content.Achievement;
import com.weeklyreport.report.content.Blocker;
import com.weeklyreport.report.content.CompletedTask;
import com.weeklyreport.report.content.PlannedTask;
import com.weeklyreport.report.content.TimeEntry;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.AchievementRepository;
import com.weeklyreport.report.repository.BlockerRepository;
import com.weeklyreport.report.repository.CompletedTaskRepository;
import com.weeklyreport.report.repository.PlannedTaskRepository;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.TimeEntryRepository;

@Service
public class ReportVersionCopyService {

    private final ReportVersionRepository reportVersionRepository;
    private final CompletedTaskRepository completedTaskRepository;
    private final PlannedTaskRepository plannedTaskRepository;
    private final BlockerRepository blockerRepository;
    private final AchievementRepository achievementRepository;
    private final TimeEntryRepository timeEntryRepository;

    public ReportVersionCopyService(
            ReportVersionRepository reportVersionRepository,
            CompletedTaskRepository completedTaskRepository,
            PlannedTaskRepository plannedTaskRepository,
            BlockerRepository blockerRepository,
            AchievementRepository achievementRepository,
            TimeEntryRepository timeEntryRepository
    ) {
        this.reportVersionRepository = reportVersionRepository;
        this.completedTaskRepository = completedTaskRepository;
        this.plannedTaskRepository = plannedTaskRepository;
        this.blockerRepository = blockerRepository;
        this.achievementRepository = achievementRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ReportVersion copyForCorrection(WeeklyReport report, ReportVersion source) {
        ReportVersion target = reportVersionRepository.saveAndFlush(
                new ReportVersion(report, source.getVersionNumber() + 1, source.getNotes())
        );
        copyCompletedTasks(source, target);
        copyPlannedTasks(source, target);
        copyBlockers(source, target);
        copyAchievements(source, target);
        copyTimeEntries(source, target);
        return target;
    }

    private void copyCompletedTasks(ReportVersion source, ReportVersion target) {
        List<CompletedTask> copies = completedTaskRepository
                .findByReportVersionIdOrderBySortOrderAsc(source.getId())
                .stream()
                .map(task -> new CompletedTask(
                        target,
                        task.getProject(),
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
                ))
                .toList();
        completedTaskRepository.saveAll(copies);
    }

    private void copyPlannedTasks(ReportVersion source, ReportVersion target) {
        List<PlannedTask> copies = plannedTaskRepository
                .findByReportVersionIdOrderBySortOrderAsc(source.getId())
                .stream()
                .map(task -> new PlannedTask(
                        target,
                        task.getProject(),
                        task.getTaskName(),
                        task.getDescription(),
                        task.getPriority(),
                        task.getEstimatedMinutes(),
                        task.getSortOrder()
                ))
                .toList();
        plannedTaskRepository.saveAll(copies);
    }

    private void copyBlockers(ReportVersion source, ReportVersion target) {
        List<Blocker> copies = blockerRepository
                .findByReportVersionIdOrderBySortOrderAsc(source.getId())
                .stream()
                .map(blocker -> new Blocker(
                        target,
                        blocker.getDescription(),
                        blocker.isKeyBlocker(),
                        blocker.isResolved(),
                        blocker.getSortOrder()
                ))
                .toList();
        blockerRepository.saveAll(copies);
    }

    private void copyAchievements(ReportVersion source, ReportVersion target) {
        List<Achievement> copies = achievementRepository
                .findByReportVersionIdOrderBySortOrderAsc(source.getId())
                .stream()
                .map(achievement -> new Achievement(
                        target,
                        achievement.getDescription(),
                        achievement.isKeyAchievement(),
                        achievement.getSortOrder()
                ))
                .toList();
        achievementRepository.saveAll(copies);
    }

    private void copyTimeEntries(ReportVersion source, ReportVersion target) {
        List<TimeEntry> copies = timeEntryRepository.findByReportVersionId(source.getId())
                .stream()
                .map(entry -> new TimeEntry(target, entry.getTaskType(), entry.getMinutes()))
                .toList();
        timeEntryRepository.saveAll(copies);
    }
}
