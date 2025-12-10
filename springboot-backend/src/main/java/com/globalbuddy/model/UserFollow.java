package com.globalbuddy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * UserFollow Entity
 * Represents a follow relationship between users (follower follows following)
 */
@Entity
@Table(name = "user_follows", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"follower_id", "following_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFollow {

    @Id
    @Column(length = 36)
    private String id;

    /**
     * The user who is following (follower)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private AppUser follower;

    /**
     * The user being followed (following)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private AppUser following;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        this.createdAt = Instant.now();
    }
}

