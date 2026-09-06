package com.weeklyreport.user.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignManagerRequest(
        @NotNull
        UUID managerId
) {
}
