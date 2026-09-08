package com.weeklyreport.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.user.entity.ManagerTeamMember;

public interface ManagerTeamMemberRepository extends JpaRepository<ManagerTeamMember, UUID> {

    List<ManagerTeamMember> findAllByOrderByAssignedAtDesc();

    Optional<ManagerTeamMember> findByTeamMemberId(UUID teamMemberId);

    List<ManagerTeamMember> findByManagerId(UUID managerId);

    boolean existsByManagerIdAndTeamMemberId(
            UUID managerId,
            UUID teamMemberId
    );
}
