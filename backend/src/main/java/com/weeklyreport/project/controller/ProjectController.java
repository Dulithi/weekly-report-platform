package com.weeklyreport.project.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.weeklyreport.project.ProjectStatus;
import com.weeklyreport.project.dto.ProjectMemberResponse;
import com.weeklyreport.project.dto.ProjectResponse;
import com.weeklyreport.project.service.ProjectMemberService;
import com.weeklyreport.project.service.ProjectService;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    public ProjectController(ProjectService projectService, ProjectMemberService projectMemberService) {
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
    }

    @GetMapping
    public Page<ProjectResponse> listProjects(
            @RequestParam(required = false) ProjectStatus status,
            @PageableDefault(size = 20,sort = "name") Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {

        return projectService.listProjects(
                status,
                pageable,
                UUID.fromString(jwt.getSubject())
        );
    }

    @GetMapping("/{projectId}")
    public ProjectResponse getProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        return projectService.getProject(projectId, UUID.fromString(jwt.getSubject()));
    }


    @GetMapping("/{projectId}/members")
    public List<ProjectMemberResponse> listProjectMembers(@PathVariable UUID projectId) {
        return projectMemberService.listProjectMembers(projectId);
    }
}
