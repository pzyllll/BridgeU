package com.globalbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Post response for list view with additional statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponse {
    private String id;
    private String communityId;
    private String authorId;
    private String authorName;
    private String title;
    private String body;
    private List<String> tags;
    private String category;
    private Instant createdAt;
    private Instant updatedAt;
    private String contentZh;
    private String contentEn;
    private String originalLanguage;
    private String imageUrl;
    
    // Statistics
    private long likeCount;
    private long commentCount;
    
    // Optional: semantic search score
    private Double score;
}

