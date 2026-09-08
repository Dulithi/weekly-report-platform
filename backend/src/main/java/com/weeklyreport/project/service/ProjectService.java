package com.weeklyreport.project.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.service.ActivityLogService;
import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.project.ProjectStatus;
import com.weeklyreport.project.dto.CreateProjectRequest;
import com.weeklyreport.project.dto.ProjectResponse;
import com.weeklyreport.project.dto.UpdateProjectRequest;
import com.weeklyreport.project.entity.Project;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.project.repository.ProjectMemberRepository;
import com.weeklyreport.report.repository.CompletedTaskRepository;
import com.weeklyreport.report.repository.PlannedTaskRepository;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final ProjectMemberRepository projectMemberRepository;
    private final CompletedTaskRepository completedTaskRepository;
    private final PlannedTaskRepository plannedTaskRepository;

    public ProjectService(ProjectRepository projectRepository, UserRepository userRepository,
            ActivityLogService activityLogService,
            ProjectMemberRepository projectMemberRepository,
            CompletedTaskRepository completedTaskRepository,
            PlannedTaskRepository plannedTaskRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.projectMemberRepository = projectMemberRepository;
        this.completedTaskRepository = completedTaskRepository;
        this.plannedTaskRepository = plannedTaskRepository;
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UUID requesterId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project Not Found"));

        User requester = getUser(requesterId);
        if (requester.getRole() == UserRole.TEAM_MEMBER
                && !projectMemberRepository.existsByProjectIdAndUserId(projectId, requesterId)) {
            throw new ResourceNotFoundException("Project Not Found");
        }

        return toResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID actorUserId) {

        String name = request.name().trim();

        if (projectRepository.existsByNameIgnoreCaseAndStatus(
                name,
                ProjectStatus.ACTIVE
        )) {
            throw new ConflictException(
                    "An active project with this name already exists"
            );
        }

        User creator = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                "User not found"
        )
                );

        Project project = new Project(
                name,
                normalizeOptional(request.description()),
                creator
        );

        Project saved = projectRepository.save(project);

        activityLogService.record(
                actorUserId,
                ActivityType.PROJECT_CREATED,
                saved.getId(),
                Map.of("projectName", saved.getName())
        );

        return toResponse(saved);
    }

    @Transactional
    public ProjectResponse updateProject(
            UUID projectId,
            UpdateProjectRequest request,
            UUID actorUserId
    ) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project Not Found"));

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ConflictException("Archived projects cannot be edited");
        }

        String name = request.name().trim();

        if (projectRepository.existsByNameIgnoreCaseAndStatusAndIdNot(
                name,
                ProjectStatus.ACTIVE,
                projectId
        )) {
            throw new ConflictException(
                    "An active project with this name already exists"
            );
        }

        project.update(name, normalizeOptional(request.description()));

        activityLogService.record(
                actorUserId,
                ActivityType.PROJECT_UPDATED,
                project.getId(),
                Map.of("projectName", project.getName())
        );

        return toResponse(project);
    }

    @Transactional
    public void archiveProject(
            UUID projectId,
            UUID actorUserId
    ) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project Not Found"));

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            return;
        }

        project.archive();

        activityLogService.record(
                actorUserId,
                ActivityType.PROJECT_ARCHIVED,
                project.getId(),
                Map.of(
                        "projectName",
                        project.getName()
                )
        );
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID actorUserId) {
        Project project = projectRepository.findByIdForUpdate(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project Not Found"));

        if (completedTaskRepository.existsByProjectId(projectId)
                || plannedTaskRepository.existsByProjectId(projectId)) {
            throw new ConflictException(
                    "Projects referenced by reports cannot be deleted; archive the project instead"
            );
        }

        String projectName = project.getName();
        projectMemberRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);

        activityLogService.record(
                actorUserId,
                ActivityType.PROJECT_DELETED,
                projectId,
                Map.of("projectName", projectName)
        );
    }

    @Transactional
    public ProjectResponse updateProjectStatus(
            UUID projectId,
            ProjectStatus status,
            UUID actorUserId
    ) {
        return status == ProjectStatus.ACTIVE
                ? activateProject(projectId, actorUserId)
                : archiveAndReturn(projectId, actorUserId);
    }

    private ProjectResponse archiveAndReturn(UUID projectId, UUID actorUserId) {
        archiveProject(projectId, actorUserId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project Not Found"));
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse activateProject(
            UUID projectId,
            UUID actorUserId
    ) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project Not Found"));

        if (project.getStatus() == ProjectStatus.ACTIVE) {
            return toResponse(project);
        }

        if (projectRepository.existsByNameIgnoreCaseAndStatusAndIdNot(
                project.getName(),
                ProjectStatus.ACTIVE,
                projectId
        )) {
            throw new ConflictException(
                    "Another active project with this name already exists"
            );
        }

        project.activate();

        activityLogService.record(
                actorUserId,
                ActivityType.PROJECT_ACTIVATED,
                project.getId(),
                Map.of(
                        "projectName",
                        project.getName()
                )
        );

        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(
            ProjectStatus status,
            Pageable pageable,
            UUID requesterId
    ) {
        User requester = getUser(requesterId);
        Page<Project> result = requester.getRole() == UserRole.TEAM_MEMBER
                ? projectRepository.findAssignedToUser(requesterId, status, pageable)
                : status == null
                        ? projectRepository.findAll(pageable)
                        : projectRepository.findByStatus(status, pageable);

        return result.map(this::toResponse);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String normalizeOptional(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmed
                = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private ProjectResponse toResponse(Project project) {

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedBy().getId(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );

    }

}
