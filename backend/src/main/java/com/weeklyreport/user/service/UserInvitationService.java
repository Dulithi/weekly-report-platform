package com.weeklyreport.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.service.ActivityLogService;
import com.weeklyreport.auth.dto.RegisteredUserResponse;
import com.weeklyreport.auth.exception.EmailAlreadyExistsException;
import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.security.SecurityProperties;
import com.weeklyreport.user.InvitationStatus;
import com.weeklyreport.user.dto.AcceptUserInvitationRequest;
import com.weeklyreport.user.dto.CreateUserInvitationRequest;
import com.weeklyreport.user.dto.CreatedUserInvitationResponse;
import com.weeklyreport.user.dto.UserInvitationResponse;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.entity.UserInvitation;
import com.weeklyreport.user.exception.InvalidInvitationException;
import com.weeklyreport.user.repository.UserInvitationRepository;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class UserInvitationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final SecurityProperties properties;
    private final Clock clock;

    public UserInvitationService(
            UserInvitationRepository invitationRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ActivityLogService activityLogService,
            SecurityProperties properties,
            Clock clock
    ) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public CreatedUserInvitationResponse create(
            CreateUserInvitationRequest request,
            UUID inviterId
    ) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException();
        }

        Instant now = clock.instant();
        invitationRepository.findPendingByEmailForUpdate(email).ifPresent(existing -> {
            if (existing.statusAt(now) == InvitationStatus.PENDING) {
                throw new ConflictException("A pending invitation already exists for this email");
            }
            existing.revoke(now);
            invitationRepository.flush();
        });

        String rawToken = generateToken();
        User inviter = userRepository.getReferenceById(inviterId);
        UserInvitation invitation = new UserInvitation(
                email,
                request.role(),
                hash(rawToken),
                inviter,
                now.plus(properties.invitation().ttl())
        );
        try {
            invitationRepository.saveAndFlush(invitation);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A pending invitation already exists for this email");
        }

        activityLogService.record(
                inviterId,
                ActivityType.USER_INVITED,
                invitation.getId(),
                Map.of("role", invitation.getRole().name())
        );
        return new CreatedUserInvitationResponse(toResponse(invitation, now), rawToken);
    }

    @Transactional(readOnly = true)
    public List<UserInvitationResponse> list(InvitationStatus status) {
        Instant now = clock.instant();
        return invitationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(invitation -> toResponse(invitation, now))
                .filter(invitation -> status == null || invitation.status() == status)
                .toList();
    }

    @Transactional
    public void revoke(UUID invitationId) {
        UserInvitation invitation = invitationRepository.findByIdForUpdate(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
        InvitationStatus status = invitation.statusAt(clock.instant());
        if (status == InvitationStatus.ACCEPTED) {
            throw new ConflictException("An accepted invitation cannot be revoked");
        }
        if (status != InvitationStatus.REVOKED) {
            invitation.revoke(clock.instant());
        }
    }

    @Transactional
    public RegisteredUserResponse accept(AcceptUserInvitationRequest request) {
        Instant now = clock.instant();
        UserInvitation invitation = invitationRepository
                .findByTokenHashForUpdate(hash(request.acceptanceToken()))
                .orElseThrow(InvalidInvitationException::new);
        if (invitation.statusAt(now) != InvitationStatus.PENDING) {
            throw new InvalidInvitationException();
        }
        if (userRepository.existsByEmailIgnoreCase(invitation.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        User user = userRepository.save(new User(
                invitation.getEmail(),
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                invitation.getRole()
        ));
        invitation.accept(now);
        activityLogService.record(
                user.getId(),
                ActivityType.INVITATION_ACCEPTED,
                invitation.getId(),
                Map.of("role", invitation.getRole().name())
        );
        return new RegisteredUserResponse(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole()
        );
    }

    private UserInvitationResponse toResponse(UserInvitation invitation, Instant now) {
        return new UserInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.statusAt(now),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getRevokedAt(),
                invitation.getCreatedAt()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
