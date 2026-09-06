package com.weeklyreport.user.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.dto.AssignManagerRequest;
import com.weeklyreport.user.dto.UpdateRoleRequest;
import com.weeklyreport.user.dto.UserSummaryResponse;
import com.weeklyreport.user.service.UserManagementService;
import com.weeklyreport.user.service.UserService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final UserService userService;
    private final UserManagementService userManagementService;
 
    public UserAdminController(UserService userService, UserManagementService userManagementService) {
        this.userService = userService;
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public Page<UserSummaryResponse> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {

        return userService.listUsers(role, active, pageable);
    }

    @PatchMapping("/{userId}/role")
    public UserSummaryResponse changeUserRole(@PathVariable UUID userId,
        @Valid @RequestBody UpdateRoleRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.changeUserRole(userId, request, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {

        userService.activateUser(userId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();

    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable UUID userId, @AuthenticationPrincipal Jwt jwt) {

        userService.deactivateUser(userId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();

    }

    @PutMapping("/{teamMemberId}/manager")
    public ResponseEntity<Void> assignManager(
            @PathVariable UUID teamMemberId,
            @Valid @RequestBody AssignManagerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ){
        userManagementService.assignManager(teamMemberId, request, UUID.fromString(jwt.getSubject()));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teamMemberId}/manager")
    public ResponseEntity<Void> removeManager(
            @PathVariable UUID teamMemberId,
            @AuthenticationPrincipal Jwt jwt
    ){
        userManagementService.removeManager(teamMemberId, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
    
    
}
