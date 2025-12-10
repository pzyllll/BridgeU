package com.globalbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDetailResponse {
    private PostResponse post;
    private String authorName;
    private String authorDisplayName;
    private String authorId;
    private long likeCount;
    private boolean isLiked;
    private long commentCount;
    private boolean isFollowing;
    private List<CommentResponse> comments;
}

