package com.globalbuddy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Notification Entity
 * Used to send feedback to users about report results and other system events
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /**
     * Notification type
     */
    public enum Type {
        REPORT_SUCCESS,           // Report was successful, content removed/collapsed
        REPORT_FAILED,            // Report was dismissed, content is compliant
        POST_VIOLATION_PENALTY,   // Post author received penalty notification
        POST_RESTORED,            // Post was restored after false positive
        COMMENT_VIOLATION_PENALTY,// Comment author received penalty notification
        COMMENT_RESTORED          // Comment was restored after false positive
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who receives the notification
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * Notification type
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    /**
     * Notification title (bilingual)
     */
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * Notification content (bilingual)
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Related report ID (if applicable)
     */
    @Column(name = "report_id")
    private Long reportId;

    /**
     * Related post ID (if applicable)
     */
    @Column(name = "post_id", length = 36)
    private String postId;

    /**
     * Related comment ID (if applicable)
     */
    @Column(name = "comment_id", length = 36)
    private String commentId;

    /**
     * Whether the notification has been read
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * When the notification was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * When the notification was read
     */
    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

