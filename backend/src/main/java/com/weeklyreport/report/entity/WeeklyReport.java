package com.weeklyreport.report.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.weeklyreport.common.entity.BaseEntity;
import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
        name = "weekly_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_weekly_report_user_week",
                        columnNames = {
                                "user_id",
                                "week_start"
                        }
                )
        }
)
public class WeeklyReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "week_start",
            nullable = false
    )
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private ReportStatus status =
            ReportStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private ReportVersion currentVersion;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Version
    @Column(
            name = "entity_version",
            nullable = false
    )
    private long entityVersion;

    protected WeeklyReport() {
    }

    public WeeklyReport(
            User user,
            LocalDate weekStart
    ) {
        this.user =
                Objects.requireNonNull(user);

        this.weekStart =
                Objects.requireNonNull(weekStart);

        this.status =
                ReportStatus.DRAFT;
    }

    public User getUser() {
        return user;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public LocalDate getWeekEnd() {
        return weekStart.plusDays(6);
    }

    public ReportStatus getStatus() {
        return status;
    }

    public ReportVersion getCurrentVersion() {
        return currentVersion;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public long getEntityVersion() {
        return entityVersion;
    }

    public void setCurrentVersion(
            ReportVersion currentVersion
    ) {
        this.currentVersion =
                Objects.requireNonNull(
                        currentVersion
                );
    }

    public void submit(
            Instant submittedAt
    ) {

        if (
                status != ReportStatus.DRAFT
                        && status
                        != ReportStatus.NEEDS_CORRECTION
        ) {
            throw new IllegalStateException(
                    "Only editable reports can be submitted"
            );
        }

        if (currentVersion == null) {
            throw new IllegalStateException(
                    "Report has no current version"
            );
        }

        this.status =
                ReportStatus.SUBMITTED;

        this.submittedAt =
                Objects.requireNonNull(
                        submittedAt
                );
    }

    public void requestChanges() {

        if (
                status
                        != ReportStatus.SUBMITTED
        ) {
            throw new IllegalStateException(
                    "Only submitted reports can be sent back for correction"
            );
        }

        this.status =
                ReportStatus.NEEDS_CORRECTION;
    }

    public void approve(
            Instant approvedAt
    ) {

        if (
                status
                        != ReportStatus.SUBMITTED
        ) {
            throw new IllegalStateException(
                    "Only submitted reports can be approved"
            );
        }

        this.status =
                ReportStatus.APPROVED;

        this.approvedAt =
                Objects.requireNonNull(
                        approvedAt
                );
    }

    public boolean isEditable() {

        return status == ReportStatus.DRAFT
                || status
                == ReportStatus.NEEDS_CORRECTION;
    }
}