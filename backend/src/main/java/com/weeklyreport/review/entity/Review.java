package com.weeklyreport.review.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.review.ReviewAction;
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
@Table(name = "review")
public class Review {

    @Id
    @UuidGenerator(
            style = UuidGenerator.Style.VERSION_7
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "report_version_id",
            nullable = false
    )
    private ReportVersion reportVersion;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reviewer_id",
            nullable = false
    )
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private ReviewAction action;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected Review() {
    }

    public Review(
            ReportVersion reportVersion,
            User reviewer,
            ReviewAction action,
            String comment
    ) {
        this.reportVersion =
                reportVersion;

        this.reviewer =
                reviewer;

        this.action =
                action;

        this.comment =
                comment;
    }

    public UUID getId() {
        return id;
    }

    public ReportVersion getReportVersion() {
        return reportVersion;
    }

    public User getReviewer() {
        return reviewer;
    }

    public ReviewAction getAction() {
        return action;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}