package com.globalbuddy.dto;

import java.time.Instant;
import java.util.List;

public class PostResponse {
    private String id;
    private String communityId;
    private String authorId;
    private String title;
    private String body;
    private List<String> tags;
    private String category;
    private String embedding;
    private Instant createdAt;
    private Instant updatedAt;
    private String contentZh;
    private String contentEn;
    private String originalLanguage;
    private String imageUrl;

    // Constructor for backward compatibility
    public PostResponse(String id, String communityId, String authorId, String title, String body,
                        List<String> tags, String category, String embedding, Instant createdAt, Instant updatedAt) {
        this(id, communityId, authorId, title, body, tags, category, embedding, createdAt, updatedAt, null, null, null, null);
    }

    // Full constructor with translation fields
    public PostResponse(String id, String communityId, String authorId, String title, String body,
                        List<String> tags, String category, String embedding, Instant createdAt, Instant updatedAt,
                        String contentZh, String contentEn, String originalLanguage, String imageUrl) {
        this.id = id;
        this.communityId = communityId;
        this.authorId = authorId;
        this.title = title;
        this.body = body;
        this.tags = tags;
        this.category = category;
        this.embedding = embedding;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.contentZh = contentZh;
        this.contentEn = contentEn;
        this.originalLanguage = originalLanguage;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public String getCommunityId() {
        return communityId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getCategory() {
        return category;
    }

    public String getEmbedding() {
        return embedding;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getContentZh() {
        return contentZh;
    }

    public String getContentEn() {
        return contentEn;
    }

    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}

