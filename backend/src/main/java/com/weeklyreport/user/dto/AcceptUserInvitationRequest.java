package com.weeklyreport.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptUserInvitationRequest(
        @NotBlank @Size(max = 128) String acceptanceToken,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName
) {
}
