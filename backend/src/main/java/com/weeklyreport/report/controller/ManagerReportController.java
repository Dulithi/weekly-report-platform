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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.SubmissionTrackingStatus;
import com.weeklyreport.report.dto.ManagerReportDetailResponse;
import com.weeklyreport.report.dto.ManagerReportFilter;
import com.weeklyreport.report.dto.ManagerReportSummaryResponse;
import com.weeklyreport.report.dto.ReportVersionResponse;
import com.weeklyreport.report.dto.ReportVersionSummaryResponse;
import com.weeklyreport.report.dto.SubmissionTrackingResponse;
import com.weeklyreport.report.service.ManagerReportService;
import com.weeklyreport.report.service.ReportVersionHistoryService;
import com.weeklyreport.report.service.SubmissionTrackingService;
import com.weeklyreport.review.dto.CreateReviewRequest;
import com.weeklyreport.review.dto.ReviewHistoryResponse;
import com.weeklyreport.review.dto.ReviewResponse;
import com.weeklyreport.review.service.ReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/manager/reports")
public class ManagerReportController {

    private final ManagerReportService managerReportService;
    private final SubmissionTrackingService submissionTrackingService;
    private final ReviewService reviewService;
    private final ReportVersionHistoryService reportVersionHistoryService;

    public ManagerReportController(
            ManagerReportService managerReportService,
            SubmissionTrackingService submissionTrackingService,
            ReviewService reviewService,
            ReportVersionHistoryService reportVersionHistoryService
    ) {
        this.managerReportService = managerReportService;
        this.submissionTrackingService = submissionTrackingService;
        this.reviewService = reviewService;
        this.reportVersionHistoryService = reportVersionHistoryService;
    }

    @GetMapping
    public Page<ManagerReportSummaryResponse> list(
            @AuthenticationPrincipal Jwt jwt,
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
                UUID.fromString(jwt.getSubject()),
                new ManagerReportFilter(memberId, projectId, from, to, status), pageable
        );
    }

    @GetMapping("/submission-tracking")
    public List<SubmissionTrackingResponse> submissionTracking(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) SubmissionTrackingStatus status
    ) {
        return submissionTrackingService.getForWeek(
                UUID.fromString(jwt.getSubject()), weekStart, status
        );
    }

    @PostMapping("/{reportId}/reviews")
    public ReviewResponse createReview(
            @PathVariable UUID reportId,
            @Valid @RequestBody CreateReviewRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal
            org.springframework.security.oauth2.jwt.Jwt jwt
    ) {
        return reviewService.createReview(
                reportId,
                UUID.fromString(jwt.getSubject()),
                request.action(),
                request.comment()
        );
    }

    @GetMapping("/{reportId}/reviews")
    public List<ReviewHistoryResponse> reviews(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reviewService.getForManager(UUID.fromString(jwt.getSubject()), reportId);
    }

    @GetMapping("/{reportId}/versions")
    public List<ReportVersionSummaryResponse> versions(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reportVersionHistoryService.listForManager(
                UUID.fromString(jwt.getSubject()), reportId
        );
    }

    @GetMapping("/{reportId}/versions/{versionNumber}")
    public ReportVersionResponse version(
            @PathVariable UUID reportId,
            @PathVariable int versionNumber,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reportVersionHistoryService.getForManager(
                UUID.fromString(jwt.getSubject()), reportId, versionNumber
        );
    }

    @GetMapping("/{reportId}")
    public ManagerReportDetailResponse get(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return managerReportService.getSubmittedDetail(
                UUID.fromString(jwt.getSubject()), reportId
        );
    }
}
