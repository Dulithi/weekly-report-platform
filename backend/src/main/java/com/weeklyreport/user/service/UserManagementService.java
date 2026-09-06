package com.weeklyreport.user.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.service.ActivityLogService;
import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.dto.AssignManagerRequest;
import com.weeklyreport.user.dto.TeamMemberResponse;
import com.weeklyreport.user.entity.ManagerTeamMember;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;
import com.weeklyreport.user.repository.UserRepository;

@Service 
public class UserManagementService {

    private final UserRepository userRepository;
    private final ManagerTeamMemberRepository managerTeamMemberRepository;
    private final ActivityLogService activityLogService;


    public UserManagementService(UserRepository userRepository, ManagerTeamMemberRepository managerTeamMemberRepository,
            ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.managerTeamMemberRepository = managerTeamMemberRepository;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public void assignManager(
            UUID teamMemberId,
            AssignManagerRequest request,
            UUID actorUserId
    ) {

        UUID managerId = request.managerId();

        User teamMember = userRepository.findById(teamMemberId).orElseThrow(
            ()-> new ResourceNotFoundException("User not found")
        );


        User manager =userRepository.findById(managerId).orElseThrow(
            ()-> new ResourceNotFoundException("User not found")
        );

        if (teamMember.getRole() != UserRole.TEAM_MEMBER) {
            throw new ConflictException("Only team members can be assigned to managers");
        }

        if (manager.getRole()!= UserRole.MANAGER) {
            throw new ConflictException("Selected user is not a manager");
        }

        if (!teamMember.isActive()|| !manager.isActive()) {
            throw new ConflictException("Inactive users cannot participate in manager assignments");
        }

        if (teamMemberId.equals(managerId)) {
            throw new ConflictException("A user cannot manage themselves");
        }

        managerTeamMemberRepository
                .findByTeamMemberId(teamMemberId)
                .ifPresent(managerTeamMemberRepository::delete);

        ManagerTeamMember assignment = new ManagerTeamMember(teamMember, manager);

        managerTeamMemberRepository.save(assignment);

        activityLogService.record(
                actorUserId,
                ActivityType.MANAGER_ASSIGNED,
                teamMemberId,
                Map.of(
                        "managerId",
                        managerId.toString()
                )
        );
    }

    @Transactional
    public void removeManager(UUID teamMemberId, UUID actorUserId) {

        ManagerTeamMember assignment = managerTeamMemberRepository
                        .findByTeamMemberId(teamMemberId)
                        .orElseThrow(
                            () -> new ResourceNotFoundException("Manager assignment not found"));

        managerTeamMemberRepository.delete(assignment);

        activityLogService.record(
                actorUserId,
                ActivityType.MANAGER_REMOVED,
                teamMemberId,
                Map.of()
        );
    }


    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listTeamMembers(UUID managerId) {

        return managerTeamMemberRepository.findByManagerId(managerId).stream()
            .map(member -> member.getTeamMember())
            .map(this::toTeamMemberResponse)
            .toList();

    }

    private TeamMemberResponse toTeamMemberResponse(User user) {
        return new TeamMemberResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.isActive()
        );
    }

}
