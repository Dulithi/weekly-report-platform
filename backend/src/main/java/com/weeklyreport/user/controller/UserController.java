package com.weeklyreport.user.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.auth.dto.CurrentUserResponse;
import com.weeklyreport.user.service.UserService;

@RestController 
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(
            @AuthenticationPrincipal Jwt jwt
    ) {

        UUID userId = UUID.fromString(jwt.getSubject());

        return userService.getCurrentUser(userId);
    } 
    
}
