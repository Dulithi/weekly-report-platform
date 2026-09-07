package com.weeklyreport.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockerRequest(

        @NotBlank
        @Size(max = 4000)
        String description,

        boolean keyBlocker,

        boolean resolved
) {
}