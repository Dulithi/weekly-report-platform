package com.weeklyreport.user.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "manager_team_member")
public class ManagerTeamMember {

    @Id
    @Column(name = "team_member_id")
    private UUID teamMemberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_member_id")
    private User teamMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected ManagerTeamMember() {
    }

    public ManagerTeamMember(User teamMember, User manager) {
        this.teamMember = teamMember;
        this.manager = manager;
    }

    public UUID getTeamMemberId() {
        return teamMemberId;
    }

    public User getTeamMember() {
        return teamMember;
    }

    public User getManager() {
        return manager;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}