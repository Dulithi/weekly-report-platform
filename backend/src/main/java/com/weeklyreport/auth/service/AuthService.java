package com.weeklyreport.auth.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.AccessTokenResponse;
import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.auth.dto.LoginResult;
import com.weeklyreport.auth.dto.RegisterRequest;
import com.weeklyreport.auth.dto.RegisteredUserResponse;
import com.weeklyreport.auth.entity.RefreshToken;
import com.weeklyreport.auth.exception.EmailAlreadyExistsException;
import com.weeklyreport.auth.exception.InvalidRefreshTokenException;
import com.weeklyreport.security.AuthenticatedUser;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AccessTokenService accessTokenService;

    private final RefreshTokenService refreshTokenService;

    private final AuthenticationManager authenticationManager;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService,
            RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public RegisteredUserResponse register(
            RegisterRequest request
    ) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash
                = passwordEncoder.encode(request.password());

        User user = new User(
                email,
                passwordHash,
                request.firstName().trim(),
                request.lastName().trim(),
                UserRole.TEAM_MEMBER
        );

        User saved = userRepository.save(user);

        return new RegisteredUserResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRole()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public LoginResult login(LoginRequest request) {

        Authentication authentication
                = authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken
                                .unauthenticated(
                                        normalizeEmail(request.email()),
                                        request.password()
                                )
                );

        AuthenticatedUser principal
                = (AuthenticatedUser) authentication.getPrincipal();

        User user = userRepository.findById(principal.id())
                .orElseThrow();

        AccessTokenResponse accessToken
                = accessTokenService.create(user);

        IssuedRefreshToken refreshToken
                = refreshTokenService.create(
                        user,
                        UUID.randomUUID()
                );

        return new LoginResult(accessToken, refreshToken);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {

        RefreshToken oldToken
                = refreshTokenService
                        .consumeForRotation(rawRefreshToken);

        User user = oldToken.getUser();

        if (!user.isActive()) {

            refreshTokenService.revokeFamily(
                    oldToken.getFamilyId()
            );

            throw new InvalidRefreshTokenException();
        }

        AccessTokenResponse accessToken
                = accessTokenService.create(user);

        IssuedRefreshToken newRefreshToken
                = refreshTokenService.create(
                        user,
                        oldToken.getFamilyId()
                );

        return new LoginResult(
                accessToken,
                newRefreshToken
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {

        refreshTokenService.revokeIfPresent(
                rawRefreshToken
        );
    }

}
