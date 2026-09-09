package com.weeklyreport.comparison.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.comparison.dto.MemberSectionComparisonResponse;
import com.weeklyreport.comparison.dto.WeeklySectionComparisonResponse;
import com.weeklyreport.report.content.Achievement;
import com.weeklyreport.report.content.Blocker;
import com.weeklyreport.report.dto.AchievementResponse;
import com.weeklyreport.report.dto.BlockerResponse;
import com.weeklyreport.report.dto.SubmissionTrackingResponse;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.repository.AchievementRepository;
import com.weeklyreport.report.repository.BlockerRepository;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.service.SubmissionTrackingService;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.service.ManagerScopeService;

@Service
public class ReportComparisonService {

    private final SubmissionTrackingService submissionTrackingService;
    private final ReportVersionRepository reportVersionRepository;
    private final BlockerRepository blockerRepository;
    private final AchievementRepository achievementRepository;
    private final Clock clock;
    private final ManagerScopeService managerScopeService;

    public ReportComparisonService(
            SubmissionTrackingService submissionTrackingService,
            ReportVersionRepository reportVersionRepository,
            BlockerRepository blockerRepository,
            AchievementRepository achievementRepository,
            Clock clock,
            ManagerScopeService managerScopeService
    ) {
        this.submissionTrackingService = submissionTrackingService;
        this.reportVersionRepository = reportVersionRepository;
        this.blockerRepository = blockerRepository;
        this.achievementRepository = achievementRepository;
        this.clock = clock;
        this.managerScopeService = managerScopeService;
    }

    @Transactional(readOnly = true)
    public WeeklySectionComparisonResponse get(UUID actorId, LocalDate requestedWeekStart) {
        LocalDate weekStart = requestedWeekStart == null
                ? LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : requestedWeekStart;
        List<SubmissionTrackingResponse> tracking =
                submissionTrackingService.getForWeek(actorId, weekStart, null);
        List<ReportVersion> versions = reportVersionRepository
                .findLatestSubmittedForActiveMembers(
                        weekStart, UserRole.TEAM_MEMBER,
                        managerScopeService.visibleMemberIds(actorId)
                );
        Map<UUID, ReportVersion> versionsByReportId = versions.stream()
                .collect(Collectors.toMap(version -> version.getReport().getId(), version -> version));
        List<UUID> versionIds = versions.stream().map(ReportVersion::getId).toList();

        Map<UUID, List<Blocker>> blockersByVersion = versionIds.isEmpty()
                ? Map.of()
                : blockerRepository.findForVersions(versionIds).stream()
                        .collect(Collectors.groupingBy(blocker -> blocker.getReportVersion().getId()));
        Map<UUID, List<Achievement>> achievementsByVersion = versionIds.isEmpty()
                ? Map.of()
                : achievementRepository.findForVersions(versionIds).stream()
                        .collect(Collectors.groupingBy(
                                achievement -> achievement.getReportVersion().getId()
                        ));

        List<MemberSectionComparisonResponse> members = tracking.stream()
                .map(item -> toResponse(
                        item,
                        item.reportId() == null ? null : versionsByReportId.get(item.reportId()),
                        blockersByVersion,
                        achievementsByVersion
                ))
                .toList();
        return new WeeklySectionComparisonResponse(
                weekStart,
                weekStart.plusDays(6),
                members
        );
    }

    private MemberSectionComparisonResponse toResponse(
            SubmissionTrackingResponse tracking,
            ReportVersion version,
            Map<UUID, List<Blocker>> blockersByVersion,
            Map<UUID, List<Achievement>> achievementsByVersion
    ) {
        List<BlockerResponse> blockers = version == null
                ? List.of()
                : blockersByVersion.getOrDefault(version.getId(), List.of()).stream()
                        .map(this::toResponse)
                        .toList();
        List<AchievementResponse> achievements = version == null
                ? List.of()
                : achievementsByVersion.getOrDefault(version.getId(), List.of()).stream()
                        .map(this::toResponse)
                        .toList();
        return new MemberSectionComparisonResponse(
                tracking.member(),
                tracking.reportId(),
                tracking.status(),
                tracking.timing(),
                version == null ? null : version.getVersionNumber(),
                blockers,
                achievements
        );
    }

    private BlockerResponse toResponse(Blocker blocker) {
        return new BlockerResponse(
                blocker.getId(),
                blocker.getDescription(),
                blocker.isKeyBlocker(),
                blocker.isResolved(),
                blocker.getSortOrder()
        );
    }

    private AchievementResponse toResponse(Achievement achievement) {
        return new AchievementResponse(
                achievement.getId(),
                achievement.getDescription(),
                achievement.isKeyAchievement(),
                achievement.getSortOrder()
        );
    }
}
