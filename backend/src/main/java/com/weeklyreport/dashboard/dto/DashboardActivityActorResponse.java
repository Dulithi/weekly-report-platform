package com.weeklyreport.dashboard.dto;

import java.util.UUID;

public record DashboardActivityActorResponse(
        UUID id,
        String firstName,
        String lastName
) {
}
