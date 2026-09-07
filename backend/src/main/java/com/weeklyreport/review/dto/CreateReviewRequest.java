package com.weeklyreport.review.dto;

import com.weeklyreport.review.ReviewAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull ReviewAction action,
        @Size(max = 2000) String comment
) {
}
