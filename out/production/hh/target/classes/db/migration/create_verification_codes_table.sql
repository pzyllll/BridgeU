-- 创建验证码表
CREATE TABLE IF NOT EXISTS verification_codes (
    id VARCHAR(36) PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL COMMENT '标识符：邮箱地址或手机号码',
    type VARCHAR(10) NOT NULL COMMENT '验证码类型：EMAIL 或 SMS',
    code VARCHAR(10) NOT NULL COMMENT '6位验证码',
    purpose VARCHAR(20) NOT NULL COMMENT '用途：REGISTER（注册）或 RESET_PASSWORD（重置密码）',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    used BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已使用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_identifier_type (identifier, type),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表';

