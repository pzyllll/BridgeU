package com.globalbuddy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Language Detection Service
 * Detects the language of text content (Chinese/English/Thai)
 */
@Slf4j
@Service
public class LanguageDetectionService {

    // Patterns for detecting different languages
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern THAI_PATTERN = Pattern.compile("[\\u0e00-\\u0e7f]");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");

    /**
     * Detect language of the text
     * 
     * @param text Text to detect
     * @return Language code: "zh" (Chinese), "en" (English), "th" (Thai), or "unknown"
     */
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "unknown";
        }

        int chineseCount = 0;
        int thaiCount = 0;
        int englishCount = 0;
        int totalChars = 0;

        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) || Character.isIdeographic(c)) {
                totalChars++;
                if (CHINESE_PATTERN.matcher(String.valueOf(c)).find()) {
                    chineseCount++;
                } else if (THAI_PATTERN.matcher(String.valueOf(c)).find()) {
                    thaiCount++;
                } else if (ENGLISH_PATTERN.matcher(String.valueOf(c)).find()) {
                    englishCount++;
                }
            }
        }

        if (totalChars == 0) {
            return "unknown";
        }

        double chineseRatio = (double) chineseCount / totalChars;
        double thaiRatio = (double) thaiCount / totalChars;
        double englishRatio = (double) englishCount / totalChars;

        log.debug("Language detection - Chinese: {:.2f}%, Thai: {:.2f}%, English: {:.2f}%", 
                 String.format("%.2f", chineseRatio * 100), 
                 String.format("%.2f", thaiRatio * 100), 
                 String.format("%.2f", englishRatio * 100));

        // Determine language based on highest ratio
        if (chineseRatio > 0.3) {
            return "zh";
        } else if (thaiRatio > 0.3) {
            return "th";
        } else if (englishRatio > 0.5) {
            return "en";
        } else if (chineseCount > 0) {
            return "zh";
        } else if (thaiCount > 0) {
            return "th";
        } else if (englishCount > 0) {
            return "en";
        }

        return "unknown";
    }

    /**
     * Check if text contains Chinese characters
     */
    public boolean containsChinese(String text) {
        return text != null && CHINESE_PATTERN.matcher(text).find();
    }

    /**
     * Check if text contains Thai characters
     */
    public boolean containsThai(String text) {
        return text != null && THAI_PATTERN.matcher(text).find();
    }

    /**
     * Check if text is primarily English
     */
    public boolean isPrimarilyEnglish(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        int englishCount = 0;
        int totalChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                totalChars++;
                if (ENGLISH_PATTERN.matcher(String.valueOf(c)).find()) {
                    englishCount++;
                }
            }
        }
        return totalChars > 0 && (double) englishCount / totalChars > 0.7;
    }
}

