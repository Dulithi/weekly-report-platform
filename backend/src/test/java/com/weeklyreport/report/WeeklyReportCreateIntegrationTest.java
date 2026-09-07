package com.weeklyreport.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.weeklyreport.user.UserRole;

class WeeklyReportCreateIntegrationTest extends ReportIntegrationTestSupport {

    @Test
    void memberCreatesDraftWithDerivedWeekEndAndFirstVersion() throws Exception {
        var member = user("create-member@example.com", UserRole.TEAM_MEMBER);

        mockMvc.perform(post("/api/v1/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(login(member)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weekStart\":\"2026-08-17\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.weekEnd").value("2026-08-23"))
            .andExpect(jsonPath("$.currentVersion.versionNumber").value(1));
    }

    @Test
    void duplicateMemberWeekReturnsConflict() throws Exception {
        var member = user("create-duplicate@example.com", UserRole.TEAM_MEMBER);
        var token = login(member);
        createReport(token, LocalDate.of(2026, 8, 17));

        mockMvc.perform(post("/api/v1/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weekStart\":\"2026-08-17\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void nonMondayWeekStartReturnsBadRequest() throws Exception {
        var member = user("create-tuesday@example.com", UserRole.TEAM_MEMBER);

        mockMvc.perform(post("/api/v1/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(login(member)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weekStart\":\"2026-08-18\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void differentMembersCanUseTheSameWeek() throws Exception {
        var first = user("create-first@example.com", UserRole.TEAM_MEMBER);
        var second = user("create-second@example.com", UserRole.TEAM_MEMBER);

        createReport(login(first), LocalDate.of(2026, 8, 17));
        createReport(login(second), LocalDate.of(2026, 8, 17));
    }

    @Test
    void managerCannotCreateReport() throws Exception {
        var manager = user("create-manager@example.com", UserRole.MANAGER);

        mockMvc.perform(post("/api/v1/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weekStart\":\"2026-08-17\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotCreateReport() throws Exception {
        var admin = user("create-admin@example.com", UserRole.ADMIN);

        mockMvc.perform(post("/api/v1/reports")
                .header(HttpHeaders.AUTHORIZATION, bearer(login(admin)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weekStart\":\"2026-08-17\"}"))
            .andExpect(status().isForbidden());
    }
}