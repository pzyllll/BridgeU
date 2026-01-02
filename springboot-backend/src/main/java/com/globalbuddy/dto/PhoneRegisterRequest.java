package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 手机注册请求 DTO（手机号已通过 Firebase 验证）
 */
@Data
public class PhoneRegisterRequest {

    /**
     * 手机号（建议为带国家区号的格式，例如 +66...）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含英文字母、数字和下划线")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6个字符")
    @Pattern(regexp = ".*[a-z].*", message = "密码必须包含至少一个小写字母")
    @Pattern(regexp = ".*[A-Z].*", message = "密码必须包含至少一个大写字母")
    private String password;

    /**
     * 显示名称，可选，默认为 username
     */
    private String displayName;

    /**
     * 界面语言偏好（zh/en），默认为 en
     */
    private String preferredLanguage;
}


