package com.weeklyreport.project.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProjectMemberId implements Serializable {

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId;

    protected ProjectMemberId() {
    }

    public ProjectMemberId(UUID projectId, UUID userId) {
        this.projectId = projectId;
        this.userId = userId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProjectMemberId that)) {
            return false;
        }

        return Objects.equals(projectId, that.projectId)
                && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, userId);
    }
}