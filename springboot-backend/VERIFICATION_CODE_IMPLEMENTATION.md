# 验证码功能实现说明

## 📋 功能概述

已实现完整的邮箱/手机验证码注册和密码重置功能，包括：

1. **发送验证码** - 支持邮箱和手机号
2. **验证验证码** - 验证码验证
3. **注册** - 带验证码的注册流程
4. **忘记密码** - 发送重置密码验证码
5. **重置密码** - 使用验证码重置密码

## 🗄️ 数据库表

### verification_codes 表

表会在应用启动时自动创建（JPA自动建表），或手动执行SQL脚本：

```sql
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
```

## 🔌 API 端点

### 1. 发送验证码（注册）

**POST** `/api/auth/send-verification-code`

**请求体：**
```json
{
  "identifier": "user@example.com",  // 邮箱或手机号
  "type": "email"  // "email" 或 "phone"
}
```

**响应：**
```json
{
  "success": true,
  "message": "验证码已发送"
}
```

### 2. 验证验证码

**POST** `/api/auth/verify-code`

**请求体：**
```json
{
  "identifier": "user@example.com",
  "code": "123456",
  "type": "email",
  "purpose": "REGISTER"  // 可选，默认为 "REGISTER"
}
```

**响应：**
```json
{
  "success": true,
  "message": "验证码验证成功"
}
```

### 3. 注册（带验证码）

**POST** `/api/auth/register`

**请求体：**
```json
{
  "identifier": "user@example.com",
  "code": "123456",
  "type": "email",
  "username": "username",
  "password": "Password123",
  "displayName": "Display Name",  // 可选
  "preferredLanguage": "en"  // "zh" 或 "en"
}
```

**响应：**
```json
{
  "token": "jwt_token_here",
  "expiresIn": 86400000,
  "user": {
    "id": "user_id",
    "username": "username",
    "email": "user@example.com",
    ...
  }
}
```

### 4. 发送密码重置验证码

**POST** `/api/auth/forgot-password/send-code`

**请求体：**
```json
{
  "identifier": "user@example.com",
  "type": "email"
}
```

**响应：**
```json
{
  "success": true,
  "message": "验证码已发送"
}
```

### 5. 重置密码

**POST** `/api/auth/forgot-password/reset`

**请求体：**
```json
{
  "identifier": "user@example.com",
  "code": "123456",
  "newPassword": "NewPassword123",
  "type": "email"
}
```

**响应：**
```json
{
  "success": true,
  "message": "密码重置成功"
}
```

## ⚙️ 配置说明

### 邮件配置（可选）

在 `application.properties` 中添加：

```properties
# 启用邮件功能
app.email.enabled=true

# SMTP 配置
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 短信配置（可选）

在 `application.properties` 中添加：

```properties
# 启用短信功能
app.sms.enabled=true

# TODO: 集成实际的短信服务（如Twilio、阿里云短信等）
```

### 开发环境

默认情况下，邮件和短信功能都是关闭的（`app.email.enabled=false`, `app.sms.enabled=false`）。

在开发环境中，验证码会直接打印到日志中，方便测试：

```
验证码 (REGISTER): user@example.com -> 123456
```

## 🔒 安全特性

1. **验证码有效期**：10分钟
2. **防频繁发送**：1分钟内不能重复发送
3. **验证码使用后失效**：验证成功后立即标记为已使用
4. **密码强度要求**：必须包含大小写字母
5. **用户名限制**：只能包含英文字母、数字和下划线

## 📝 使用流程

### 注册流程

1. 用户选择邮箱或手机号
2. 输入邮箱/手机号，点击"发送验证码"
3. 输入收到的6位验证码，点击"验证"
4. 设置用户名（仅英文）、密码（大小写字母）、确认密码、语言偏好
5. 点击"注册"完成注册

### 忘记密码流程

1. 点击"忘记密码"
2. 选择邮箱或手机号
3. 输入邮箱/手机号，点击"发送验证码"
4. 输入收到的6位验证码，点击"验证"
5. 设置新密码（大小写字母）、确认密码
6. 点击"重置密码"完成重置

## 🧪 测试

### 测试发送验证码

```bash
curl -X POST http://localhost:8080/api/auth/send-verification-code \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "test@example.com",
    "type": "email"
  }'
```

### 测试验证码验证

```bash
curl -X POST http://localhost:8080/api/auth/verify-code \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "test@example.com",
    "code": "123456",
    "type": "email"
  }'
```

### 测试注册

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "test@example.com",
    "code": "123456",
    "type": "email",
    "username": "testuser",
    "password": "Test123",
    "preferredLanguage": "en"
  }'
```

## 📦 依赖

已在 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

## 🔧 注意事项

1. **开发环境**：验证码会打印到日志，无需配置邮件/短信服务
2. **生产环境**：需要配置实际的邮件服务（SMTP）或短信服务
3. **验证码存储**：验证码存储在数据库中，建议定期清理过期验证码
4. **手机号注册**：使用手机号注册时，系统会生成临时邮箱 `{phone}@bridgeu.local`

## 🚀 下一步

1. 集成实际的短信服务（如Twilio、阿里云短信）
2. 添加定时任务清理过期验证码
3. 添加验证码发送频率限制（IP级别）
4. 添加验证码重试次数限制

