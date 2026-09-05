package com.weeklyreport.activity.entity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "metadata",
            nullable = false,
            columnDefinition = "jsonb"
    )
    private Map<String, Object> metadata = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivityLog() {
    }

    public ActivityLog(
            User actor,
            ActivityType activityType,
            UUID entityId,
            Map<String, Object> metadata
    ) {
        this.actor = actor;
        this.activityType = activityType;
        this.entityId = entityId;
        this.metadata = metadata == null
                ? new HashMap<>()
                : new HashMap<>(metadata);
    }

    public UUID getId() {
        return id;
    }

    public User getActor() {
        return actor;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

}