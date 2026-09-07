package com.weeklyreport.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.entity.RefreshToken;
import com.weeklyreport.auth.exception.InvalidRefreshTokenException;
import com.weeklyreport.auth.repository.RefreshTokenRepository;
import com.weeklyreport.security.SecurityProperties;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecurityProperties properties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            SecurityProperties properties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
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

    // This method joins AuthService.refresh's transaction; preserve revocations here too.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshToken consumeForRotation(
            String rawToken
    ) {

        RefreshToken token = findForMutation(rawToken)
                .orElseThrow(InvalidRefreshTokenException::new);

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

        // Logout also revokes a successor created by a refresh that just finished.
        findForMutation(rawToken).ifPresent(token -> revokeFamily(token.getFamilyId()));
    }

    private Optional<RefreshToken> findForMutation(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = hash(rawToken);
        Optional<UUID> userId = refreshTokenRepository.findUserIdByTokenHash(tokenHash);
        if (userId.isEmpty()) {
            return Optional.empty();
        }

        // A stable account row serializes refresh/logout, including older-token replays.
        // The caller's transaction holds this lock through revocation and replacement.
        if (userRepository.findByIdForUpdate(userId.get()).isEmpty()) {
            return Optional.empty();
        }

        return refreshTokenRepository.findByTokenHash(tokenHash);
    }

    @Transactional
    public void revokeFamily(UUID familyId) {

        // Refresh/logout callers acquire the account lock before reaching this method.

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
