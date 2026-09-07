package com.weeklyreport.report.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.common.exception.BadRequestException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.report.dto.ManagerReportDetailResponse;
import com.weeklyreport.report.dto.ManagerReportFilter;
import com.weeklyreport.report.dto.ManagerReportMemberResponse;
import com.weeklyreport.report.dto.ManagerReportSummaryResponse;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.ManagerReportSpecifications;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.user.entity.User;

@Service
public class ManagerReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportVersionService reportVersionService;

    public ManagerReportService(
            WeeklyReportRepository weeklyReportRepository,
            ReportVersionRepository reportVersionRepository,
            ReportVersionService reportVersionService
    ) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.reportVersionRepository = reportVersionRepository;
        this.reportVersionService = reportVersionService;
    }

    @Transactional(readOnly = true)
    public Page<ManagerReportSummaryResponse> list(
            ManagerReportFilter filter, Pageable pageable
    ) {
        validateDateRange(filter.from(), filter.to());

        // A report overlaps "from" when its Monday is no earlier than six days before it.
        LocalDate earliestWeekStart = filter.from() == null ? null : filter.from().minusDays(6);

        return weeklyReportRepository.findAll(
                        ManagerReportSpecifications.filteredBy(
                                filter.memberId(),
                                filter.projectId(),
                                earliestWeekStart,
                                filter.to(),
                                filter.status()
                        ),
                        pageable
                )
                .map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public ManagerReportDetailResponse getSubmittedDetail(UUID reportId) {
        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Submitted report not found"));

        var submittedVersion = reportVersionRepository
                .findTopByReportIdAndSubmittedAtIsNotNullOrderByVersionNumberDesc(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Submitted report not found"));

        return new ManagerReportDetailResponse(
                report.getId(),
                toMember(report.getUser()),
                report.getWeekStart(),
                report.getWeekEnd(),
                report.getStatus(),
                report.getSubmittedAt(),
                report.getApprovedAt(),
                reportVersionService.getResponse(submittedVersion)
        );
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("From date must be on or before to date");
        }
    }

    private ManagerReportSummaryResponse toSummary(WeeklyReport report) {
        return new ManagerReportSummaryResponse(
                report.getId(),
                toMember(report.getUser()),
                report.getWeekStart(),
                report.getWeekEnd(),
                report.getStatus(),
                report.getSubmittedAt(),
                report.getApprovedAt(),
                report.getUpdatedAt()
        );
    }

    private ManagerReportMemberResponse toMember(User user) {
        return new ManagerReportMemberResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.isActive()
        );
    }
}
