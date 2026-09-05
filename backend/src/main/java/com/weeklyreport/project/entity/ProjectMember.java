package com.weeklyreport.project.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.weeklyreport.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "project_member")
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id;

    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected ProjectMember() {
    }

    public ProjectMember(Project project, User user) {
        this.project = project;
        this.user = user;

        this.id = new ProjectMemberId(
                project.getId(),
                user.getId()
        );
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public ProjectMemberId getId() {
        return id;
    }
}