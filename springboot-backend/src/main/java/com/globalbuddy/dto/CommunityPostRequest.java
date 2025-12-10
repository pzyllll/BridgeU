package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityPostRequest {
    @NotBlank
    private String authorId;
    @NotBlank
    private String title;
    @NotBlank
    private String body;
    private List<String> tags = new ArrayList<>();
    private String category;
}

