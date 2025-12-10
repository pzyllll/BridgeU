package com.globalbuddy.dto;

import java.time.Instant;
import java.util.List;

public class CommunityResponse {
    private String id;
    private String title;
    private String description;
    private String country;
    private String language;
    private List<String> tags;
    private String createdBy;
    private Instant createdAt;

    public CommunityResponse(String id, String title, String description, String country, String language,
                             List<String> tags, String createdBy, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.country = country;
        this.language = language;
        this.tags = tags;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCountry() {
        return country;
    }

    public String getLanguage() {
        return language;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

