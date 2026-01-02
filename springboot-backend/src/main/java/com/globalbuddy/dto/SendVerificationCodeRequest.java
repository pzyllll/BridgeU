package com.globalbuddy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送验证码请求DTO
 */
@Data
public class SendVerificationCodeRequest {

    @NotBlank(message = "标识符不能为空")
    private String identifier; // 邮箱地址或手机号码

    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = "email|phone", flags = Pattern.Flag.CASE_INSENSITIVE, message = "类型必须是email或phone")
    private String type; // "email" 或 "phone"
}

