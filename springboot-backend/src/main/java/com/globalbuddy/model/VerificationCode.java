package com.globalbuddy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 验证码实体
 * 用于存储邮箱和手机验证码
 */
@Entity
@Table(name = "verification_codes", indexes = {
    @Index(name = "idx_identifier_type", columnList = "identifier,type"),
    @Index(name = "idx_expires_at", columnList = "expiresAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    /**
     * 标识符：邮箱地址或手机号码
     */
    @Column(nullable = false, length = 255)
    private String identifier;

    /**
     * 验证码类型：EMAIL 或 SMS
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CodeType type;

    /**
     * 6位验证码
     */
    @Column(nullable = false, length = 10)
    private String code;

    /**
     * 用途：REGISTER（注册）或 RESET_PASSWORD（重置密码）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CodePurpose purpose;

    /**
     * 过期时间
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 是否已使用
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean used = false;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30); // 默认30分钟过期
        }
    }

    /**
     * 验证码类型枚举
     */
    public enum CodeType {
        EMAIL,
        SMS
    }

    /**
     * 验证码用途枚举
     */
    public enum CodePurpose {
        REGISTER,
        RESET_PASSWORD
    }

    /**
     * 检查验证码是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查验证码是否有效（未使用且未过期）
     */
    public boolean isValid() {
        return !used && !isExpired();
    }
}

