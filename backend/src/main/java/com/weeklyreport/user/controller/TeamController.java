package com.weeklyreport.user.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.user.dto.TeamMemberResponse;
import com.weeklyreport.user.service.UserManagementService;

@RestController 
@RequestMapping("/api/v1/manager/")
public class TeamController {

    private final UserManagementService userManagementService;

    public TeamController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping("/team")
    public List<TeamMemberResponse> team(
            @AuthenticationPrincipal Jwt jwt
    ) {

        return userManagementService.listTeamMembers(UUID.fromString(jwt.getSubject()));
    }
    
}
