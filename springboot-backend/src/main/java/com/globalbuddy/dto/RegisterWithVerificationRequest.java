package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 带验证码的注册请求DTO
 */
@Data
public class RegisterWithVerificationRequest {

    @NotBlank(message = "标识符不能为空")
    private String identifier; // 邮箱地址或手机号码

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须是6位数字")
    private String code; // 验证码

    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "email|phone", flags = Pattern.Flag.CASE_INSENSITIVE, message = "类型必须是email或phone")
    private String type; // "email" 或 "phone"

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含英文字母、数字和下划线")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    @Pattern(regexp = ".*[a-z].*", message = "密码必须包含至少一个小写字母")
    @Pattern(regexp = ".*[A-Z].*", message = "密码必须包含至少一个大写字母")
    private String password;

    private String displayName; // 可选，默认为username

    private String preferredLanguage; // "zh" 或 "en"，默认为 "en"
}

