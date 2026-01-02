package com.globalbuddy.service;

import com.globalbuddy.model.VerificationCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 短信服务
 * 负责发送验证码短信
 * 
 * 注意：实际生产环境需要集成第三方短信服务（如Twilio、阿里云短信等）
 */
@Service
@Slf4j
public class SmsService {

    @Value("${app.sms.enabled:false}")
    private boolean smsEnabled;

    /**
     * 发送验证码短信
     * @param phoneNumber 手机号码
     * @param code 验证码
     * @param purpose 验证码用途
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String phoneNumber, String code, VerificationCode.CodePurpose purpose) {
        if (!smsEnabled) {
            log.warn("短信功能未启用，跳过发送验证码到: {}", phoneNumber);
            // 开发环境：直接打印验证码到日志
            log.info("验证码 ({}): {} -> {}", purpose, phoneNumber, code);
            return true; // 开发环境返回true以便测试
        }

        try {
            // TODO: 集成实际的短信服务
            // 示例：使用Twilio
            // Twilio.init(accountSid, authToken);
            // Message message = Message.creator(
            //     new PhoneNumber(phoneNumber),
            //     new PhoneNumber(fromNumber),
            //     String.format("您的验证码是：%s，有效期10分钟", code)
            // ).create();

            String message = purpose == VerificationCode.CodePurpose.REGISTER
                ? String.format("您的BridgeU注册验证码是：%s，有效期10分钟", code)
                : String.format("您的BridgeU密码重置验证码是：%s，有效期10分钟", code);

            log.info("短信验证码 ({}): {} -> {}", purpose, phoneNumber, code);
            log.info("短信内容: {}", message);
            
            // 实际生产环境需要调用短信API
            // 这里返回true以便测试
            return true;
        } catch (Exception e) {
            log.error("发送验证码短信失败: {}", e.getMessage(), e);
            return false;
        }
    }
}

