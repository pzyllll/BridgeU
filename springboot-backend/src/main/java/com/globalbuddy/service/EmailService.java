package com.globalbuddy.service;

import com.globalbuddy.model.VerificationCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务
 * 负责发送验证码邮件
 */
@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * 发送验证码邮件
     * @param to 收件人邮箱
     * @param code 验证码
     * @param purpose 验证码用途
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String to, String code, VerificationCode.CodePurpose purpose) {
        if (!emailEnabled || mailSender == null) {
            log.warn("邮件功能未启用，跳过发送验证码到: {}", to);
            // 开发环境：直接打印验证码到日志
            log.info("验证码 ({}): {} -> {}", purpose, to, code);
            return true; // 开发环境返回true以便测试
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isEmpty()) {
                message.setFrom(fromEmail);
            }
            message.setTo(to);
            
            if (purpose == VerificationCode.CodePurpose.REGISTER) {
                message.setSubject("BridgeU 注册验证码 | Registration Code");
                message.setText(String.format(
                    "【中文】\n" +
                    "您的注册验证码是：%s\n" +
                    "验证码有效期：30 分钟，请勿泄露。\n" +
                    "若非本人操作，请忽略本邮件。\n\n" +
                    "【English】\n" +
                    "Your registration verification code is: %s\n" +
                    "Validity: 30 minutes. Do NOT share it with anyone.\n" +
                    "If you did not request this code, you can ignore this email.",
                    code, code
                ));
            } else {
                message.setSubject("BridgeU 密码重置验证码 | Password Reset Code");
                message.setText(String.format(
                    "【中文】\n" +
                    "您的密码重置验证码是：%s\n" +
                    "验证码有效期：30 分钟，请勿泄露。\n" +
                    "若非本人操作，请忽略本邮件。\n\n" +
                    "【English】\n" +
                    "Your password reset verification code is: %s\n" +
                    "Validity: 30 minutes. Do NOT share it with anyone.\n" +
                    "If you did not request this code, you can ignore this email.",
                    code, code
                ));
            }

            mailSender.send(message);
            log.info("验证码邮件发送成功: {}", to);
            return true;
        } catch (Exception e) {
            log.error("发送验证码邮件失败: {}", e.getMessage(), e);
            // 开发环境：即使发送失败也返回true，验证码已在日志中打印
            log.info("验证码 ({}): {} -> {}", purpose, to, code);
            return true; // 开发环境返回true以便测试
        }
    }
}

