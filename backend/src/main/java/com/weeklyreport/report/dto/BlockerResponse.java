package com.weeklyreport.report.dto;

import java.util.UUID;

public record BlockerResponse(
        UUID id,
        String description,
        boolean keyBlocker,
        boolean resolved,
        int sortOrder
) {
}