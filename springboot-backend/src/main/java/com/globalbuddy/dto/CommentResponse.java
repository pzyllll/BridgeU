package com.globalbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private String id;
    private String postId;
    private String authorId;
    private String authorName;
    private String authorDisplayName;
    private String content;
    private String contentZh;
    private String contentEn;
    private String originalLanguage;
    private Instant createdAt;
    private Instant updatedAt;
}

