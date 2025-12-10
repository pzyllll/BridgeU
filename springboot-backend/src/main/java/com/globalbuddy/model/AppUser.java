package com.globalbuddy.model;

import com.globalbuddy.model.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * User Entity
 * Implements Spring Security's UserDetails interface to support authentication
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class AppUser implements UserDetails {

    /**
     * User role enumeration
     */
    public enum Role {
        USER,       // Regular user
        ADMIN,      // Administrator
        MERCHANT    // Merchant user (requires document verification)
    }

    public enum MerchantStatus {
        PENDING,    // Waiting for verification
        APPROVED,   // Verified
        REJECTED    // Failed verification
    }

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Compatible with old field, but uses passwordHash
    @Column(name = "password")
    private String password;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "business_name", length = 255)
    private String businessName;

    @Column(name = "id_number", length = 100)
    private String idNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_status", length = 20)
    private MerchantStatus merchantStatus;

    @Column(name = "merchant_doc_type", length = 50)
    private String merchantDocType;

    @Column(name = "merchant_doc_url", length = 500)
    private String merchantDocUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private String nationality = "";

    @Column(name = "studying_in_country", nullable = false)
    private String studyingInCountry = "";

    private String institution;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> languages = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> interests = new ArrayList<>();

    @Column(nullable = false)
    private Boolean enabled = true;

    /**
     * User's preferred language for UI and content display (zh/en)
     * Defaults to 'en' if not specified
     */
    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage = "en";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // ============ UserDetails Interface Implementation ============

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ============ Factory Methods ============

    /**
     * Create new user
     */
    public static AppUser create(String username, String email, String passwordHash, String displayName) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setNationality("");
        user.setStudyingInCountry("");
        user.setLanguages(new ArrayList<>());
        user.setInterests(new ArrayList<>());
        user.setPreferredLanguage("en"); // Default to English
        return user;
    }

    public static AppUser create(String username, String email, String passwordHash, String displayName, String preferredLanguage) {
        AppUser user = create(username, email, passwordHash, displayName);
        if (preferredLanguage != null && (preferredLanguage.equals("zh") || preferredLanguage.equals("en"))) {
            user.setPreferredLanguage(preferredLanguage);
        }
        return user;
    }

    /**
     * Create merchant user with pending verification
     */
    public static AppUser createMerchant(String username,
                                         String email,
                                         String phone,
                                         String passwordHash,
                                         String displayName,
                                         String businessName,
                                         String idNumber,
                                         String preferredLanguage) {
        AppUser user = create(username, email, passwordHash, displayName, preferredLanguage);
        user.setRole(Role.MERCHANT);
        user.setPhone(phone);
        user.setBusinessName(businessName);
        user.setIdNumber(idNumber);
        user.setMerchantStatus(MerchantStatus.PENDING);
        return user;
    }

    /**
     * Create admin user
     */
    public static AppUser createAdmin(String username, String email, String passwordHash, String displayName) {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        user.setNationality("");
        user.setStudyingInCountry("");
        user.setLanguages(new ArrayList<>());
        user.setInterests(new ArrayList<>());
        return user;
    }

    /**
     * Check if user is admin
     */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

