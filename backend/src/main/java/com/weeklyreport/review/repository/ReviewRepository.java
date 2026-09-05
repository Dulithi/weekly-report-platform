package com.weeklyreport.review.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.review.entity.Review;

public interface ReviewRepository
        extends JpaRepository<Review, UUID> {

    List<Review>
            findByReportVersionReportIdOrderByCreatedAtDesc(
                    UUID reportId
            );

    List<Review>
            findByReportVersionIdOrderByCreatedAtDesc(
                    UUID reportVersionId
            );

    Optional<Review>
            findFirstByReportVersionReportIdOrderByCreatedAtDesc(
                    UUID reportId
            );

    Optional<Review>
            findFirstByReportVersionIdOrderByCreatedAtDesc(
                    UUID reportVersionId
            );
}