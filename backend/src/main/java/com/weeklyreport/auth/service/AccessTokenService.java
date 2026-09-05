package com.weeklyreport.auth.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.weeklyreport.auth.dto.AccessTokenResponse;
import com.weeklyreport.security.SecurityProperties;
import com.weeklyreport.user.entity.User;

@Service
public class AccessTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;

    public AccessTokenService(
            JwtEncoder jwtEncoder,
            SecurityProperties properties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public AccessTokenResponse create(User user) {
        Instant now = Instant.now();

        Instant expiresAt = now.plus(
                properties.jwt().accessTokenTtl()
        );

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim(
                        "roles",
                        List.of(user.getRole().name())
                )
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        Jwt jwt = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        header,
                        claims
                )
        );

        return new AccessTokenResponse(
                jwt.getTokenValue(),
                "Bearer",
                expiresAt
        );
    }
}