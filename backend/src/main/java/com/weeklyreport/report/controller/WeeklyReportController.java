package com.weeklyreport.report.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.report.dto.CreateWeeklyReportRequest;
import com.weeklyreport.report.dto.UpdateWeeklyReportRequest;
import com.weeklyreport.report.dto.WeeklyReportResponse;
import com.weeklyreport.report.dto.WeeklyReportSummaryResponse;
import com.weeklyreport.report.service.WeeklyReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reports")
public class WeeklyReportController {

    private final WeeklyReportService reportService;

    public WeeklyReportController(
            WeeklyReportService reportService
    ) {
        this.reportService =
                reportService;
    }

    @PostMapping
    public ResponseEntity<WeeklyReportResponse> create(
            @Valid
            @RequestBody
            CreateWeeklyReportRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {

        WeeklyReportResponse response =
                reportService.create(
                        currentUserId(jwt),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{reportId}")
    public WeeklyReportResponse update(
            @PathVariable UUID reportId,

            @Valid
            @RequestBody
            UpdateWeeklyReportRequest request,

            @AuthenticationPrincipal
            Jwt jwt
    ) {

        return reportService.update(
                reportId,
                currentUserId(jwt),
                request
        );
    }

    @PostMapping("/{reportId}/submit")
    public WeeklyReportResponse submit(
            @PathVariable UUID reportId,

            @AuthenticationPrincipal
            Jwt jwt
    ) {

        return reportService.submit(
                reportId,
                currentUserId(jwt)
        );
    }

    @GetMapping("/{reportId}")
    public WeeklyReportResponse get(
            @PathVariable UUID reportId,

            @AuthenticationPrincipal
            Jwt jwt
    ) {

        return reportService
                .getOwnedReportDetails(
                        reportId,
                        currentUserId(jwt)
                );
    }

    @GetMapping("/me")
    public Page<WeeklyReportSummaryResponse> history(
            @PageableDefault(
                    size = 20,
                    sort = "weekStart",
                    direction =
                            org.springframework.data
                                    .domain.Sort
                                    .Direction.DESC
            )
            Pageable pageable,

            @AuthenticationPrincipal
            Jwt jwt
    ) {

        return reportService.getMyReports(
                currentUserId(jwt),
                pageable
        );
    }

    private UUID currentUserId(
            Jwt jwt
    ) {

        return UUID.fromString(
                jwt.getSubject()
        );
    }
}