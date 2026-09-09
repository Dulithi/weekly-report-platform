package com.weeklyreport.user.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.dto.TeamMemberProfileResponse;
import com.weeklyreport.user.dto.TeamMemberReportStatisticsResponse;
import com.weeklyreport.user.dto.TeamMemberResponse;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class TeamMemberProfileService {

    private final UserRepository userRepository;
    private final ManagerScopeService managerScopeService;
    private final WeeklyReportRepository weeklyReportRepository;

    public TeamMemberProfileService(
            UserRepository userRepository,
            ManagerScopeService managerScopeService,
            WeeklyReportRepository weeklyReportRepository
    ) {
        this.userRepository = userRepository;
        this.managerScopeService = managerScopeService;
        this.weeklyReportRepository = weeklyReportRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> list(UUID actorId) {
        return managerScopeService.visibleMembers(actorId).stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamMemberProfileResponse get(UUID actorId, UUID memberId) {
        managerScopeService.requireMember(actorId, memberId);
        User member = userRepository.findByIdAndRole(memberId, UserRole.TEAM_MEMBER)
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found"));
        var statistics = weeklyReportRepository.getStatisticsForUser(memberId);

        return new TeamMemberProfileResponse(
                toMemberResponse(member),
                new TeamMemberReportStatisticsResponse(
                        statistics.getTotalReports(),
                        statistics.getDraftReports(),
                        statistics.getSubmittedReports(),
                        statistics.getNeedsCorrectionReports(),
                        statistics.getApprovedReports()
                )
        );
    }

    private TeamMemberResponse toMemberResponse(User user) {
        return new TeamMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isActive()
        );
    }
}
