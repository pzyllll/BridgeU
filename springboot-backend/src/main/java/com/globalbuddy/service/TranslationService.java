package com.globalbuddy.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Translation Service
 * Uses Qwen-Max model to translate content between Chinese, English, and Thai
 */
@Slf4j
@Service
public class TranslationService {

    @Value("${dashscope.api.key:}")
    private String apiKey;

    private final Generation gen = new Generation();

    /**
     * Translate text to Chinese
     * 
     * @param text Text to translate
     * @param sourceLang Source language (zh/en/th)
     * @return Translated Chinese text
     */
    public String translateToChinese(String text, String sourceLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // If already Chinese, return as is
        if ("zh".equals(sourceLang)) {
            return text;
        }

        try {
            String prompt = buildTranslationPrompt(text, sourceLang, "zh");
            return callTranslationAPI(prompt);
        } catch (Exception e) {
            log.error("Failed to translate to Chinese: {} - {}", text.substring(0, Math.min(50, text.length())), e.getMessage(), e);
            return null; // Return null on failure, will use fallback
        }
    }

    /**
     * Translate text to English
     * 
     * @param text Text to translate
     * @param sourceLang Source language (zh/en/th)
     * @return Translated English text
     */
    public String translateToEnglish(String text, String sourceLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // If already English, return as is
        if ("en".equals(sourceLang)) {
            return text;
        }

        try {
            String prompt = buildTranslationPrompt(text, sourceLang, "en");
            return callTranslationAPI(prompt);
        } catch (Exception e) {
            log.error("Failed to translate to English: {} - {}", text.substring(0, Math.min(50, text.length())), e.getMessage(), e);
            return null; // Return null on failure, will use fallback
        }
    }

    /**
     * Build translation prompt for Qwen-Max
     */
    private String buildTranslationPrompt(String text, String sourceLang, String targetLang) {
        String sourceLangName = getLanguageName(sourceLang);
        String targetLangName = getLanguageName(targetLang);
        
        return String.format(
            "You are a professional translator. Translate the following text from %s to %s.\n\n" +
            "IMPORTANT RULES:\n" +
            "1. Translate ONLY the actual text content. Do NOT translate markdown formatting markers like **, emojis (📝, 📄, 🔗), or section headers like 'AI Summary', 'Detailed Content', 'Read Original', 'Source'.\n" +
            "2. Keep ALL markdown formatting exactly as it is (**, ---, line breaks, etc.).\n" +
            "3. Keep ALL emojis exactly as they are.\n" +
            "4. Keep section headers like '**AI Summary**', '**News Summary**', '**Detailed Content**', '**Read Original**:', '**Source**:' exactly as they are.\n" +
            "5. Only translate the actual content text between the formatting markers.\n" +
            "6. Return ONLY the translated text with original formatting preserved. Do not add explanations or notes.\n\n" +
            "Text to translate:\n%s",
            sourceLangName, targetLangName, text
        );
    }

    /**
     * Get language name in English
     */
    private String getLanguageName(String langCode) {
        switch (langCode) {
            case "zh": return "Chinese";
            case "en": return "English";
            case "th": return "Thai";
            default: return "the original language";
        }
    }

    /**
     * Call DashScope API for translation
     */
    private String callTranslationAPI(String prompt) throws NoApiKeyException, ApiException, InputRequiredException {
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("DashScope API Key not configured");
            throw new IllegalStateException("Please configure dashscope.api.key");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build());

        QwenParam param = QwenParam.builder()
                .apiKey(apiKey)
                .model("qwen-max-2025-01-25") // Updated model version
                .messages(messages)
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                .temperature(0.3f) // Lower temperature for more accurate translation
                .build();

        log.info("Calling DashScope API for translation...");
        String result = gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();
        log.info("Translation completed successfully");
        
