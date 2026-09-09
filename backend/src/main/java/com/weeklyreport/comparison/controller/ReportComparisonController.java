package com.weeklyreport.comparison.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.weeklyreport.comparison.dto.WeeklySectionComparisonResponse;
import com.weeklyreport.comparison.service.ReportComparisonService;

@RestController
@RequestMapping("/api/v1/manager/report-comparisons")
public class ReportComparisonController {

    private final ReportComparisonService reportComparisonService;

    public ReportComparisonController(ReportComparisonService reportComparisonService) {
        this.reportComparisonService = reportComparisonService;
    }

    @GetMapping
    public WeeklySectionComparisonResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        return reportComparisonService.get(UUID.fromString(jwt.getSubject()), weekStart);
    }
}
