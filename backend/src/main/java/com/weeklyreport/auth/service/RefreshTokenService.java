package com.weeklyreport.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.entity.RefreshToken;
import com.weeklyreport.auth.exception.InvalidRefreshTokenException;
import com.weeklyreport.auth.repository.RefreshTokenRepository;
import com.weeklyreport.security.SecurityProperties;
import com.weeklyreport.user.entity.User;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties properties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            SecurityProperties properties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    @Transactional
    public IssuedRefreshToken create(
            User user,
            UUID familyId
    ) {

        String rawToken = generateToken();
        String tokenHash = hash(rawToken);

        Instant expiresAt = Instant.now()
                .plus(properties.refreshToken().ttl());

        RefreshToken token = new RefreshToken(
                user,
                tokenHash,
                familyId,
                expiresAt
        );

        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(
                rawToken,
                expiresAt
        );
    }

    @Transactional
    public RefreshToken consumeForRotation(
            String rawToken
    ) {

        RefreshToken token = find(rawToken);

        Instant now = Instant.now();

        if (token.isRevoked()) {
            revokeFamily(token.getFamilyId());

            throw new InvalidRefreshTokenException();
        }

        if (token.isExpired(now)) {
            token.revoke(now);

            throw new InvalidRefreshTokenException();
        }

        token.revoke(now);

        return token;
    }

    @Transactional
    public void revokeIfPresent(
            String rawToken
    ) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .ifPresent(
                        token -> token.revoke(Instant.now())
                );
    }

    @Transactional
    public void revokeFamily(UUID familyId) {

        Instant now = Instant.now();

        refreshTokenRepository
                .findByFamilyId(familyId)
                .forEach(
                        token -> token.revoke(now)
                );
    }

    @Transactional(readOnly = true)
    public RefreshToken find(
            String rawToken
    ) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        return refreshTokenRepository
                .findByTokenHash(hash(rawToken))
                .orElseThrow(
                        InvalidRefreshTokenException::new
                );
    }

    private String generateToken() {

        byte[] bytes = new byte[32];

        SECURE_RANDOM.nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hash(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] result = digest.digest(
                    token.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return HexFormat
                    .of()
                    .formatHex(result);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}