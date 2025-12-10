package com.globalbuddy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商家注册请求 DTO（包含基本信息，不含文件）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50个字符之间")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6个字符")
    private String password;

    @NotBlank(message = "商家名称不能为空")
    private String businessName;

    @NotBlank(message = "身份证号不能为空")
    private String idNumber;

    private String preferredLanguage;
}

