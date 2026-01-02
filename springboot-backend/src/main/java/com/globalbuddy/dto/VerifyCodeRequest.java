package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 验证验证码请求DTO
 */
@Data
public class VerifyCodeRequest {

    @NotBlank(message = "标识符不能为空")
    private String identifier; // 邮箱地址或手机号码

    @NotBlank(message = "验证码不能为空")
    @Size(min = 6, max = 6, message = "验证码必须是6位数字")
    private String code; // 6位验证码

    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "email|phone", flags = Pattern.Flag.CASE_INSENSITIVE, message = "类型必须是email或phone")
    private String type; // "email" 或 "phone"

    private String purpose = "REGISTER"; // 用途：REGISTER 或 RESET_PASSWORD，默认为 REGISTER
}

