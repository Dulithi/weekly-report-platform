package com.weeklyreport.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Read only the ID so no potentially stale token entity is loaded before locking.
    @Query("select token.user.id from RefreshToken token where token.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    List<RefreshToken> findByFamilyId(UUID familyId);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);
}
