package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.weeklyreport.user.UserRole;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportVersionHistoryIntegrationTest extends ReportIntegrationTestSupport {

    @Test
    void ownerSeesAllVersionsWhileManagerSeesOnlySubmittedVersions() throws Exception {
        var manager = user("versions-manager@example.com", UserRole.MANAGER);
        var owner = user("versions-owner@example.com", UserRole.TEAM_MEMBER);
        var otherMember = user("versions-other@example.com", UserRole.TEAM_MEMBER);
        assignManager(manager, owner);
        String ownerToken = login(owner);
        String managerToken = login(manager);
        UUID reportId = createReport(ownerToken, LocalDate.of(2026, 8, 17));
        update(ownerToken, reportId,
                "{\"entityVersion\":0,\"notes\":\"Version one\"}")
                .andExpect(status().isOk());
        submit(ownerToken, reportId);

        mockMvc.perform(post("/api/v1/manager/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"CHANGES_REQUESTED\","
                                + "\"comment\":\"Please revise\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/reports/{id}/versions", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].versionNumber").value(1))
                .andExpect(jsonPath("$[0].submitted").value(true))
                .andExpect(jsonPath("$[0].current").value(false))
                .andExpect(jsonPath("$[1].versionNumber").value(2))
                .andExpect(jsonPath("$[1].submitted").value(false))
                .andExpect(jsonPath("$[1].current").value(true));
        mockMvc.perform(get("/api/v1/reports/{id}/versions/{number}", reportId, 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Version one"));
        mockMvc.perform(get("/api/v1/reports/{id}/versions/{number}", reportId, 2)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Version one"));

        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].versionNumber").value(1));
        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions/{number}", reportId, 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Version one"));
        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions/{number}", reportId, 2)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());

        String otherToken = login(otherMember);
        mockMvc.perform(get("/api/v1/reports/{id}/versions", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reports/{id}/versions/{number}", reportId, 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        update(ownerToken, reportId,
                "{\"entityVersion\":0,\"notes\":\"Version two corrected\"}")
                .andExpect(status().isOk());
        submit(ownerToken, reportId);

        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].versionNumber").value(2))
                .andExpect(jsonPath("$[1].current").value(true));
        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions/{number}", reportId, 2)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Version two corrected"));
    }

    @Test
    void managerGetsEmptyHistoryForDraftAndMissingResourcesReturnNotFound() throws Exception {
        var manager = user("versions-empty-manager@example.com", UserRole.MANAGER);
        var member = user("versions-empty-member@example.com", UserRole.TEAM_MEMBER);
        assignManager(manager, member);
        String managerToken = login(manager);
        UUID draftId = createReport(login(member), LocalDate.of(2026, 8, 17));

        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions", draftId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions/{number}", draftId, 1)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/manager/reports/{id}/versions", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());
    }

    private void submit(String token, UUID reportId) throws Exception {
        mockMvc.perform(post("/api/v1/reports/{id}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }
}
