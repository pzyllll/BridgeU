package com.globalbuddy.service;

import com.globalbuddy.model.VerificationCode;
import com.globalbuddy.repository.VerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * 验证码服务
 * 负责生成、存储和验证验证码
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationCodeService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final Random random = new Random();

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * 发送验证码
     * @param identifier 邮箱地址或手机号码
     * @param type 验证码类型（EMAIL 或 SMS）
     * @param purpose 验证码用途（REGISTER 或 RESET_PASSWORD）
     * @return 是否发送成功
     */
    @Transactional
    public boolean sendVerificationCode(String identifier, String type, String purpose) {
        try {
            VerificationCode.CodeType codeType = VerificationCode.CodeType.valueOf(type.toUpperCase());
            VerificationCode.CodePurpose codePurpose = VerificationCode.CodePurpose.valueOf(purpose.toUpperCase());

            // 检查是否在1分钟内已发送过验证码（防止频繁发送）
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
            if (verificationCodeRepository.existsRecentCode(identifier, codeType, oneMinuteAgo)) {
                log.warn("验证码发送过于频繁: {}", identifier);
                throw new RuntimeException("验证码发送过于频繁，请稍后再试");
            }

            // 生成验证码
            String code = generateCode();

            // 创建验证码记录
            VerificationCode verificationCode = VerificationCode.builder()
                .identifier(identifier)
                .type(codeType)
                .code(code)
                .purpose(codePurpose)
                .expiresAt(LocalDateTime.now().plusMinutes(30)) // 30分钟过期
                .used(false)
                .build();

            verificationCodeRepository.save(verificationCode);

            // 发送验证码
            boolean sent = false;
            if (codeType == VerificationCode.CodeType.EMAIL) {
                sent = emailService.sendVerificationCode(identifier, code, codePurpose);
            } else if (codeType == VerificationCode.CodeType.SMS) {
                sent = smsService.sendVerificationCode(identifier, code, codePurpose);
            }

            if (sent) {
                log.info("验证码发送成功: {} ({})", identifier, codeType);
                return true;
            } else {
                log.error("验证码发送失败: {} ({})", identifier, codeType);
                // 删除已创建的记录
                verificationCodeRepository.delete(verificationCode);
                return false;
            }
        } catch (IllegalArgumentException e) {
            log.error("无效的验证码类型或用途: type={}, purpose={}", type, purpose);
            throw new RuntimeException("无效的验证码类型或用途");
        } catch (Exception e) {
            log.error("发送验证码时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("发送验证码失败: " + e.getMessage());
        }
    }

    /**
     * 验证验证码（返回详细错误信息）
     * @param identifier 邮箱地址或手机号码
     * @param code 验证码
     * @param type 验证码类型
     * @param purpose 验证码用途
     * @return 验证结果对象，包含是否成功和错误信息
     */
    public static class VerificationResult {
        private final boolean success;
        private final String errorMessage;
        
        public VerificationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * 验证验证码（返回详细错误信息）
     * @param identifier 邮箱地址或手机号码
     * @param code 验证码
     * @param type 验证码类型
     * @param purpose 验证码用途
     * @return 验证结果对象
     */
    @Transactional
    public VerificationResult verifyCodeWithDetails(String identifier, String code, String type, String purpose) {
        try {
            VerificationCode.CodeType codeType = VerificationCode.CodeType.valueOf(type.toUpperCase());
            VerificationCode.CodePurpose codePurpose = VerificationCode.CodePurpose.valueOf(purpose.toUpperCase());

            // 先尝试查找有效的验证码
            Optional<VerificationCode> validCode = verificationCodeRepository.findLatestValidCode(
                identifier,
                codeType,
                codePurpose,
                LocalDateTime.now()
            );

            if (validCode.isPresent()) {
                VerificationCode verificationCode = validCode.get();
                // 验证码是否匹配
                if (!verificationCode.getCode().equals(code)) {
                    log.warn("验证码不匹配: {} (期望: {}, 实际: {})", identifier, verificationCode.getCode(), code);
                    return new VerificationResult(false, "验证码错误，请检查后重试");
                }
                // 注意：这里不立即标记为已使用，允许在注册时再次验证
                // 验证码将在注册成功后标记为已使用
                log.info("验证码验证成功: {} (暂不标记为已使用，等待注册完成)", identifier);
                return new VerificationResult(true, null);
            }

            // 如果没有找到有效验证码，查找最近的验证码（包括已过期或已使用的）以提供更详细的错误信息
            Optional<VerificationCode> latestCode = verificationCodeRepository.findAll().stream()
                .filter(vc -> vc.getIdentifier().equals(identifier) 
                    && vc.getType() == codeType 
                    && vc.getPurpose() == codePurpose)
                .max((vc1, vc2) -> vc1.getCreatedAt().compareTo(vc2.getCreatedAt()));

            if (latestCode.isEmpty()) {
                log.warn("未找到验证码: {}", identifier);
                return new VerificationResult(false, "未找到验证码，请先发送验证码");
            }

            VerificationCode verificationCode = latestCode.get();

            // 检查是否已使用
            if (verificationCode.getUsed()) {
                log.warn("验证码已使用: {}", identifier);
                return new VerificationResult(false, "验证码已使用，请重新发送验证码");
            }

            // 检查是否已过期
            if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("验证码已过期: {} (过期时间: {})", identifier, verificationCode.getExpiresAt());
                return new VerificationResult(false, "验证码已过期（有效期30分钟），请重新发送验证码");
            }

            // 如果到这里，说明验证码存在但可能不匹配
            log.warn("验证码不匹配: {} (期望: {}, 实际: {})", identifier, verificationCode.getCode(), code);
            return new VerificationResult(false, "验证码错误，请检查后重试");
        } catch (IllegalArgumentException e) {
            log.error("无效的验证码类型或用途: type={}, purpose={}", type, purpose);
            return new VerificationResult(false, "无效的验证码类型或用途");
        } catch (Exception e) {
            log.error("验证验证码时发生错误: {}", e.getMessage(), e);
            return new VerificationResult(false, "验证失败：" + e.getMessage());
        }
    }

    /**
     * 验证验证码（兼容旧方法）
     * @param identifier 邮箱地址或手机号码
     * @param code 验证码
     * @param type 验证码类型
     * @param purpose 验证码用途
     * @return 是否验证成功
     */
    @Transactional
    public boolean verifyCode(String identifier, String code, String type, String purpose) {
        return verifyCodeWithDetails(identifier, code, type, purpose).isSuccess();
    }

    /**
     * 标记验证码为已使用（用于注册成功后）
     * @param identifier 邮箱地址或手机号码
     * @param type 验证码类型
     * @param purpose 验证码用途
     */
    @Transactional
    public void markCodeAsUsed(String identifier, String type, String purpose) {
        try {
            VerificationCode.CodeType codeType = VerificationCode.CodeType.valueOf(type.toUpperCase());
            VerificationCode.CodePurpose codePurpose = VerificationCode.CodePurpose.valueOf(purpose.toUpperCase());
            
            // 查找最新的未使用验证码
            Optional<VerificationCode> validCode = verificationCodeRepository.findLatestValidCode(
                identifier,
                codeType,
                codePurpose,
                LocalDateTime.now()
            );
            
            if (validCode.isPresent()) {
                verificationCodeRepository.markAsUsed(validCode.get().getId());
                log.info("验证码已标记为已使用: {}", identifier);
            } else {
                log.warn("未找到可标记的验证码: {}", identifier);
            }
        } catch (Exception e) {
            log.error("标记验证码为已使用时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("标记验证码为已使用时发生错误: " + e.getMessage());
        }
    }

    /**
     * 清理过期的验证码（定时任务调用）
     */
    @Transactional
    public void cleanupExpiredCodes() {
        LocalDateTime now = LocalDateTime.now();
        verificationCodeRepository.deleteExpiredCodes(now);
        log.info("已清理过期的验证码");
    }
}

