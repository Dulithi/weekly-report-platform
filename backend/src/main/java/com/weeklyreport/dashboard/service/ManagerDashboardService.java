package com.weeklyreport.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.entity.ActivityLog;
import com.weeklyreport.activity.repository.ActivityLogRepository;
import com.weeklyreport.common.exception.BadRequestException;
import com.weeklyreport.dashboard.dto.CompletedTaskTrendPointResponse;
import com.weeklyreport.dashboard.dto.DashboardActivityActorResponse;
import com.weeklyreport.dashboard.dto.DashboardSummaryResponse;
import com.weeklyreport.dashboard.dto.ManagerDashboardResponse;
import com.weeklyreport.dashboard.dto.ProjectTaskDistributionResponse;
import com.weeklyreport.dashboard.dto.RecentReportActivityResponse;
import com.weeklyreport.dashboard.dto.TaskTypeTimeResponse;
import com.weeklyreport.dashboard.repository.CompletedTaskTrendProjection;
import com.weeklyreport.dashboard.repository.DashboardContentMetricsProjection;
import com.weeklyreport.report.SubmissionTiming;
import com.weeklyreport.report.TaskType;
import com.weeklyreport.report.dto.SubmissionTrackingResponse;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.report.service.SubmissionTrackingService;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.service.ManagerScopeService;

@Service
public class ManagerDashboardService {

    private static final int MIN_TREND_WEEKS = 1;
    private static final int MAX_TREND_WEEKS = 26;
    private static final int RECENT_ACTIVITY_LIMIT = 10;
    private static final EnumSet<ActivityType> REPORT_ACTIVITY_TYPES = EnumSet.of(
            ActivityType.REPORT_CREATED,
            ActivityType.REPORT_SUBMITTED,
            ActivityType.REPORT_RESUBMITTED,
            ActivityType.REPORT_CHANGES_REQUESTED,
            ActivityType.REPORT_APPROVED
    );

    private final SubmissionTrackingService submissionTrackingService;
    private final WeeklyReportRepository weeklyReportRepository;
    private final ActivityLogRepository activityLogRepository;
    private final Clock clock;
    private final ManagerScopeService managerScopeService;

    public ManagerDashboardService(
            SubmissionTrackingService submissionTrackingService,
            WeeklyReportRepository weeklyReportRepository,
            ActivityLogRepository activityLogRepository,
            Clock clock,
            ManagerScopeService managerScopeService
    ) {
        this.submissionTrackingService = submissionTrackingService;
        this.weeklyReportRepository = weeklyReportRepository;
        this.activityLogRepository = activityLogRepository;
        this.clock = clock;
        this.managerScopeService = managerScopeService;
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse get(
            UUID actorId, LocalDate requestedWeekStart, int trendWeeks
    ) {
        if (trendWeeks < MIN_TREND_WEEKS || trendWeeks > MAX_TREND_WEEKS) {
            throw new BadRequestException("Trend weeks must be between 1 and 26");
        }

        LocalDate weekStart = requestedWeekStart == null
                ? LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : requestedWeekStart;
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BadRequestException("Week start must be a Monday");
        }

        List<SubmissionTrackingResponse> submissions =
                submissionTrackingService.getForWeek(actorId, weekStart, null);
        List<UUID> memberIds = managerScopeService.visibleMemberIds(actorId);
        DashboardContentMetricsProjection contentMetrics =
                weeklyReportRepository.getDashboardContentMetrics(weekStart, memberIds);

        long submittedReports = submissions.stream()
                .filter(item -> item.firstSubmittedAt() != null)
                .count();
        long onTimeSubmissions = countTiming(submissions, SubmissionTiming.ON_TIME);
        long pendingSubmissions = countTiming(submissions, SubmissionTiming.PENDING);
        long lateSubmissions = countTiming(submissions, SubmissionTiming.LATE);
        long memberCount = submissions.size();

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                memberCount,
                submittedReports,
                onTimeSubmissions,
                pendingSubmissions,
                lateSubmissions,
                percentage(submittedReports, memberCount),
                percentage(onTimeSubmissions, memberCount),
                contentMetrics.getNeedsCorrectionReports(),
                contentMetrics.getOpenBlockers()
        );

        Instant dueAt = weekStart.plusDays(7).atStartOfDay(clock.getZone()).toInstant();
        return new ManagerDashboardResponse(
                weekStart,
                weekStart.plusDays(6),
                dueAt,
                summary,
                submissions,
                completedTaskTrend(weekStart, trendWeeks, memberIds),
                weeklyReportRepository.getProjectTaskDistribution(weekStart, memberIds).stream()
                        .map(item -> new ProjectTaskDistributionResponse(
                                item.getProjectId(), item.getProjectName(), item.getTaskCount()
                        ))
                        .toList(),
                weeklyReportRepository.getTimeByTaskType(weekStart, memberIds).stream()
                        .map(item -> new TaskTypeTimeResponse(
                                TaskType.valueOf(item.getTaskType()), item.getMinutes()
                        ))
                        .toList(),
                recentActivity(memberIds)
        );
    }

    private List<CompletedTaskTrendPointResponse> completedTaskTrend(
            LocalDate selectedWeek,
            int trendWeeks,
            List<UUID> memberIds
    ) {
        LocalDate firstWeek = selectedWeek.minusWeeks(trendWeeks - 1L);
        Map<LocalDate, CompletedTaskTrendProjection> values = weeklyReportRepository
                .getCompletedTaskTrend(firstWeek, selectedWeek, memberIds)
                .stream()
                .collect(Collectors.toMap(
                        CompletedTaskTrendProjection::getWeekStart,
                        Function.identity()
                ));

        return IntStream.range(0, trendWeeks)
                .mapToObj(offset -> firstWeek.plusWeeks(offset))
                .map(week -> new CompletedTaskTrendPointResponse(
                        week,
                        values.containsKey(week) ? values.get(week).getCompletedTasks() : 0
                ))
                .toList();
    }

    private List<RecentReportActivityResponse> recentActivity(List<UUID> memberIds) {
        if (memberIds.isEmpty()) return List.of();
        var reportIds = new java.util.HashSet<>(
                weeklyReportRepository.findIdsByUserIdIn(memberIds)
        );
        return activityLogRepository.findByActivityTypeInOrderByCreatedAtDesc(
                        REPORT_ACTIVITY_TYPES,
                        PageRequest.of(0, RECENT_ACTIVITY_LIMIT)
                )
                .stream()
                .filter(activity -> reportIds.contains(activity.getEntityId()))
                .map(this::toRecentActivity)
                .toList();
    }

    private RecentReportActivityResponse toRecentActivity(ActivityLog activity) {
        User actor = activity.getActor();
        DashboardActivityActorResponse actorResponse = actor == null
                ? null
                : new DashboardActivityActorResponse(
                        actor.getId(), actor.getFirstName(), actor.getLastName()
                );
        return new RecentReportActivityResponse(
                activity.getId(),
                activity.getActivityType(),
                activity.getEntityId(),
                actorResponse,
                activity.getCreatedAt()
        );
    }

    private long countTiming(
            List<SubmissionTrackingResponse> submissions,
            SubmissionTiming timing
    ) {
        return submissions.stream().filter(item -> item.timing() == timing).count();
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }
}
