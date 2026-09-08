package com.weeklyreport.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStateRequest(
        @NotNull Boolean active
) {
}
