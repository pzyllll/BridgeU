package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机号重置密码请求DTO（Firebase已验证手机号）
 */
@Data
public class PhoneResetPasswordRequest {

    @NotBlank(message = "手机号不能为空")
    private String phone; // 带国家区号的手机号，如 +66...

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    @Pattern(regexp = ".*[a-z].*", message = "密码必须包含至少一个小写字母")
    @Pattern(regexp = ".*[A-Z].*", message = "密码必须包含至少一个大写字母")
    private String newPassword;
}

