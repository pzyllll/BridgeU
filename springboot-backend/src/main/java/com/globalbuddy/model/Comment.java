package com.globalbuddy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Comment Entity
 * Represents a comment on a post
 */
@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Chinese translation of the comment content
     */
    @Column(name = "content_zh", columnDefinition = "TEXT")
    private String contentZh;

    /**
     * English translation of the comment content
     */
    @Column(name = "content_en", columnDefinition = "TEXT")
    private String contentEn;

    /**
     * Detected original language (zh/en/th)
     */
    @Column(name = "original_language", length = 10)
    private String originalLanguage;

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
}

