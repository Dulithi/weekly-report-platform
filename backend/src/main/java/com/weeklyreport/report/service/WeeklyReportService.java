package com.weeklyreport.report.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.service.ActivityLogService;
import com.weeklyreport.common.exception.BadRequestException;
import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.project.ProjectStatus;
import com.weeklyreport.project.entity.Project;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.TaskType;
import com.weeklyreport.report.content.Achievement;
import com.weeklyreport.report.content.Blocker;
import com.weeklyreport.report.content.CompletedTask;
import com.weeklyreport.report.content.PlannedTask;
import com.weeklyreport.report.content.TimeEntry;
import com.weeklyreport.report.dto.AchievementRequest;
import com.weeklyreport.report.dto.BlockerRequest;
import com.weeklyreport.report.dto.CompletedTaskRequest;
import com.weeklyreport.report.dto.CreateWeeklyReportRequest;
import com.weeklyreport.report.dto.PlannedTaskRequest;
import com.weeklyreport.report.dto.TimeEntryRequest;
import com.weeklyreport.report.dto.UpdateWeeklyReportRequest;
import com.weeklyreport.report.dto.WeeklyReportResponse;
import com.weeklyreport.report.dto.WeeklyReportSummaryResponse;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.mapper.ReportMapper;
import com.weeklyreport.report.repository.AchievementRepository;
import com.weeklyreport.report.repository.BlockerRepository;
import com.weeklyreport.report.repository.CompletedTaskRepository;
import com.weeklyreport.report.repository.PlannedTaskRepository;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.TimeEntryRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.review.entity.ReportStatusHistory;
import com.weeklyreport.review.repository.ReportStatusHistoryRepository;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportVersionRepository reportVersionRepository;

    private final CompletedTaskRepository completedTaskRepository;
    private final PlannedTaskRepository plannedTaskRepository;
    private final BlockerRepository blockerRepository;
    private final AchievementRepository achievementRepository;
    private final TimeEntryRepository timeEntryRepository;

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    private final ReportStatusHistoryRepository statusHistoryRepository;
    private final ActivityLogService activityLogService;

    private final ReportMapper reportMapper;

    public WeeklyReportService(AchievementRepository achievementRepository, ActivityLogService activityLogService, BlockerRepository blockerRepository, CompletedTaskRepository completedTaskRepository, PlannedTaskRepository plannedTaskRepository, ProjectRepository projectRepository, ReportMapper reportMapper, ReportVersionRepository reportVersionRepository, ReportStatusHistoryRepository statusHistoryRepository, TimeEntryRepository timeEntryRepository, UserRepository userRepository, WeeklyReportRepository weeklyReportRepository) {
        this.achievementRepository = achievementRepository;
        this.activityLogService = activityLogService;
        this.blockerRepository = blockerRepository;
        this.completedTaskRepository = completedTaskRepository;
        this.plannedTaskRepository = plannedTaskRepository;
        this.projectRepository = projectRepository;
        this.reportMapper = reportMapper;
        this.reportVersionRepository = reportVersionRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.userRepository = userRepository;
        this.weeklyReportRepository = weeklyReportRepository;
    }

    @Transactional
    public WeeklyReportResponse create(
            UUID userId,
            CreateWeeklyReportRequest request
    ) {

        validateMonday(
                request.weekStart()
        );

        if (weeklyReportRepository
                .existsByUserIdAndWeekStart(
                        userId,
                        request.weekStart()
                )) {
            throw new ConflictException(
                    "A report already exists for this week"
            );
        }

        User user
                = userRepository
                        .findById(userId)
                        .orElseThrow(
                                ()
                                -> new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        WeeklyReport report
                = new WeeklyReport(
                        user,
                        request.weekStart()
                );

        weeklyReportRepository.save(report);

        ReportVersion version
                = new ReportVersion(
                        report,
                        1,
                        null
                );

        reportVersionRepository.save(version);

        report.setCurrentVersion(version);

        activityLogService.record(
                userId,
                ActivityType.REPORT_CREATED,
                report.getId(),
                Map.of(
                        "weekStart",
                        request.weekStart()
                                .toString()
                )
        );

        return loadResponse(report);
    }

    @Transactional
    public WeeklyReportResponse update(
            UUID reportId,
            UUID userId,
            UpdateWeeklyReportRequest request
    ) {

        WeeklyReport report
                = getOwnedReport(
                        reportId,
                        userId
                );

        if (!report.isEditable()) {
            throw new ConflictException(
                    "This report is not editable"
            );
        }

        ReportVersion version
                = report.getCurrentVersion();

        if (version.isSubmitted()) {
            throw new ConflictException(
                    "Submitted report versions cannot be modified"
            );
        }

        if (version.getEntityVersion() != request.entityVersion()
        ) {
            throw new ConflictException(
                    "The report was modified elsewhere. Reload and try again."
            );
        }

        validateContent(request);

        replaceVersionContent(
                version,
                request
        );

        version.updateNotes(
                normalizeOptional(
                        request.notes()
                )
        );

        return loadResponse(report);
    }

    @Transactional
    public WeeklyReportResponse submit(
            UUID reportId,
            UUID userId
    ) {

        WeeklyReport report
                = getOwnedReport(
                        reportId,
                        userId
                );

        if (report.getStatus()
                != ReportStatus.DRAFT
                && report.getStatus()
                != ReportStatus.NEEDS_CORRECTION) {
            throw new ConflictException(
                    "Only editable reports can be submitted"
            );
        }

        ReportVersion version = report.getCurrentVersion();

        if (version.isSubmitted()) {
            throw new ConflictException(
                    "Current version has already been submitted"
            );
        }

        validateForSubmission(version);

        Instant now = Instant.now();

        ReportStatus previousStatus
                = report.getStatus();

        version.markSubmitted(now);

        report.submit(now);

        User actor
                = userRepository
                        .getReferenceById(
                                userId
                        );

        statusHistoryRepository.save(
                new ReportStatusHistory(
                        report,
                        previousStatus,
                        ReportStatus.SUBMITTED,
                        actor
                )
        );

        ActivityType activityType
                = previousStatus
                == ReportStatus.NEEDS_CORRECTION
                        ? ActivityType.REPORT_RESUBMITTED
                        : ActivityType.REPORT_SUBMITTED;

        activityLogService.record(
                userId,
                activityType,
                report.getId(),
                Map.of(
                        "versionNumber",
                        version.getVersionNumber()
                )
        );

        return loadResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<WeeklyReportSummaryResponse> getMyReports(
            UUID userId,
            Pageable pageable
    ) {

        return weeklyReportRepository
                .findByUserId(
                        userId,
                        pageable
                )
                .map(
                        reportMapper::toSummary
                );
    }

    private void validateMonday(LocalDate weekStart) {
        if (weekStart.getDayOfWeek()
                != DayOfWeek.MONDAY) {
            throw new BadRequestException(
                    "Week start must be a Monday"
            );
        }
    }

    private String normalizeOptional(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed
                = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private Project resolveProject(
            UUID projectId
    ) {

        if (projectId == null) {
            return null;
        }

        Project project
                = projectRepository
                        .findById(projectId)
                        .orElseThrow(
                                ()
                                -> new ResourceNotFoundException(
                                        "Project not found"
                                )
                        );

        if (project.getStatus()
                != ProjectStatus.ACTIVE) {
            throw new ConflictException(
                    "Archived projects cannot be used in editable reports"
            );
        }

        return project;
    }

    private WeeklyReport getOwnedReport(
            UUID reportId,
            UUID userId
    ) {

        return weeklyReportRepository
                .findByIdAndUserId(
                        reportId,
                        userId
                )
                .orElseThrow(
                        ()
                        -> new ResourceNotFoundException(
                                "Report not found"
                        )
                );
    }

    @Transactional(readOnly = true)
    public WeeklyReportResponse getOwnedReportDetails(
            UUID reportId,
            UUID userId
    ) {

        WeeklyReport report
                = getOwnedReport(
                        reportId,
                        userId
                );

        return loadResponse(report);
    }

    private void replaceVersionContent(
            ReportVersion version,
            UpdateWeeklyReportRequest request
    ) {

        UUID versionId
                = version.getId();

        completedTaskRepository
                .deleteByReportVersionId(
                        versionId
                );

        plannedTaskRepository
                .deleteByReportVersionId(
                        versionId
                );

        blockerRepository
                .deleteByReportVersionId(
                        versionId
                );

        achievementRepository
                .deleteByReportVersionId(
                        versionId
                );

        timeEntryRepository
                .deleteByReportVersionId(
                        versionId
                );

        saveCompletedTasks(
                version,
                request.completedTasks()
        );

        savePlannedTasks(
                version,
                request.plannedTasks()
        );

        saveBlockers(
                version,
                request.blockers()
        );

        saveAchievements(
                version,
                request.achievements()
        );

        saveTimeEntries(
                version,
                request.timeEntries()
        );
    }

    private void saveCompletedTasks(
            ReportVersion version,
            List<CompletedTaskRequest> requests
    ) {

        for (int index = 0;
                index < requests.size();
                index++) {

            CompletedTaskRequest request
                    = requests.get(index);

            Project project
                    = resolveProject(
                            request.projectId()
                    );

            CompletedTask task
                    = new CompletedTask(
                            version,
                            project,
                            request.taskName().trim(),
                            normalizeOptional(
                                    request.description()
                            ),
                            request.priority(),
                            request.plannedPercentage(),
                            request.actualPercentage(),
                            request.status(),
                            request.plannedMinutes(),
                            request.spentMinutes(),
                            normalizeOptional(
                                    request.deliverable()
                            ),
                            index
                    );

            completedTaskRepository.save(task);
        }
    }

    private void savePlannedTasks(
            ReportVersion version,
            List<PlannedTaskRequest> requests
    ) {

        for (int index = 0;
                index < requests.size();
                index++) {

            PlannedTaskRequest request
                    = requests.get(index);

            Project project
                    = resolveProject(
                            request.projectId()
                    );

            PlannedTask task
                    = new PlannedTask(
                            version,
                            project,
                            request.taskName().trim(),
                            normalizeOptional(
                                    request.description()
                            ),
                            request.priority(),
                            request.estimatedMinutes(),
                            index
                    );

            plannedTaskRepository.save(task);
        }
    }

    private void saveBlockers(
            ReportVersion version,
            List<BlockerRequest> requests
    ) {

        for (int index = 0;
                index < requests.size();
                index++) {

            BlockerRequest request
                    = requests.get(index);

            blockerRepository.save(
                    new Blocker(
                            version,
                            request.description().trim(),
                            request.keyBlocker(),
                            request.resolved(),
                            index
                    )
            );
        }
    }

    private void saveAchievements(
            ReportVersion version,
            List<AchievementRequest> requests
    ) {

        for (int index = 0;
                index < requests.size();
                index++) {

            AchievementRequest request
                    = requests.get(index);

            achievementRepository.save(
                    new Achievement(
                            version,
                            request.description().trim(),
                            request.keyAchievement(),
                            index
                    )
            );
        }
    }

    private void saveTimeEntries(
            ReportVersion version,
            List<TimeEntryRequest> requests
    ) {

        for (TimeEntryRequest request : requests) {

            timeEntryRepository.save(
                    new TimeEntry(
                            version,
                            request.taskType(),
                            request.minutes()
                    )
            );
        }
    }

    private void validateContent(UpdateWeeklyReportRequest request) {

        long keyBlockers
                = request.blockers()
                        .stream()
                        .filter(blocker -> blocker.keyBlocker())
                        .count();

        if (keyBlockers > 1) {
            throw new BadRequestException(
                    "Only one blocker can be marked as the key blocker"
            );
        }

        long keyAchievements
                = request.achievements()
                        .stream()
                        .filter(
                                achievement -> achievement.keyAchievement()
                        )
                        .count();

        if (keyAchievements > 1) {
            throw new BadRequestException(
                    "Only one achievement can be marked as the key achievement"
            );
        }

        Set<TaskType> taskTypes
                = new HashSet<>();

        for (TimeEntryRequest entry
                : request.timeEntries()) {

            if (!taskTypes.add(
                    entry.taskType()
            )) {
                throw new BadRequestException(
                        "Each task type can appear only once"
                );
            }
        }
    }

    private void validateForSubmission(
            ReportVersion version
    ) {

        UUID versionId
                = version.getId();

        boolean hasCompletedTasks = !completedTaskRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        )
                        .isEmpty();

        boolean hasPlannedTasks = !plannedTaskRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        )
                        .isEmpty();

        boolean hasBlockers = !blockerRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        )
                        .isEmpty();

        boolean hasAchievements = !achievementRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        )
                        .isEmpty();

        boolean hasNotes = version.getNotes() != null && !version.getNotes().isBlank();

        if (
                !hasCompletedTasks
                && !hasPlannedTasks
                && !hasBlockers
                && !hasAchievements
                && !hasNotes
        ) {
            throw new BadRequestException(
                    "The report must contain at least some work information before submission"
            );
        }
    }

    private WeeklyReportResponse loadResponse(
            WeeklyReport report
    ) {

        ReportVersion version
                = report.getCurrentVersion();

        UUID versionId
                = version.getId();

        return reportMapper.toResponse(
                report,
                completedTaskRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        ),
                plannedTaskRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        ),
                blockerRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        ),
                achievementRepository
                        .findByReportVersionIdOrderBySortOrderAsc(
                                versionId
                        ),
                timeEntryRepository
                        .findByReportVersionId(
                                versionId
                        )
        );
    }

}
