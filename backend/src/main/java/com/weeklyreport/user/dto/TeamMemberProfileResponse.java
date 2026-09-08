package com.weeklyreport.user.dto;

public record TeamMemberProfileResponse(
        TeamMemberResponse member,
        TeamMemberReportStatisticsResponse statistics
) {
}
