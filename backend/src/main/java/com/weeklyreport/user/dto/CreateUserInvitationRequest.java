package com.weeklyreport.user.dto;

import com.weeklyreport.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserInvitationRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull UserRole role
) {
}
