package com.weeklyreport.comparison.dto;

import java.time.LocalDate;
import java.util.List;

public record WeeklySectionComparisonResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        List<MemberSectionComparisonResponse> members
) {
}
