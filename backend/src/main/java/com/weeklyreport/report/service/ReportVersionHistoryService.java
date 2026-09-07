package com.weeklyreport.report.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.report.dto.ReportVersionResponse;
import com.weeklyreport.report.dto.ReportVersionSummaryResponse;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;

@Service
public class ReportVersionHistoryService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final ReportVersionRepository reportVersionRepository;
    private final ReportVersionService reportVersionService;

    public ReportVersionHistoryService(
            WeeklyReportRepository weeklyReportRepository,
            ReportVersionRepository reportVersionRepository,
            ReportVersionService reportVersionService
    ) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.reportVersionRepository = reportVersionRepository;
        this.reportVersionService = reportVersionService;
    }

    @Transactional(readOnly = true)
    public List<ReportVersionSummaryResponse> listForOwner(UUID reportId, UUID ownerId) {
        WeeklyReport report = getOwnedReport(reportId, ownerId);
        return reportVersionRepository.findByReportIdOrderByVersionNumberAsc(reportId)
                .stream()
                .map(version -> toSummary(version, report))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportVersionResponse getForOwner(
            UUID reportId,
            int versionNumber,
            UUID ownerId
    ) {
        getOwnedReport(reportId, ownerId);
        ReportVersion version = reportVersionRepository
                .findByReportIdAndVersionNumber(reportId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Report version not found"));
        return reportVersionService.getResponse(version);
    }

    @Transactional(readOnly = true)
    public List<ReportVersionSummaryResponse> listForManager(UUID reportId) {
        WeeklyReport report = getReport(reportId);
        return reportVersionRepository
                .findByReportIdAndSubmittedAtIsNotNullOrderByVersionNumberAsc(reportId)
                .stream()
                .map(version -> toSummary(version, report))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportVersionResponse getForManager(UUID reportId, int versionNumber) {
        getReport(reportId);
        ReportVersion version = reportVersionRepository
                .findByReportIdAndVersionNumberAndSubmittedAtIsNotNull(reportId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Submitted report version not found"));
        return reportVersionService.getResponse(version);
    }

    private WeeklyReport getOwnedReport(UUID reportId, UUID ownerId) {
        return weeklyReportRepository.findByIdAndUserId(reportId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private WeeklyReport getReport(UUID reportId) {
        return weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private ReportVersionSummaryResponse toSummary(
            ReportVersion version,
            WeeklyReport report
    ) {
        return new ReportVersionSummaryResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getCreatedAt(),
                version.getSubmittedAt(),
                version.isSubmitted(),
                report.getCurrentVersion().getId().equals(version.getId())
        );
    }
}
