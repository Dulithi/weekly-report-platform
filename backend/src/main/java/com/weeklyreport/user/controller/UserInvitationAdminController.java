package com.weeklyreport.user.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.user.InvitationStatus;
import com.weeklyreport.user.dto.CreateUserInvitationRequest;
import com.weeklyreport.user.dto.CreatedUserInvitationResponse;
import com.weeklyreport.user.dto.UserInvitationResponse;
import com.weeklyreport.user.service.UserInvitationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/user-invitations")
public class UserInvitationAdminController {

    private final UserInvitationService invitationService;

    public UserInvitationAdminController(UserInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping
    public ResponseEntity<CreatedUserInvitationResponse> create(
            @Valid @RequestBody CreateUserInvitationRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var response = invitationService.create(
                request,
                UUID.fromString(jwt.getSubject())
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @GetMapping
    public List<UserInvitationResponse> list(
            @RequestParam(required = false) InvitationStatus status
    ) {
        return invitationService.list(status);
    }

    @DeleteMapping("/{invitationId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID invitationId) {
        invitationService.revoke(invitationId);
        return ResponseEntity.noContent().build();
    }
}
