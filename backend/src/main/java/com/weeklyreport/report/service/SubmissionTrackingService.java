package com.weeklyreport.report.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.common.exception.BadRequestException;
import com.weeklyreport.report.SubmissionTiming;
import com.weeklyreport.report.SubmissionTrackingStatus;
import com.weeklyreport.report.dto.ManagerReportMemberResponse;
import com.weeklyreport.report.dto.SubmissionTrackingResponse;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.ReportFirstSubmissionProjection;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class SubmissionTrackingService {

    private final UserRepository userRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final Clock clock;

    public SubmissionTrackingService(
            UserRepository userRepository,
            WeeklyReportRepository weeklyReportRepository,
            ReportVersionRepository reportVersionRepository,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.reportVersionRepository = reportVersionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SubmissionTrackingResponse> getForWeek(
            LocalDate weekStart,
            SubmissionTrackingStatus status
    ) {
        validateMonday(weekStart);

        List<User> members = userRepository
                .findAllByRoleAndActiveTrueOrderByLastNameAscFirstNameAscEmailAsc(
                        UserRole.TEAM_MEMBER
                );
        if (members.isEmpty()) {
            return List.of();
        }

        List<UUID> memberIds = members.stream().map(User::getId).toList();
        Map<UUID, WeeklyReport> reportsByMember = weeklyReportRepository
                .findAllByUserIdInAndWeekStart(memberIds, weekStart)
                .stream()
                .collect(Collectors.toMap(report -> report.getUser().getId(), Function.identity()));

        List<UUID> reportIds = reportsByMember.values().stream()
                .map(WeeklyReport::getId)
                .toList();
        Map<UUID, Instant> firstSubmissions = reportIds.isEmpty()
                ? Map.of()
                : reportVersionRepository.findFirstSubmissions(reportIds).stream()
                        .collect(Collectors.toMap(
                                ReportFirstSubmissionProjection::getReportId,
                                ReportFirstSubmissionProjection::getFirstSubmittedAt
                        ));

        Instant dueAt = weekStart.plusDays(7).atStartOfDay(clock.getZone()).toInstant();
        Instant now = clock.instant();

        return members.stream()
                .map(member -> toResponse(
                        member,
                        reportsByMember.get(member.getId()),
                        weekStart,
                        dueAt,
                        now,
                        firstSubmissions
                ))
                .filter(item -> status == null || item.status() == status)
                .toList();
    }

    private SubmissionTrackingResponse toResponse(
            User member,
            WeeklyReport report,
            LocalDate weekStart,
            Instant dueAt,
            Instant now,
            Map<UUID, Instant> firstSubmissions
    ) {
        Instant firstSubmittedAt = report == null
                ? null
                : firstSubmissions.get(report.getId());
        SubmissionTrackingStatus status = report == null
                ? SubmissionTrackingStatus.NOT_STARTED
                : SubmissionTrackingStatus.valueOf(report.getStatus().name());
        SubmissionTiming timing = firstSubmittedAt != null
                ? (firstSubmittedAt.isBefore(dueAt)
                        ? SubmissionTiming.ON_TIME : SubmissionTiming.LATE)
                : (now.isBefore(dueAt) ? SubmissionTiming.PENDING : SubmissionTiming.LATE);

        return new SubmissionTrackingResponse(
                new ManagerReportMemberResponse(
                        member.getId(), member.getEmail(), member.getFirstName(),
                        member.getLastName(), member.isActive()
                ),
                report == null ? null : report.getId(),
                weekStart,
                weekStart.plusDays(6),
                status,
                timing,
                dueAt,
                firstSubmittedAt
        );
    }

    private void validateMonday(LocalDate weekStart) {
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new BadRequestException("Week start must be a Monday");
        }
    }
}
