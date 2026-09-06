package com.weeklyreport.project.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.service.ActivityLogService;
import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.project.ProjectStatus;
import com.weeklyreport.project.dto.AssignProjectMemberRequest;
import com.weeklyreport.project.dto.ProjectMemberResponse;
import com.weeklyreport.project.entity.Project;
import com.weeklyreport.project.entity.ProjectMember;
import com.weeklyreport.project.entity.ProjectMemberId;
import com.weeklyreport.project.repository.ProjectMemberRepository;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;


@Service 
public class ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;


    public ProjectMemberService(ProjectRepository projectRepository, UserRepository userRepository,
            ActivityLogService activityLogService, ProjectMemberRepository projectMemberRepository) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.projectMemberRepository = projectMemberRepository;
    }

    @Transactional 
    public ProjectMemberResponse assignProjectMember(
        UUID projectId,
        AssignProjectMemberRequest request,
        UUID actorUserId
    ){
        Project project = projectRepository.findById(projectId).orElseThrow(
            ()-> new ResourceNotFoundException("Project Not Found")
        );

        if(project.getStatus() != ProjectStatus.ACTIVE) {
            throw new ConflictException("Cannot assign members to an archived project");
        }

        User member = userRepository.findById(request.userId()).orElseThrow(
            ()-> new ResourceNotFoundException("User not found")
        );

        if(!member.isActive()) {
            throw new ConflictException("Cannot assign inactive user to a project");
        }

        if(projectMemberRepository.existsByProjectIdAndUserId(projectId, request.userId())){
            throw new ConflictException("User is already assigned to this project");
        }

        ProjectMember projectMember = new ProjectMember(project, member);

        projectMemberRepository.save(projectMember);

        activityLogService.record(
                actorUserId,
                ActivityType.PROJECT_MEMBER_ASSIGNED,
                projectId,
                Map.of("userId", request.userId().toString())
        );
        

        return toMemberResponse(projectMember);
        
    }

    @Transactional
    public void removeProjectMember(UUID projectId, UUID userId, UUID actorUserId) {

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, userId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId)
                .orElseThrow(() -> new ResourceNotFoundException("Project membership not found"));

        projectMemberRepository.delete(projectMember);

        activityLogService.record(
                actorUserId,
                ActivityType.PROJECT_MEMBER_REMOVED,
                projectId,
                Map.of("userId", userId.toString())
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listProjectMembers(UUID projectId) {

        projectRepository.findById(projectId).orElseThrow(
            ()-> new ResourceNotFoundException("Project Not Found")
        );

        return projectMemberRepository
                .findByProjectId(projectId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember projectMember) {

        User user = projectMember.getUser();

        return new ProjectMemberResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole()
        );
    }

    
    
}
