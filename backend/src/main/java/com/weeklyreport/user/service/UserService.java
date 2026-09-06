package com.weeklyreport.user.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.service.ActivityLogService;
import com.weeklyreport.auth.dto.CurrentUserResponse;
import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.dto.UpdateRoleRequest;
import com.weeklyreport.user.dto.UserSummaryResponse;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ManagerTeamMemberRepository managerTeamMemberRepository;
    private final ActivityLogService activityLogService;

    public UserService(UserRepository userRepository, ManagerTeamMemberRepository managerTeamMemberRepository,
            ActivityLogService activityLogService) {
        this.userRepository = userRepository;
        this.managerTeamMemberRepository = managerTeamMemberRepository;
        this.activityLogService = activityLogService;
    }

    public CurrentUserResponse getCurrentUser(
            UUID userId
    ) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Authenticated user no longer exists"
                        )
                );

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(
            UserRole role,
            Boolean active,
            Pageable pageable
    ) {

        return userRepository.findWithFilters(role, active, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public UserSummaryResponse changeUserRole(UUID userId, UpdateRoleRequest request, UUID actorUserId) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        UserRole oldRole = user.getRole();
        UserRole newRole = request.role();

        if (oldRole == UserRole.MANAGER && newRole != UserRole.MANAGER
                && !managerTeamMemberRepository.findByManagerId(userId).isEmpty()) {
            throw new ConflictException(
                    "Cannot change role while manager still has assigned team members"
            );
        }

        if (oldRole == UserRole.TEAM_MEMBER && newRole != UserRole.TEAM_MEMBER
                        && managerTeamMemberRepository
                                .findByTeamMemberId(userId)
                                .isPresent()
        ) {
            throw new ConflictException(
                    "Remove the user's manager assignment before changing their role"
            );
        }

        user.changeRole(newRole);

        activityLogService.record(
                actorUserId,
                ActivityType.USER_ROLE_CHANGED,
                userId,
                Map.of("fromRole", oldRole.name(),
                        "toRole", newRole.name()
                )
        );

        return toResponse(user);
    }

    @Transactional
    public void deactivateUser(UUID userId, UUID actorUserId) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        if (user.getId().equals(actorUserId)) {
            throw new ConflictException("Administrators cannot deactivate their own account");
        }

        if (!user.isActive()) {
            return;
        }

        if (user.getRole() == UserRole.MANAGER
                        && !managerTeamMemberRepository
                                .findByManagerId(userId)
                                .isEmpty()
        ) {
            throw new ConflictException(
                    "Cannot deactivate a manager with assigned team members"
            );
        }

        user.deactivate();

        activityLogService.record(
                actorUserId,
                ActivityType.USER_DEACTIVATED,
                userId,
                Map.of()
        );
    }

    @Transactional
    public void activateUser(UUID userId, UUID actorUserId) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not found")
        );

        if (user.isActive()) {
            return;
        }

        user.activate();

        activityLogService.record(
                actorUserId,
                ActivityType.USER_ACTIVATED,
                userId,
                Map.of()
        );
    }

    private UserSummaryResponse toResponse(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isActive()
        );
    }

}
