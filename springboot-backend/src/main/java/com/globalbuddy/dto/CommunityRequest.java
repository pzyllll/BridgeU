package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommunityRequest {
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String country;
    private String language;
    private List<String> tags = new ArrayList<>();
    private String createdBy;
}

