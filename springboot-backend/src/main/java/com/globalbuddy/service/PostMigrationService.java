package com.globalbuddy.service;

import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Post Migration Service
 * One-time service to translate existing posts that don't have translations
 * This is a migration tool, not a regular feature
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostMigrationService {

    private final CommunityPostRepository postRepository;
    private final LanguageDetectionService languageDetectionService;
    private final TranslationService translationService;

    /**
     * Translate all posts that don't have translations
     * 
     * @param limit Maximum number of posts to process (0 = all)
     * @param forceRetranslate If true, retranslate even posts that already have translations
     * @return Migration result statistics
     */
    @Transactional
    public MigrationResult translateUntranslatedPosts(int limit, boolean forceRetranslate) {
        log.info("Starting post translation migration, limit: {}, forceRetranslate: {}", 
                limit == 0 ? "unlimited" : limit, forceRetranslate);

        // Find posts to translate
        List<CommunityPost> postsToTranslate = forceRetranslate 
                ? findAllPosts(limit) 
                : findUntranslatedPosts(limit);
        
        if (postsToTranslate.isEmpty()) {
            log.info("No posts need translation");
            return MigrationResult.builder()
                    .totalProcessed(0)
                    .successCount(0)
                    .skipCount(0)
                    .errorCount(0)
                    .build();
        }

        log.info("Found {} posts to translate", postsToTranslate.size());

        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (CommunityPost post : postsToTranslate) {
            try {
                // Skip if already has translations (unless force retranslate)
                if (!forceRetranslate && hasTranslations(post)) {
                    log.debug("Post already has translations, skipping: {}", post.getId());
                    skipCount++;
                    continue;
                }

                // Detect language if not already detected
                String detectedLang = post.getOriginalLanguage();
                if (detectedLang == null || detectedLang.isEmpty()) {
                    String combinedText = (post.getTitle() != null ? post.getTitle() + " " : "") + 
                                         (post.getBody() != null ? post.getBody() : "");
                    if (combinedText.trim().isEmpty()) {
                        log.warn("Post has no content, skipping: {}", post.getId());
                        skipCount++;
                        continue;
                    }
                    detectedLang = languageDetectionService.detectLanguage(combinedText);
                    post.setOriginalLanguage(detectedLang);
                    log.info("Detected language for post {}: {}", post.getId(), detectedLang);
                }
                
                // Translate if needed
                TranslationService.TranslationResult translationResult = 
                    translationService.translateContent(post.getTitle(), post.getBody(), detectedLang);
                
                // Set Chinese title translation (always update if force retranslate, or if missing)
                if (forceRetranslate || post.getTitleZh() == null || post.getTitleZh().isEmpty()) {
                    if (translationResult.getTitleZh() != null && !translationResult.getTitleZh().isEmpty()) {
                        post.setTitleZh(translationResult.getTitleZh());
                        log.info("✅ Set titleZh from translation for post {}", post.getId());
                    } else if (languageDetectionService.containsChinese(post.getTitle())) {
                        // 如果标题包含中文，使用原标题
                        post.setTitleZh(post.getTitle());
                        log.info("✅ Post {} title contains Chinese, using original as titleZh", post.getId());
                    } else if ("zh".equals(detectedLang)) {
                        post.setTitleZh(post.getTitle());
                        log.info("✅ Post {} detected as Chinese, using original as titleZh", post.getId());
                    }
                }
                
                // Set Chinese content translation (always update if force retranslate, or if missing)
                if (forceRetranslate || post.getContentZh() == null || post.getContentZh().isEmpty()) {
                    if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                        post.setContentZh(translationResult.getBodyZh());
                        log.info("✅ Set contentZh from translation for post {}", post.getId());
                    } else if (languageDetectionService.containsChinese(post.getBody())) {
                        // 如果内容包含中文，使用原内容
                        post.setContentZh(post.getBody());
                        log.info("✅ Post {} body contains Chinese, using original as contentZh", post.getId());
                    } else if ("zh".equals(detectedLang)) {
                        post.setContentZh(post.getBody());
                        log.info("✅ Post {} detected as Chinese, using original as contentZh", post.getId());
                    } else {
                        log.warn("⚠️ Chinese content translation failed for post {} (detected lang: {})", 
                                post.getId(), detectedLang);
                    }
                }
                
                // Set English title translation (always update if force retranslate, or if missing)
                if (forceRetranslate || post.getTitleEn() == null || post.getTitleEn().isEmpty()) {
                    if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                        if (!translationResult.getTitleEn().equals(post.getTitle()) || "en".equals(detectedLang)) {
                            post.setTitleEn(translationResult.getTitleEn());
                        } else {
                            post.setTitleEn(post.getTitle());
                        }
                    } else if ("en".equals(detectedLang)) {
                        post.setTitleEn(post.getTitle());
                    }
                }
                
                // Set English content translation (always update if force retranslate, or if missing)
                if (forceRetranslate || post.getContentEn() == null || post.getContentEn().isEmpty()) {
                    if (translationResult.getBodyEn() != null && !translationResult.getBodyEn().isEmpty()) {
                        // TranslationService now ensures the result is different from original
                        post.setContentEn(translationResult.getBodyEn());
                        log.info("✅ {} English translation for post: {} (length: {} -> {})", 
                                forceRetranslate ? "Updated" : "Added", 
                                post.getTitle(),
                                post.getBody() != null ? post.getBody().length() : 0,
                                translationResult.getBodyEn().length());
                    } else {
                        // Only use original as fallback if source language is English
                        if ("en".equals(detectedLang)) {
                            post.setContentEn(post.getBody());
                            log.info("✅ Post is already in English, using original");
                        } else {
                            // Don't set if translation failed - leave it null/empty
                            log.error("❌ English translation failed for post {} (detected lang: {}), leaving contentEn empty", 
                                    post.getId(), detectedLang);
                        }
                    }
                }
                
                // Save the updated post
                postRepository.save(post);
                successCount++;
                log.debug("Successfully translated post: {} -> {}", post.getId(), post.getTitle());

            } catch (Exception e) {
                errorCount++;
                log.error("Failed to translate post: {} - {}", post.getId(), e.getMessage(), e);
            }
        }

        MigrationResult result = MigrationResult.builder()
                .totalProcessed(postsToTranslate.size())
                .successCount(successCount)
                .skipCount(skipCount)
                .errorCount(errorCount)
                .build();

        log.info("Post translation migration completed: {}", result);
        return result;
    }

    /**
     * Find posts that don't have translations
     */
    private List<CommunityPost> findUntranslatedPosts(int limit) {
        List<CommunityPost> allPosts = postRepository.findAll();
        
        return allPosts.stream()
                .filter(post -> !hasTranslations(post))
                .limit(limit == 0 ? Long.MAX_VALUE : limit)
                .collect(Collectors.toList());
    }

    /**
     * Find all posts (for force retranslate)
     */
    private List<CommunityPost> findAllPosts(int limit) {
        List<CommunityPost> allPosts = postRepository.findAll();
        
        return allPosts.stream()
                .limit(limit == 0 ? Long.MAX_VALUE : limit)
                .collect(Collectors.toList());
    }

    /**
     * Clear all translation fields (content_zh and content_en) from all posts
     * 
     * @return Number of posts cleared
     */
    @Transactional
    public int clearAllTranslations() {
        log.info("Starting to clear all translation fields from posts table");
        
        List<CommunityPost> allPosts = postRepository.findAll();
        int clearedCount = 0;
        
        for (CommunityPost post : allPosts) {
            post.setContentZh(null);
            post.setContentEn(null);
            // Keep original_language as it's useful for re-translation
            postRepository.save(post);
            clearedCount++;
        }
        
        log.info("✅ Cleared translation fields for {} posts", clearedCount);
        return clearedCount;
    }

    /**
     * Check if post has translations
     */
    private boolean hasTranslations(CommunityPost post) {
        boolean hasZh = post.getContentZh() != null && !post.getContentZh().trim().isEmpty();
        boolean hasEn = post.getContentEn() != null && !post.getContentEn().trim().isEmpty();
        return hasZh && hasEn;
    }

    /**
     * Migration result statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class MigrationResult {
        private int totalProcessed;
        private int successCount;
        private int skipCount;
        private int errorCount;

        @Override
        public String toString() {
            return String.format("Migration Result - Total: %d, Success: %d, Skipped: %d, Failed: %d",
                    totalProcessed, successCount, skipCount, errorCount);
        }
    }
}

