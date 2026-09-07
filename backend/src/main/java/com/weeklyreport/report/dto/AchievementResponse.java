package com.weeklyreport.report.dto;

import java.util.UUID;

public record AchievementResponse(
        UUID id,
        String description,
        boolean keyAchievement,
        int sortOrder
) {
}