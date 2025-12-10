package com.globalbuddy.model;

import com.globalbuddy.model.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Community Post Entity
 * Includes AI content moderation functionality
 */
@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
public class CommunityPost {

    /**
     * Post review status
     */
    public enum Status {
        PENDING_REVIEW,  // Pending review
        APPROVED,        // Approved
        REJECTED         // Rejected
    }

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /**
     * Chinese translation of the post title
     */
    @Column(name = "title_zh", length = 500)
    private String titleZh;

    /**
     * English translation of the post title
     */
    @Column(name = "title_en", length = 500)
    private String titleEn;

    /**
     * Chinese translation of the post content
     */
    @Column(name = "content_zh", columnDefinition = "TEXT")
    private String contentZh;

    /**
     * English translation of the post content
     */
    @Column(name = "content_en", columnDefinition = "TEXT")
    private String contentEn;

    /**
     * Detected original language (zh/en/th)
     */
    @Column(name = "original_language", length = 10)
    private String originalLanguage;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> tags = new ArrayList<>();

    private String category;

    // ============ AI Moderation Related Fields ============

    /**
     * Image URL (if post contains images)
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * AI moderation result
     * Stores AI-returned classification/judgment result
     */
    @Column(name = "ai_result", columnDefinition = "TEXT")
    private String aiResult;

    /**
     * AI confidence (0.0 - 1.0)
     */
    @Column(name = "ai_confidence")
    private Double aiConfidence;

    /**
     * Post review status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING_REVIEW;

    /**
     * Review note (filled by administrator)
     */
    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    /**
     * Reviewer ID
     */
    @Column(name = "reviewed_by")
    private String reviewedBy;

    /**
     * Review time
     */
    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // ============ Other Fields ============

    @Column(columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ============ Convenience Methods ============

    /**
     * Check if manual review is needed
     */
    public boolean needsManualReview() {
        return status == Status.PENDING_REVIEW;
    }

    /**
     * Approve post
     */
    public void approve(String reviewerId, String note) {
        this.status = Status.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    /**
     * Reject post
     */
    public void reject(String reviewerId, String note) {
        this.status = Status.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }
}

