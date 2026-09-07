package com.weeklyreport.report.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.SubmissionTrackingStatus;
import com.weeklyreport.report.dto.ManagerReportDetailResponse;
import com.weeklyreport.report.dto.ManagerReportFilter;
import com.weeklyreport.report.dto.ManagerReportSummaryResponse;
import com.weeklyreport.report.dto.SubmissionTrackingResponse;
import com.weeklyreport.report.service.ManagerReportService;
import com.weeklyreport.report.service.SubmissionTrackingService;

@RestController
@RequestMapping("/api/v1/manager/reports")
public class ManagerReportController {

    private final ManagerReportService managerReportService;
    private final SubmissionTrackingService submissionTrackingService;

    public ManagerReportController(
            ManagerReportService managerReportService,
            SubmissionTrackingService submissionTrackingService
    ) {
        this.managerReportService = managerReportService;
        this.submissionTrackingService = submissionTrackingService;
    }

    @GetMapping
    public Page<ManagerReportSummaryResponse> list(
            @RequestParam(required = false) UUID memberId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = "weekStart",
                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        return managerReportService.list(
                new ManagerReportFilter(memberId, projectId, from, to, status), pageable
        );
    }

    @GetMapping("/submission-tracking")
    public List<SubmissionTrackingResponse> submissionTracking(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) SubmissionTrackingStatus status
    ) {
        return submissionTrackingService.getForWeek(weekStart, status);
    }

    @GetMapping("/{reportId}")
    public ManagerReportDetailResponse get(@PathVariable UUID reportId) {
        return managerReportService.getSubmittedDetail(reportId);
    }
}
