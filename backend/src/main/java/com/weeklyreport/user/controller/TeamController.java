package com.weeklyreport.user.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.user.dto.TeamMemberResponse;
import com.weeklyreport.user.dto.TeamMemberProfileResponse;
import com.weeklyreport.user.service.TeamMemberProfileService;

@RestController 
@RequestMapping("/api/v1/manager/team-members")
public class TeamController {

    private final TeamMemberProfileService teamMemberProfileService;

    public TeamController(TeamMemberProfileService teamMemberProfileService) {
        this.teamMemberProfileService = teamMemberProfileService;
    }

    @GetMapping
    public List<TeamMemberResponse> team() {
        return teamMemberProfileService.list();
    }

    @GetMapping("/{memberId}")
    public TeamMemberProfileResponse profile(@PathVariable UUID memberId) {
        return teamMemberProfileService.get(memberId);
    }
}
