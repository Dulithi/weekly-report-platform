package com.weeklyreport.dashboard.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.weeklyreport.dashboard.dto.ManagerDashboardResponse;
import com.weeklyreport.dashboard.service.ManagerDashboardService;

@RestController
@RequestMapping("/api/v1/manager/dashboard")
public class ManagerDashboardController {

    private final ManagerDashboardService managerDashboardService;

    public ManagerDashboardController(ManagerDashboardService managerDashboardService) {
        this.managerDashboardService = managerDashboardService;
    }

    @GetMapping
    public ManagerDashboardResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(defaultValue = "8") int trendWeeks
    ) {
        return managerDashboardService.get(
                UUID.fromString(jwt.getSubject()), weekStart, trendWeeks
        );
    }
}
