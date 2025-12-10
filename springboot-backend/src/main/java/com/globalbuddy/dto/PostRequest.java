package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PostRequest {
    // 社区可选，若为空则后端允许空社区
    private String communityId;
    @NotBlank
    private String authorId;
    @NotBlank
    private String title;
    @NotBlank
    private String body;
    private List<String> tags = new ArrayList<>();
    private String category;
    private String imageUrl; // optional, set after upload
}

