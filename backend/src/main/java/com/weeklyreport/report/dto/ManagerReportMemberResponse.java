package com.weeklyreport.report.dto;

import java.util.UUID;

public record ManagerReportMemberResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active
) {
}
