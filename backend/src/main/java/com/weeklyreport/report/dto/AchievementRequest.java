package com.weeklyreport.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AchievementRequest(

        @NotBlank
        @Size(max = 4000)
        String description,

        boolean keyAchievement
) {
}