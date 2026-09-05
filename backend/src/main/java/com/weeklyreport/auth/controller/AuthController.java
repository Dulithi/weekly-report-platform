package com.weeklyreport.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weeklyreport.auth.dto.AccessTokenResponse;
import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.auth.dto.LoginResult;
import com.weeklyreport.auth.dto.RegisterRequest;
import com.weeklyreport.auth.dto.RegisteredUserResponse;
import com.weeklyreport.auth.service.AuthService;
import com.weeklyreport.auth.service.RefreshCookieService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieService cookieService;

    public AuthController(
            AuthService authService,
            RefreshCookieService cookieService
    ) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisteredUserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisteredUserResponse response =
                authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResult result =
                authService.login(request);

        ResponseCookie cookie =
                cookieService.create(
                        result.refreshToken()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                )
                .body(result.accessToken());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            HttpServletRequest request
    ) {

        String rawRefreshToken =
                cookieService.extract(request);

        LoginResult result =
                authService.refresh(
                        rawRefreshToken
                );

        ResponseCookie cookie =
                cookieService.create(
                        result.refreshToken()
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookie.toString()
                )
                .body(
                        result.accessToken()
                );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request
    ) {

        String rawRefreshToken =
                cookieService.extract(request);

        authService.logout(
                rawRefreshToken
        );

        ResponseCookie clearedCookie =
                cookieService.clear();

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearedCookie.toString()
                )
                .build();
    }
}