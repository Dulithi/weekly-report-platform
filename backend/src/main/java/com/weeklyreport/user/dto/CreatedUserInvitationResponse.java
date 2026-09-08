package com.weeklyreport.user.dto;

public record CreatedUserInvitationResponse(
        UserInvitationResponse invitation,
        String acceptanceToken
) {
}
