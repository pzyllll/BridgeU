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

    // Unicode blocks for language detection
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\u4E00-\u9FFF]");  // CJK Unified Ideographs
    private static final Pattern THAI_PATTERN = Pattern.compile("[\u0E00-\u0E7F]");    // Thai
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");
    
    // Common words and characters for validation
    private static final String[] CHINESE_COMMON_WORDS = {"的", "是", "在", "我", "有", "和", "了", "不", "人", "这"};
    private static final String[] THAI_COMMON_WORDS = {"และ", "ใน", "เป็น", "ของ", "ที่", "จะ", "ได้", "นี้", "กับ", "เขา"};
    private static final String[] ENGLISH_COMMON_WORDS = {"the", "be", "to", "of", "and", "a", "in", "that", "have", "I"};

    /**
     * Detect language of the text with enhanced accuracy
     * 
     * @param text Text to detect
     * @return Language code: "zh" (Chinese), "en" (English), "th" (Thai), or "unknown"
     */
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "unknown";
        }

        // First pass: count characters
        int chineseCount = 0;
        int thaiCount = 0;
        int englishWordCount = 0;
        int totalChars = 0;
        int wordCount = 0;

        // Count characters and words
        String[] words = text.split("\\s+");
        wordCount = words.length;
        
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) || Character.isIdeographic(c)) {
                totalChars++;
                String charStr = String.valueOf(c);
                if (CHINESE_PATTERN.matcher(charStr).find()) {
                    chineseCount++;
                } else if (THAI_PATTERN.matcher(charStr).find()) {
                    thaiCount++;
                } else if (ENGLISH_PATTERN.matcher(charStr).find()) {
                    // Count English words (not just characters)
                    if (charStr.matches("[a-zA-Z]")) {
                        englishWordCount++;
                    }
                }
            }
        }

        if (totalChars == 0) {
            return "unknown";
        }

        // Calculate ratios
        double chineseRatio = (double) chineseCount / totalChars;
        double thaiRatio = (double) thaiCount / totalChars;
        double englishRatio = wordCount > 0 ? (double) englishWordCount / wordCount : 0;

        // Second pass: validate with common words
        int chineseWordMatches = countCommonWords(text, CHINESE_COMMON_WORDS);
        int thaiWordMatches = countCommonWords(text, THAI_COMMON_WORDS);
        int englishWordMatches = countCommonWords(text.toLowerCase(), ENGLISH_COMMON_WORDS);

        log.debug("Language detection - Chinese: {:.2f}% ({} matches), Thai: {:.2f}% ({} matches), English: {:.2f}% ({} matches)", 
                 chineseRatio * 100, chineseWordMatches,
                 thaiRatio * 100, thaiWordMatches,
                 englishRatio * 100, englishWordMatches);

        // Decision logic - Chinese detection is prioritized and relaxed
        // Chinese characters are distinctive, so even a small ratio is reliable
        // 中文字符非常独特，即使比例很低也可以可靠地检测
        
        // 1. If there are any Chinese characters (>10%), it's likely Chinese
        if (chineseRatio > 0.1 && chineseCount >= 2) {
            log.debug("Detected as Chinese: ratio={}, count={}", chineseRatio, chineseCount);
            return "zh";
        }
        
        // 2. Thai detection - requires higher threshold and common words
        if (thaiRatio > 0.2 && thaiWordMatches >= 1) {
            log.debug("Detected as Thai: ratio={}, wordMatches={}", thaiRatio, thaiWordMatches);
            return "th";
        }
        
        // 3. English detection
        if (englishRatio > 0.3 && englishWordMatches >= 1) {
            log.debug("Detected as English: ratio={}, wordMatches={}", englishRatio, englishWordMatches);
            return "en";
        }
        
        // Fallback: character-based detection with lower thresholds
        if (chineseCount >= 1) {
            // Any Chinese character means it's Chinese
            log.debug("Fallback to Chinese: count={}", chineseCount);
            return "zh";
        } else if (thaiRatio > 0.15) {
            log.debug("Fallback to Thai: ratio={}", thaiRatio);
            return "th";
        } else if (englishRatio > 0.2) {
            log.debug("Fallback to English: ratio={}", englishRatio);
            return "en";
        }

        // Last resort: use the most common script
        if (thaiCount > 0 && thaiCount > englishWordCount) {
            return "th";
        } else if (englishWordCount > 0) {
            return "en";
        }

        return "unknown";
    }
    
    /**
     * Count occurrences of common words in the text
     */
    private int countCommonWords(String text, String[] commonWords) {
        int count = 0;
        for (String word : commonWords) {
            if (text.contains(word)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Check if text contains Chinese characters (relaxed validation)
     * For translation validation, we only check if there are Chinese characters
     */
    public boolean containsChinese(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Check for Chinese characters - count them
        int chineseCount = 0;
        int totalChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) || Character.isIdeographic(c)) {
                totalChars++;
                if (CHINESE_PATTERN.matcher(String.valueOf(c)).find()) {
                    chineseCount++;
                }
            }
        }
        
        // If at least 2 Chinese characters or 20% of text is Chinese, consider it contains Chinese
        return chineseCount >= 2 || (totalChars > 0 && (double) chineseCount / totalChars >= 0.2);
    }
    
    /**
     * Check if text contains Chinese characters (strict validation with common words)
     * Use this for language detection, not for translation validation
     */
    public boolean containsChineseStrict(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // First check for Chinese characters
        boolean hasChineseChars = CHINESE_PATTERN.matcher(text).find();
        if (!hasChineseChars) {
            return false;
        }
        
        // Then validate with common Chinese words to reduce false positives
        int chineseWordMatches = countCommonWords(text, CHINESE_COMMON_WORDS);
        return chineseWordMatches >= 1; // At least one common Chinese word found
    }

    /**
     * Check if text contains Thai characters (for translation validation)
     * Returns true only if Thai is the dominant language (>30% Thai characters)
     * This allows accepting translations that have some Thai mixed in but are primarily Chinese
     */
    public boolean containsThai(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Check for Thai characters - count them
        int thaiCount = 0;
        int chineseCount = 0;
        int totalChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) || Character.isIdeographic(c)) {
                totalChars++;
                if (THAI_PATTERN.matcher(String.valueOf(c)).find()) {
                    thaiCount++;
                } else if (CHINESE_PATTERN.matcher(String.valueOf(c)).find()) {
                    chineseCount++;
                }
            }
        }
        
        // If Chinese characters are more than Thai, don't consider it as Thai
        // This handles mixed Chinese-Thai text where Chinese is dominant
        if (chineseCount > thaiCount) {
            return false;
        }
        
        // Only return true if Thai is dominant (>30% of text)
        return totalChars > 0 && (double) thaiCount / totalChars > 0.3;
    }
    
    /**
     * Check if text has any Thai characters at all (strict check)
     */
    public boolean hasAnyThai(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return THAI_PATTERN.matcher(text).find();
    }
    
    /**
     * Check if text is primarily Chinese (Chinese characters > 50%)
     */
    public boolean isPrimarilyChinese(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        int chineseCount = 0;
        int totalChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) || Character.isIdeographic(c)) {
                totalChars++;
                if (CHINESE_PATTERN.matcher(String.valueOf(c)).find()) {
                    chineseCount++;
                }
            }
        }
        
        return totalChars > 0 && (double) chineseCount / totalChars > 0.5;
    }
    
    /**
     * Check if text is primarily Thai (Thai characters > 50%)
     */
    public boolean isPrimarilyThai(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        int thaiCount = 0;
        int totalChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c) || Character.isIdeographic(c)) {
                totalChars++;
                if (THAI_PATTERN.matcher(String.valueOf(c)).find()) {
                    thaiCount++;
                }
            }
        }
        
        return totalChars > 0 && (double) thaiCount / totalChars > 0.5;
    }
    
    /**
     * Check if text contains Thai characters (strict validation with common words)
     * Use this for language detection, not for translation validation
     */
    public boolean containsThaiStrict(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // First check for Thai characters
        boolean hasThaiChars = THAI_PATTERN.matcher(text).find();
        if (!hasThaiChars) {
            return false;
        }
        
        // Then validate with common Thai words to reduce false positives
        int thaiWordMatches = countCommonWords(text, THAI_COMMON_WORDS);
        return thaiWordMatches >= 1; // At least one common Thai word found
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

