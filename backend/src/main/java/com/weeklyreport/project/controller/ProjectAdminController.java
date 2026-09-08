package com.weeklyreport.project.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.project.dto.AssignProjectMemberRequest;
import com.weeklyreport.project.dto.CreateProjectRequest;
import com.weeklyreport.project.dto.ProjectMemberResponse;
import com.weeklyreport.project.dto.ProjectResponse;
import com.weeklyreport.project.dto.UpdateProjectRequest;
import com.weeklyreport.project.dto.UpdateProjectStatusRequest;
import com.weeklyreport.project.service.ProjectMemberService;
import com.weeklyreport.project.service.ProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/projects")
public class ProjectAdminController {

    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;

    public ProjectAdminController(ProjectService projectService, ProjectMemberService projectMemberService) {
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        ProjectResponse response = projectService.createProject(
                        request, 
                        UUID.fromString(jwt.getSubject())
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        ProjectResponse response = projectService.updateProject(
                projectId,
                request,
                UUID.fromString(jwt.getSubject())
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        projectService.deleteProject(
                projectId,
                UUID.fromString(jwt.getSubject())
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProjectStatus(
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        ProjectResponse response = projectService.updateProjectStatus(
                projectId,
                request.status(),
                UUID.fromString(jwt.getSubject())
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ProjectMemberResponse> assignProjectMember(
        @PathVariable UUID projectId,
        @Valid @RequestBody AssignProjectMemberRequest request,
        @AuthenticationPrincipal Jwt jwt
        
    ){
        ProjectMemberResponse projectMember = projectMemberService.assignProjectMember(
                                    projectId, 
                                    request, 
                                    UUID.fromString(jwt.getSubject())
                                );

        return ResponseEntity.status(HttpStatus.OK).body(projectMember);

    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<Void> removeProjectMember (
        @PathVariable UUID projectId,
        @PathVariable UUID memberId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        projectMemberService.removeProjectMember(projectId, memberId, UUID.fromString(jwt.getSubject()));

        return ResponseEntity.noContent().build();

    }

}
