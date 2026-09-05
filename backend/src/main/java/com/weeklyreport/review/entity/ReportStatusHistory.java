package com.weeklyreport.review.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.entity.WeeklyReport;
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
@Table(name = "report_status_history")
public class ReportStatusHistory {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private WeeklyReport report;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ReportStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ReportStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected ReportStatusHistory() {
    }

    public ReportStatusHistory(
            WeeklyReport report,
            ReportStatus fromStatus,
            ReportStatus toStatus,
            User changedBy
    ) {
        this.report = report;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
    }

    public UUID getId() {
        return id;
    }

    public WeeklyReport getReport() {
        return report;
    }

    public ReportStatus getFromStatus() {
        return fromStatus;
    }

    public ReportStatus getToStatus() {
        return toStatus;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}