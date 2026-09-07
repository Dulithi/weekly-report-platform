package com.weeklyreport.review.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.service.ActivityLogService;
import com.weeklyreport.common.exception.BadRequestException;
import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.common.exception.ResourceNotFoundException;
import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.dto.ManagerReportMemberResponse;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.report.service.ReportVersionCopyService;
import com.weeklyreport.review.ReviewAction;
import com.weeklyreport.review.dto.ReviewResponse;
import com.weeklyreport.review.dto.ReviewHistoryResponse;
import com.weeklyreport.review.entity.ReportStatusHistory;
import com.weeklyreport.review.entity.Review;
import com.weeklyreport.review.repository.ReportStatusHistoryRepository;
import com.weeklyreport.review.repository.ReviewRepository;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Service
public class ReviewService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final ReportStatusHistoryRepository statusHistoryRepository;
    private final ReportVersionCopyService reportVersionCopyService;
    private final ActivityLogService activityLogService;
    private final Clock clock;

    public ReviewService(
            WeeklyReportRepository weeklyReportRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            ReportStatusHistoryRepository statusHistoryRepository,
            ReportVersionCopyService reportVersionCopyService,
            ActivityLogService activityLogService,
            Clock clock
    ) {
        this.weeklyReportRepository = weeklyReportRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.reportVersionCopyService = reportVersionCopyService;
        this.activityLogService = activityLogService;
        this.clock = clock;
    }

    @Transactional
    public ReviewResponse createReview(
            UUID reportId,
            UUID reviewerId,
            ReviewAction action,
            String comment
    ) {
        return switch (action) {
            case APPROVED -> approve(reportId, reviewerId, comment);
            case CHANGES_REQUESTED -> requestChanges(reportId, reviewerId, comment);
        };
    }

    @Transactional
    public ReviewResponse approve(UUID reportId, UUID reviewerId, String comment) {
        WeeklyReport report = getSubmittedReportForReview(reportId, reviewerId);
        ReportVersion reviewedVersion = report.getCurrentVersion();
        User reviewer = userRepository.getReferenceById(reviewerId);
        Review review = new Review(
                reviewedVersion,
                reviewer,
                ReviewAction.APPROVED,
                normalizeOptional(comment)
        );

        report.approve(clock.instant());
        reviewRepository.saveAndFlush(review);
        recordStatusChange(report, reviewer, ReportStatus.APPROVED);
        activityLogService.record(
                reviewerId,
                ActivityType.REPORT_APPROVED,
                reportId,
                Map.of("versionNumber", reviewedVersion.getVersionNumber())
        );

        return toResponse(review, report, reviewedVersion, null);
    }

    @Transactional
    public ReviewResponse requestChanges(UUID reportId, UUID reviewerId, String comment) {
        WeeklyReport report = getSubmittedReportForReview(reportId, reviewerId);
        ReportVersion reviewedVersion = report.getCurrentVersion();
        User reviewer = userRepository.getReferenceById(reviewerId);
        String normalizedComment = normalizeRequired(comment);
        ReportVersion editableVersion = reportVersionCopyService
                .copyForCorrection(report, reviewedVersion);
        Review review = new Review(
                reviewedVersion,
                reviewer,
                ReviewAction.CHANGES_REQUESTED,
                normalizedComment
        );

        report.requestChanges();
        report.setCurrentVersion(editableVersion);
        reviewRepository.saveAndFlush(review);
        recordStatusChange(report, reviewer, ReportStatus.NEEDS_CORRECTION);
        activityLogService.record(
                reviewerId,
                ActivityType.REPORT_CHANGES_REQUESTED,
                reportId,
                Map.of(
                        "reviewedVersionNumber", reviewedVersion.getVersionNumber(),
                        "editableVersionNumber", editableVersion.getVersionNumber()
                )
        );

        return toResponse(review, report, reviewedVersion, editableVersion);
    }

    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> getForOwner(UUID reportId, UUID ownerId) {
        weeklyReportRepository.findByIdAndUserId(reportId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return getHistory(reportId);
    }

    @Transactional(readOnly = true)
    public List<ReviewHistoryResponse> getForManager(UUID reportId) {
        if (!weeklyReportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Report not found");
        }
        return getHistory(reportId);
    }

    private WeeklyReport getSubmittedReportForReview(UUID reportId, UUID reviewerId) {
        WeeklyReport report = weeklyReportRepository.findByIdForReview(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Submitted report not found"));
        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new ConflictException("Only submitted reports can be reviewed");
        }
        if (report.getUser().getId().equals(reviewerId)) {
            throw new ConflictException("A user cannot review their own report");
        }
        if (report.getCurrentVersion() == null || !report.getCurrentVersion().isSubmitted()) {
            throw new ConflictException("The report has no submitted version to review");
        }
        return report;
    }

    private void recordStatusChange(
            WeeklyReport report,
            User reviewer,
            ReportStatus targetStatus
    ) {
        statusHistoryRepository.save(new ReportStatusHistory(
                report,
                ReportStatus.SUBMITTED,
                targetStatus,
                reviewer
        ));
    }

    private ReviewResponse toResponse(
            Review review,
            WeeklyReport report,
            ReportVersion reviewedVersion,
            ReportVersion editableVersion
    ) {
        User reviewer = review.getReviewer();
        return new ReviewResponse(
                review.getId(),
                report.getId(),
                reviewedVersion.getId(),
                reviewedVersion.getVersionNumber(),
                review.getAction(),
                review.getComment(),
                new ManagerReportMemberResponse(
                        reviewer.getId(), reviewer.getEmail(), reviewer.getFirstName(),
                        reviewer.getLastName(), reviewer.isActive()
                ),
                review.getCreatedAt(),
                report.getStatus(),
                editableVersion == null ? null : editableVersion.getId(),
                editableVersion == null ? null : editableVersion.getVersionNumber()
        );
    }

    private List<ReviewHistoryResponse> getHistory(UUID reportId) {
        return reviewRepository.findByReportVersionReportIdOrderByCreatedAtAsc(reportId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private ReviewHistoryResponse toHistoryResponse(Review review) {
        User reviewer = review.getReviewer();
        ReportVersion version = review.getReportVersion();
        return new ReviewHistoryResponse(
                review.getId(),
                version.getId(),
                version.getVersionNumber(),
                review.getAction(),
                review.getComment(),
                new ManagerReportMemberResponse(
                        reviewer.getId(), reviewer.getEmail(), reviewer.getFirstName(),
                        reviewer.getLastName(), reviewer.isActive()
                ),
                review.getCreatedAt()
        );
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 2000) {
            throw new BadRequestException("Review comment must not exceed 2000 characters");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new BadRequestException("A correction comment is required");
        }
        return normalized;
    }
}
