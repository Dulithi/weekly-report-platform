package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.repository.ActivityLogRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.review.repository.ReportStatusHistoryRepository;
import com.weeklyreport.user.UserRole;

class WeeklyReportSubmissionIntegrationTest extends ReportIntegrationTestSupport {

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Test
    void completedDraftCanBeSubmittedAndRecordsHistoryAndActivity() throws Exception {
        var member = user("submit-success@example.com", UserRole.TEAM_MEMBER);
        String token = login(member);
        UUID reportId = createReport(token, LocalDate.of(2026, 8, 17));

        update(token, reportId, """
                {"entityVersion":0,"completedTasks":[{"taskName":"Task A","priority":"HIGH","plannedPercentage":100,"actualPercentage":100,"status":"COMPLETED"}]}
                """).andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/v1/reports/{reportId}/submit", reportId
        ).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.submittedAt").isNotEmpty())
            .andExpect(jsonPath("$.currentVersion.submittedAt").isNotEmpty());

        var histories = statusHistoryRepository.findByReportIdOrderByChangedAtAsc(reportId);
        assertThat(histories).hasSize(1);
        assertThat(histories.getFirst().getFromStatus()).isEqualTo(ReportStatus.DRAFT);
        assertThat(histories.getFirst().getToStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(activityLogRepository.findByEntityIdOrderByCreatedAtDesc(reportId,
                org.springframework.data.domain.PageRequest.of(0, 10)).getContent())
            .extracting(activity -> activity.getActivityType())
            .contains(ActivityType.REPORT_SUBMITTED);
        assertThat(weeklyReportRepository.findById(reportId).orElseThrow().getSubmittedAt()).isNotNull();
    }

    @Test
    void emptyDraftCannotBeSubmitted() throws Exception {
        var member = user("submit-empty@example.com", UserRole.TEAM_MEMBER);
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/v1/reports/{reportId}/submit", reportId
        ).header("Authorization", bearer(login(member))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void submittedReportCannotBeUpdatedOrSubmittedAgain() throws Exception {
        var member = user("submit-immutable@example.com", UserRole.TEAM_MEMBER);
        String token = login(member);
        UUID reportId = createReport(token, LocalDate.of(2026, 8, 17));
        update(token, reportId, """
                {"entityVersion":0,"completedTasks":[{"taskName":"Task A","priority":"HIGH","plannedPercentage":100,"actualPercentage":100,"status":"COMPLETED"}]}
                """).andExpect(status().isOk());
        submit(token, reportId).andExpect(status().isOk());

        update(token, reportId, """
                {"entityVersion":0,"completedTasks":[{"taskName":"Task B","priority":"HIGH","plannedPercentage":100,"actualPercentage":100,"status":"COMPLETED"}]}
                """).andExpect(status().isConflict());
        submit(token, reportId).andExpect(status().isConflict());

        getReport(token, reportId)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentVersion.completedTasks.length()").value(1))
            .andExpect(jsonPath("$.currentVersion.completedTasks[0].taskName").value("Task A"));
    }

    private org.springframework.test.web.servlet.ResultActions submit(String token, UUID reportId) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/v1/reports/{reportId}/submit", reportId
        ).header("Authorization", bearer(token)));
    }

    @Test
    void reportWithBlockerButNoCompletedTaskCanBeSubmitted() throws Exception {

        var member =user("submit-blocker-only@example.com",
                        UserRole.TEAM_MEMBER);

        String token =
                login(member);

        UUID reportId =
                createReport(
                        token,
                        LocalDate.of(2026, 8, 17)
                );

        update(
                token,
                reportId,
                """
                {
                "entityVersion": 0,
                "blockers": [
                    {
                    "description": "Blocked pending client access",
                    "keyBlocker": true,
                    "resolved": false
                    }
                ]
                }
                """
        ).andExpect(status().isOk());

        submit(
                token,
                reportId
        ).andExpect(status().isOk());
    }
}