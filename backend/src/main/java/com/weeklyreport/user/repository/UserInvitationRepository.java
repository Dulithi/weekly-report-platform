package com.weeklyreport.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.user.entity.UserInvitation;

import jakarta.persistence.LockModeType;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from UserInvitation invitation "
            + "where lower(invitation.email) = lower(:email) "
            + "and invitation.acceptedAt is null and invitation.revokedAt is null")
    Optional<UserInvitation> findPendingByEmailForUpdate(@Param("email") String email);

    List<UserInvitation> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from UserInvitation invitation "
            + "where invitation.tokenHash = :tokenHash")
    Optional<UserInvitation> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invitation from UserInvitation invitation where invitation.id = :id")
    Optional<UserInvitation> findByIdForUpdate(@Param("id") UUID id);
}
