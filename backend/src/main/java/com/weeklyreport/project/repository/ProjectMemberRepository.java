package com.weeklyreport.project.repository;

import com.weeklyreport.project.entity.ProjectMember;
import com.weeklyreport.project.entity.ProjectMemberId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectMemberRepository
        extends JpaRepository<ProjectMember, ProjectMemberId> {

    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByUserId(UUID userId);

    boolean existsByProjectIdAndUserId(
            UUID projectId,
            UUID userId
    );

    void deleteByProjectId(UUID projectId);
}
