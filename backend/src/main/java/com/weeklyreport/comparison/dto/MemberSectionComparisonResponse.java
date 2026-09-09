package com.weeklyreport.comparison.dto;

import java.util.List;
import java.util.UUID;

import com.weeklyreport.report.SubmissionTiming;
import com.weeklyreport.report.SubmissionTrackingStatus;
import com.weeklyreport.report.dto.AchievementResponse;
import com.weeklyreport.report.dto.BlockerResponse;
import com.weeklyreport.report.dto.ManagerReportMemberResponse;

public record MemberSectionComparisonResponse(
        ManagerReportMemberResponse member,
        UUID reportId,
        SubmissionTrackingStatus status,
        SubmissionTiming timing,
        Integer submittedVersionNumber,
        List<BlockerResponse> blockers,
        List<AchievementResponse> achievements
) {
}
