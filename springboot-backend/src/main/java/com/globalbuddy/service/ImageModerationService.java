package com.globalbuddy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 图片内容审核服务（占位实现）
 * 后续可以对接真实的视觉模型，例如 DashScope 多模态或其他图片审核 API。
 */
@Service
@Slf4j
public class ImageModerationService {

    public enum ImageStatus {
        SAFE,
        SUSPICIOUS,
        UNSAFE
    }

    public static class ImageModerationResult {
        private final ImageStatus status;
        private final String reason;

        public ImageModerationResult(ImageStatus status, String reason) {
            this.status = status;
            this.reason = reason;
        }

        public ImageStatus getStatus() {
            return status;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 对图片 URL 做基础审核（当前实现仅基于文件名关键字，避免引入真实视觉 API）。
     */
    public ImageModerationResult moderateImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return new ImageModerationResult(ImageStatus.SAFE, "no_image");
        }

        String lower = imageUrl.toLowerCase();

        // 非常粗糙的文件名关键词检查，可后续替换为真正的图片审核服务
        if (lower.contains("porn") || lower.contains("nude") || lower.contains("xxx") || lower.contains("sex")) {
            log.warn("ImageModeration: unsafe filename detected: {}", imageUrl);
            return new ImageModerationResult(ImageStatus.UNSAFE, "filename_contains_sensitive_keywords");
        }

        return new ImageModerationResult(ImageStatus.SAFE, "filename_check_passed");
    }
}
