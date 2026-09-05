package com.weeklyreport.auth.service;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.weeklyreport.security.SecurityProperties;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class RefreshCookieService {

    private final SecurityProperties properties;

    public RefreshCookieService(
            SecurityProperties properties
    ) {
        this.properties = properties;
    }

    public ResponseCookie create(
            IssuedRefreshToken token
    ) {
        return ResponseCookie
                .from(
                        properties.refreshToken().cookieName(),
                        token.rawToken()
                )
                .httpOnly(true)
                .secure(properties.refreshToken().secure())
                .sameSite(
                        properties.refreshToken().sameSite()
                )
                .path("/api/v1/auth")
                .maxAge(properties.refreshToken().ttl())
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie
                .from(
                        properties.refreshToken().cookieName(),
                        ""
                )
                .httpOnly(true)
                .secure(properties.refreshToken().secure())
                .sameSite(
                        properties.refreshToken().sameSite()
                )
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }

    public String extract(
            HttpServletRequest request
    ) {

        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {

            if (
                    properties
                            .refreshToken()
                            .cookieName()
                            .equals(cookie.getName())
            ) {

                return cookie.getValue();
            }
        }

        return null;
    }   
}
