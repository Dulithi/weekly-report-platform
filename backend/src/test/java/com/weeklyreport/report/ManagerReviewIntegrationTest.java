package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.repository.ActivityLogRepository;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.review.ReviewAction;
import com.weeklyreport.review.repository.ReportStatusHistoryRepository;
import com.weeklyreport.review.repository.ReviewRepository;
import com.weeklyreport.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagerReviewIntegrationTest extends ReportIntegrationTestSupport {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReportStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Test
    void managerApprovesSubmittedVersionAndRecordsAuditData() throws Exception {
        var manager = user("approve-manager@example.com", UserRole.MANAGER);
        var member = user("approve-member@example.com", UserRole.TEAM_MEMBER);
        UUID reportId = submittedReport(member, LocalDate.of(2026, 8, 17));

        mockMvc.perform(post("/api/v1/manager/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVED\",\"comment\":\"Looks good\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.reviewedVersionNumber").value(1))
                .andExpect(jsonPath("$.action").value("APPROVED"))
                .andExpect(jsonPath("$.comment").value("Looks good"))
                .andExpect(jsonPath("$.reportStatus").value("APPROVED"))
                .andExpect(jsonPath("$.editableVersionId").doesNotExist())
                .andExpect(jsonPath("$.reviewer.id").value(manager.getId().toString()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        var report = weeklyReportRepository.findById(reportId).orElseThrow();
        var reviews = reviewRepository.findByReportVersionReportIdOrderByCreatedAtAsc(reportId);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThat(report.getApprovedAt()).isNotNull();
        assertThat(reviews).singleElement().satisfies(review -> {
            assertThat(review.getAction()).isEqualTo(ReviewAction.APPROVED);
            assertThat(review.getReportVersion().getVersionNumber()).isEqualTo(1);
        });
        assertThat(statusHistoryRepository.findFirstByReportIdOrderByChangedAtDesc(reportId)
                .orElseThrow().getToStatus()).isEqualTo(ReportStatus.APPROVED);
        assertThat(activityLogRepository.findAll()).anySatisfy(activity -> {
            assertThat(activity.getActivityType()).isEqualTo(ActivityType.REPORT_APPROVED);
            assertThat(activity.getEntityId()).isEqualTo(reportId);
        });
    }

    @Test
    void requestingChangesRequiresCommentAndCopiesAnEditableVersion() throws Exception {
        var manager = user("changes-manager@example.com", UserRole.MANAGER);
        var member = user("changes-member@example.com", UserRole.TEAM_MEMBER);
        UUID reportId = submittedReport(member, LocalDate.of(2026, 8, 24));
        String managerToken = login(manager);

        mockMvc.perform(post("/api/v1/manager/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"CHANGES_REQUESTED\",\"comment\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/manager/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"CHANGES_REQUESTED\","
                                + "\"comment\":\"Add the missing delivery link\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedVersionNumber").value(1))
                .andExpect(jsonPath("$.action").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$.reportStatus").value("NEEDS_CORRECTION"))
                .andExpect(jsonPath("$.editableVersionNumber").value(2))
                .andExpect(jsonPath("$.comment").value("Add the missing delivery link"));

        var report = weeklyReportRepository.findById(reportId).orElseThrow();
        var versions = reportVersionRepository.findByReportIdOrderByVersionNumberAsc(reportId);
        assertThat(report.getStatus()).isEqualTo(ReportStatus.NEEDS_CORRECTION);
        assertThat(report.getCurrentVersion().getVersionNumber()).isEqualTo(2);
        assertThat(versions).hasSize(2);
        assertThat(versions.get(0).isSubmitted()).isTrue();
        assertThat(versions.get(1).isSubmitted()).isFalse();
        assertThat(versions.get(1).getNotes()).isEqualTo("Original submitted notes");
        assertThat(reviewRepository.findByReportVersionIdOrderByCreatedAtAsc(versions.get(0).getId()))
                .singleElement()
                .satisfies(review -> assertThat(review.getAction())
                        .isEqualTo(ReviewAction.CHANGES_REQUESTED));

        String memberToken = login(member);
        mockMvc.perform(get("/api/v1/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportVersionNumber").value(1))
                .andExpect(jsonPath("$[0].action").value("CHANGES_REQUESTED"))
                .andExpect(jsonPath("$[0].comment").value("Add the missing delivery link"))
                .andExpect(jsonPath("$[0].reviewer.id").value(manager.getId().toString()));

        getReport(memberToken, reportId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion.versionNumber").value(2))
                .andExpect(jsonPath("$.currentVersion.completedTasks[0].taskName")
                        .value("Completed work"))
                .andExpect(jsonPath("$.currentVersion.plannedTasks[0].taskName")
                        .value("Next work"))
                .andExpect(jsonPath("$.currentVersion.blockers[0].description")
                        .value("A blocker"))
                .andExpect(jsonPath("$.currentVersion.achievements[0].description")
                        .value("A highlight"))
                .andExpect(jsonPath("$.currentVersion.timeEntries[0].minutes").value(75));

        update(memberToken, reportId,
                "{\"entityVersion\":0,\"notes\":\"Corrected notes\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentVersion.versionNumber").value(2))
                .andExpect(jsonPath("$.currentVersion.notes").value("Corrected notes"));
        assertThat(reportVersionRepository.findById(versions.get(0).getId()).orElseThrow().getNotes())
                .isEqualTo("Original submitted notes");

        mockMvc.perform(post("/api/v1/reports/{id}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.currentVersion.versionNumber").value(2))
                .andExpect(jsonPath("$.currentVersion.submittedAt").isNotEmpty());

        approve(managerToken, reportId, "Correction accepted")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedVersionNumber").value(2))
                .andExpect(jsonPath("$.reportStatus").value("APPROVED"));

        mockMvc.perform(get("/api/v1/manager/reports/{id}", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.submittedVersion.versionNumber").value(2))
                .andExpect(jsonPath("$.submittedVersion.notes").value("Corrected notes"));
        mockMvc.perform(get("/api/v1/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].reportVersionNumber").value(2))
                .andExpect(jsonPath("$[1].action").value("APPROVED"));

        var otherMember = user("changes-other-member@example.com", UserRole.TEAM_MEMBER);
        mockMvc.perform(get("/api/v1/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(otherMember))))
                .andExpect(status().isNotFound());
    }

    @Test
    void reviewOnlyAcceptsSubmittedReportsAndCannotBeRepeated() throws Exception {
        var manager = user("review-state-manager@example.com", UserRole.MANAGER);
        var member = user("review-state-member@example.com", UserRole.TEAM_MEMBER);
        UUID draftId = createReport(login(member), LocalDate.of(2026, 8, 17));
        UUID submittedId = submittedReport(member, LocalDate.of(2026, 8, 24));
        String token = login(manager);

        approve(token, draftId, null).andExpect(status().isConflict());
        approve(token, UUID.randomUUID(), null).andExpect(status().isNotFound());
        approve(token, submittedId, null).andExpect(status().isOk());
        approve(token, submittedId, null).andExpect(status().isConflict());

        assertThat(reviewRepository.findByReportVersionReportIdOrderByCreatedAtAsc(submittedId))
                .hasSize(1);
    }

    @Test
    void adminCanReviewButMemberCannot() throws Exception {
        var admin = user("review-admin@example.com", UserRole.ADMIN);
        var reviewerMember = user("review-member-reviewer@example.com", UserRole.TEAM_MEMBER);
        var author = user("review-author@example.com", UserRole.TEAM_MEMBER);
        UUID first = submittedReport(author, LocalDate.of(2026, 8, 17));
        UUID second = submittedReport(author, LocalDate.of(2026, 8, 24));

        approve(login(admin), first, null).andExpect(status().isOk());
        approve(login(reviewerMember), second, null).andExpect(status().isForbidden());
    }

    private UUID submittedReport(
            com.weeklyreport.user.entity.User member,
            LocalDate weekStart
    ) throws Exception {
        String token = login(member);
        UUID reportId = createReport(token, weekStart);
        update(token, reportId, """
                {
                  "entityVersion": 0,
                  "notes": "Original submitted notes",
                  "completedTasks": [{
                    "taskName": "Completed work",
                    "description": "Original description",
                    "priority": "HIGH",
                    "plannedPercentage": 80,
                    "actualPercentage": 100,
                    "status": "COMPLETED",
                    "plannedMinutes": 60,
                    "spentMinutes": 75,
                    "deliverable": "https://example.test/output"
                  }],
                  "plannedTasks": [{"taskName":"Next work","priority":"MEDIUM"}],
                  "blockers": [{"description":"A blocker","keyBlocker":true,"resolved":false}],
                  "achievements": [{"description":"A highlight","keyAchievement":true}],
                  "timeEntries": [{"taskType":"DEVELOPMENT","minutes":75}]
                }
                """).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/{id}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
        return reportId;
    }

    private org.springframework.test.web.servlet.ResultActions approve(
            String token,
            UUID reportId,
            String comment
    ) throws Exception {
        String body = comment == null
                ? "{\"action\":\"APPROVED\"}"
                : "{\"action\":\"APPROVED\",\"comment\":\"" + comment + "\"}";
        return mockMvc.perform(post("/api/v1/manager/reports/{id}/reviews", reportId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
