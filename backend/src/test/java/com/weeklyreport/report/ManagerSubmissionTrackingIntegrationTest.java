package com.weeklyreport.report;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.user.UserRole;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;

class ManagerSubmissionTrackingIntegrationTest extends ReportIntegrationTestSupport {

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Test
    void selectedWeekIncludesEveryActiveMemberAndSeparatesWorkflowFromTiming() throws Exception {
        var manager = user("tracking-manager@example.com", UserRole.MANAGER);
        var submittedMember = user("tracking-submitted@example.com", UserRole.TEAM_MEMBER);
        var draftMember = user("tracking-draft@example.com", UserRole.TEAM_MEMBER);
        var missingMember = user("tracking-missing@example.com", UserRole.TEAM_MEMBER);
        var inactiveMember = user("tracking-inactive@example.com", UserRole.TEAM_MEMBER);
        inactiveMember.deactivate();
        userRepository.saveAndFlush(inactiveMember);
        LocalDate week = LocalDate.of(2026, 8, 17);

        var submittedReport = createReport(login(submittedMember), week);
        update(login(submittedMember), submittedReport,
                "{\"entityVersion\":0,\"notes\":\"Ready for review\"}")
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/{id}/submit", submittedReport)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(submittedMember))))
                .andExpect(status().isOk());
        createReport(login(draftMember), week);

        mockMvc.perform(get("/api/v1/manager/reports/submission-tracking")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", week.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.member.email == '%s')].status"
                        .formatted(submittedMember.getEmail())).value(hasItem("SUBMITTED")))
                .andExpect(jsonPath("$[?(@.member.email == '%s')].timing"
                        .formatted(submittedMember.getEmail())).value(hasItem("LATE")))
                .andExpect(jsonPath("$[?(@.member.email == '%s')].status"
                        .formatted(draftMember.getEmail())).value(hasItem("DRAFT")))
                .andExpect(jsonPath("$[?(@.member.email == '%s')].timing"
                        .formatted(draftMember.getEmail())).value(hasItem("LATE")))
                .andExpect(jsonPath("$[?(@.member.email == '%s')].status"
                        .formatted(missingMember.getEmail())).value(hasItem("NOT_STARTED")))
                .andExpect(jsonPath("$[?(@.member.email == '%s')].reportId"
                        .formatted(missingMember.getEmail())).value(hasItem(nullValue())))
                .andExpect(jsonPath("$[?(@.member.email == '%s')]"
                        .formatted(inactiveMember.getEmail())).isEmpty())
                .andExpect(jsonPath("$[?(@.member.email == '%s')]"
                        .formatted(manager.getEmail())).isEmpty());
    }

    @Test
    void futureUnsubmittedWorkIsPendingAndStatusCanBeFiltered() throws Exception {
        var manager = user("tracking-filter-manager@example.com", UserRole.MANAGER);
        var draftMember = user("tracking-filter-draft@example.com", UserRole.TEAM_MEMBER);
        var missingMember = user("tracking-filter-missing@example.com", UserRole.TEAM_MEMBER);
        LocalDate futureWeek = LocalDate.of(2099, 1, 5);
        createReport(login(draftMember), futureWeek);

        mockMvc.perform(get("/api/v1/manager/reports/submission-tracking")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", futureWeek.toString())
                        .param("status", "NOT_STARTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].member.id").value(missingMember.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$[0].timing").value("PENDING"))
                .andExpect(jsonPath("$[0].dueAt").isNotEmpty());
    }

    @Test
    void timingUsesFirstSubmissionRatherThanLaterCorrectionSubmission() throws Exception {
        var manager = user("tracking-first-manager@example.com", UserRole.MANAGER);
        var member = user("tracking-first-member@example.com", UserRole.TEAM_MEMBER);
        LocalDate week = LocalDate.of(2026, 8, 17);
        Instant dueAt = Instant.parse("2026-08-23T18:30:00Z");

        WeeklyReport report = weeklyReportRepository.saveAndFlush(new WeeklyReport(member, week));
        ReportVersion first = new ReportVersion(report, 1, null);
        first.markSubmitted(dueAt.minusSeconds(1));
        reportVersionRepository.saveAndFlush(first);
        report.setCurrentVersion(first);
        report.submit(dueAt.minusSeconds(1));
        weeklyReportRepository.saveAndFlush(report);

        report.requestChanges();
        ReportVersion correction = new ReportVersion(report, 2, null);
        correction.markSubmitted(dueAt.plusSeconds(60));
        reportVersionRepository.saveAndFlush(correction);
        report.setCurrentVersion(correction);
        report.submit(dueAt.plusSeconds(60));
        weeklyReportRepository.saveAndFlush(report);

        mockMvc.perform(get("/api/v1/manager/reports/submission-tracking")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", week.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstSubmittedAt")
                        .value(dueAt.minusSeconds(1).toString()))
                .andExpect(jsonPath("$[0].timing").value("ON_TIME"));
    }

    @Test
    void trackingRequiresMondayAndManagerAuthority() throws Exception {
        var manager = user("tracking-validation-manager@example.com", UserRole.MANAGER);
        var member = user("tracking-validation-member@example.com", UserRole.TEAM_MEMBER);

        mockMvc.perform(get("/api/v1/manager/reports/submission-tracking")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", "2026-08-18"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/manager/reports/submission-tracking")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(member)))
                        .param("weekStart", "2026-08-17"))
                .andExpect(status().isForbidden());
    }
}
