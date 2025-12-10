package com.globalbuddy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 简单的证件类型检测服务
 * 基于图片宽高比粗略判断身份证（横向卡片）或营业执照（更接近竖向/正方形）
 * 注意：这是启发式检测，生产环境应接入更可靠的 OCR/文档识别服务
 */
@Service
@Slf4j
public class DocumentDetectionService {

    public enum DocumentType {
        ID_CARD,
        BUSINESS_LICENSE,
        UNKNOWN
    }

    /**
     * 根据图片尺寸和宽高比简单判断证件类型
     */
    public DocumentType detectDocumentType(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("DocumentDetectionService: file is empty");
            return DocumentType.UNKNOWN;
        }

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            log.warn("DocumentDetectionService: unable to read image from file");
            return DocumentType.UNKNOWN;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        if (width == 0 || height == 0) {
            return DocumentType.UNKNOWN;
        }

        double ratio = (double) width / height;
        log.info("DocumentDetectionService: detected image size {}x{}, ratio={}", width, height, String.format("%.2f", ratio));

        // 简单启发式规则：
        // - 身份证：通常为横向卡片，宽高比大约 1.4 - 1.9
        // - 营业执照：通常为竖向/接近正方形，这里设定阈值 0.6 - 1.3
        if (ratio >= 1.3 && ratio <= 1.95) {
            return DocumentType.ID_CARD;
        }
        if (ratio >= 0.6 && ratio <= 1.35) {
            return DocumentType.BUSINESS_LICENSE;
        }
        return DocumentType.UNKNOWN;
    }
}

