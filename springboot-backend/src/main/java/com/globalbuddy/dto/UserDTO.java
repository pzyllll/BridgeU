package com.globalbuddy.dto;

import com.globalbuddy.model.AppUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 用户信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private String id;
    private String username;
    private String email;
    private String displayName;
    private String role;
    private String nationality;
    private String studyingInCountry;
    private String institution;
    private List<String> languages;
    private List<String> interests;
    private String preferredLanguage;
    private Instant createdAt;
    private String phone;
    private String businessName;
    private String merchantStatus;
    private String merchantDocType;
    private String idNumber;
    
    /**
     * 从实体转换为 DTO
     */
    public static UserDTO fromEntity(AppUser user) {
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .role(user.getRole().name())
            .nationality(user.getNationality())
            .studyingInCountry(user.getStudyingInCountry())
            .institution(user.getInstitution())
            .languages(user.getLanguages())
            .interests(user.getInterests())
            .preferredLanguage(user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en")
            .createdAt(user.getCreatedAt())
            .phone(user.getPhone())
            .businessName(user.getBusinessName())
            .merchantStatus(user.getMerchantStatus() != null ? user.getMerchantStatus().name() : null)
            .merchantDocType(user.getMerchantDocType())
            .idNumber(user.getIdNumber())
            .build();
    }
}