        return result != null ? result.trim() : null;
    }

    /**
     * Translate both title and body to Chinese and English
     * 
     * @param title Original title
     * @param body Original body
     * @param sourceLang Source language
     * @return TranslationResult containing translated content
     */
    public TranslationResult translateContent(String title, String body, String sourceLang) {
        TranslationResult result = new TranslationResult();
        
        // Translate title and body separately for better accuracy
        String titleToTranslate = title != null ? title : "";
        String bodyToTranslate = body != null ? body : "";
        
        if (titleToTranslate.trim().isEmpty() && bodyToTranslate.trim().isEmpty()) {
            return result;
        }

        // Translate to Chinese
        try {
            if (!titleToTranslate.trim().isEmpty() && !"zh".equals(sourceLang)) {
                String titleZh = translateToChinese(titleToTranslate, sourceLang);
                if (titleZh != null && !titleZh.trim().isEmpty() && !titleZh.equals(titleToTranslate)) {
                    result.setTitleZh(titleZh);
                    log.debug("✅ Title translated to Chinese: {} -> {}", titleToTranslate.substring(0, Math.min(30, titleToTranslate.length())), titleZh.substring(0, Math.min(30, titleZh.length())));
                } else {
                    log.warn("⚠️ Title Chinese translation failed or same as original: {}", titleToTranslate.substring(0, Math.min(50, titleToTranslate.length())));
                    result.setTitleZh(null); // Don't set if translation failed
                }
            } else if ("zh".equals(sourceLang)) {
                result.setTitleZh(titleToTranslate); // Already Chinese
            } else {
                result.setTitleZh(null);
            }
            
            if (!bodyToTranslate.trim().isEmpty() && !"zh".equals(sourceLang)) {
                String bodyZh = translateToChinese(bodyToTranslate, sourceLang);
                if (bodyZh != null && !bodyZh.trim().isEmpty() && !bodyZh.equals(bodyToTranslate)) {
                    result.setBodyZh(bodyZh);
                    log.debug("✅ Body translated to Chinese (length: {} -> {})", bodyToTranslate.length(), bodyZh.length());
                } else {
                    log.warn("⚠️ Body Chinese translation failed or same as original (length: {})", bodyToTranslate.length());
                    result.setBodyZh(null); // Don't set if translation failed
                }
            } else if ("zh".equals(sourceLang)) {
                result.setBodyZh(bodyToTranslate); // Already Chinese
            } else {
                result.setBodyZh(null);
            }
            
            if (result.getBodyZh() != null) {
                log.info("✅ Chinese translation completed successfully");
            } else {
                log.warn("⚠️ Chinese translation not available");
            }
        } catch (Exception e) {
            log.error("❌ Exception during Chinese translation: {}", e.getMessage(), e);
            result.setTitleZh(null);
            result.setBodyZh(null);
        }

        // Translate to English
        try {
            if (!titleToTranslate.trim().isEmpty() && !"en".equals(sourceLang)) {
                String titleEn = translateToEnglish(titleToTranslate, sourceLang);
                if (titleEn != null && !titleEn.trim().isEmpty() && !titleEn.equals(titleToTranslate)) {
                    result.setTitleEn(titleEn);
                    log.debug("✅ Title translated to English: {} -> {}", titleToTranslate.substring(0, Math.min(30, titleToTranslate.length())), titleEn.substring(0, Math.min(30, titleEn.length())));
                } else {
                    log.warn("⚠️ Title English translation failed or same as original: {}", titleToTranslate.substring(0, Math.min(50, titleToTranslate.length())));
                    result.setTitleEn(null); // Don't set if translation failed
                }
            } else if ("en".equals(sourceLang)) {
                result.setTitleEn(titleToTranslate); // Already English
            } else {
                result.setTitleEn(null);
            }
            
            if (!bodyToTranslate.trim().isEmpty() && !"en".equals(sourceLang)) {
                String bodyEn = translateToEnglish(bodyToTranslate, sourceLang);
                if (bodyEn != null && !bodyEn.trim().isEmpty() && !bodyEn.equals(bodyToTranslate)) {
                    result.setBodyEn(bodyEn);
                    log.debug("✅ Body translated to English (length: {} -> {})", bodyToTranslate.length(), bodyEn.length());
                } else {
                    log.warn("⚠️ Body English translation failed or same as original (length: {})", bodyToTranslate.length());
                    result.setBodyEn(null); // Don't set if translation failed
                }
            } else if ("en".equals(sourceLang)) {
                result.setBodyEn(bodyToTranslate); // Already English
            } else {
                result.setBodyEn(null);
            }
            
            if (result.getBodyEn() != null) {
                log.info("✅ English translation completed successfully");
            } else {
                log.warn("⚠️ English translation not available");
            }
        } catch (Exception e) {
            log.error("❌ Exception during English translation: {}", e.getMessage(), e);
            result.setTitleEn(null);
            result.setBodyEn(null);
        }

        return result;
    }

    /**
     * Translation result container
     */
    public static class TranslationResult {
        private String titleZh;
        private String bodyZh;
        private String titleEn;
        private String bodyEn;

        public String getTitleZh() {
            return titleZh;
        }

        public void setTitleZh(String titleZh) {
            this.titleZh = titleZh;
        }

        public String getBodyZh() {
            return bodyZh;
        }

        public void setBodyZh(String bodyZh) {
            this.bodyZh = bodyZh;
        }

        public String getTitleEn() {
            return titleEn;
        }

        public void setTitleEn(String titleEn) {
            this.titleEn = titleEn;
        }

        public String getBodyEn() {
            return bodyEn;
        }

        public void setBodyEn(String bodyEn) {
            this.bodyEn = bodyEn;
        }
    }
}

