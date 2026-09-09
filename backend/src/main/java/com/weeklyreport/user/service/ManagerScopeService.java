package com.weeklyreport.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class ManagerScopeService {

    private static final Comparator<User> MEMBER_ORDER = Comparator
            .comparing(User::getLastName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(User::getFirstName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(User::getEmail, String.CASE_INSENSITIVE_ORDER);

    private final UserRepository userRepository;
    private final ManagerTeamMemberRepository managerTeamMemberRepository;

    public ManagerScopeService(
            UserRepository userRepository,
            ManagerTeamMemberRepository managerTeamMemberRepository
    ) {
        this.userRepository = userRepository;
        this.managerTeamMemberRepository = managerTeamMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<User> visibleMembers(UUID actorId) {
        User actor = actor(actorId);
        if (actor.getRole() == UserRole.ADMIN) {
            return userRepository.findAllByRoleOrderByLastNameAscFirstNameAscEmailAsc(
                    UserRole.TEAM_MEMBER
            );
        }
        return managerTeamMemberRepository.findByManagerId(actorId).stream()
                .map(assignment -> assignment.getTeamMember())
                .sorted(MEMBER_ORDER)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UUID> visibleMemberIds(UUID actorId) {
        return visibleMembers(actorId).stream().map(User::getId).toList();
    }

    @Transactional(readOnly = true)
    public void requireMember(UUID actorId, UUID memberId) {
        if (!visibleMemberIds(actorId).contains(memberId)) {
            // Do not reveal whether a member outside the caller's scope exists.
            throw new ResourceNotFoundException("Team member not found");
        }
    }

    private User actor(UUID actorId) {
        return userRepository.findById(actorId)
                .filter(User::isActive)
                .filter(user -> user.getRole() == UserRole.MANAGER
                        || user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new AccessDeniedException("Manager access required"));
    }
}
