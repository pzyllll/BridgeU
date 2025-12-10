package com.globalbuddy.service;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Community;
import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.model.News;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.CommunityRepository;
import com.globalbuddy.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * News to Post Service
 * Automatically converts crawled news to community posts and pushes to frontend
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsToPostService {

    private final NewsRepository newsRepository;
    private final CommunityRepository communityRepository;
    private final CommunityPostRepository postRepository;
    private final AppUserRepository userRepository;
    private final LanguageDetectionService languageDetectionService;
    private final TranslationService translationService;

    // System user email (for auto-generated posts)
    private static final String SYSTEM_USER_EMAIL = "system@globalbuddy.com";
    // News community title
    private static final String NEWS_COMMUNITY_TITLE = "News & Information";

    /**
     * Convert unconverted news to posts
     * 
     * @param limit Maximum number of news items to convert (default 10)
     * @return Conversion result statistics
     */
    @Transactional
    public ConversionResult convertNewsToPosts(int limit) {
        log.info("Starting to convert news to posts, limit: {}", limit);

        // 1. Get or create system user
        AppUser systemUser = getOrCreateSystemUser();
        
        // 2. Get or create news community
        Community newsCommunity = getOrCreateNewsCommunity(systemUser);

        // 3. Query unconverted news (by checking if posts with same title already exist)
        List<News> newsList = findUnconvertedNews(limit);
        
        if (newsList.isEmpty()) {
            log.info("No news to convert");
            return ConversionResult.builder()
                    .totalProcessed(0)
                    .successCount(0)
                    .skipCount(0)
                    .errorCount(0)
                    .build();
        }

        log.info("Found {} news items to convert", newsList.size());

        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        for (News news : newsList) {
            try {
                // Check if post with same title already exists
                if (postExistsWithTitle(news.getTitle())) {
                    log.debug("Post already exists, skipping: {}", news.getTitle());
                    skipCount++;
                    continue;
                }

                // Create post
                CommunityPost post = createPostFromNews(news, newsCommunity, systemUser);
                postRepository.save(post);
                
                successCount++;
                log.debug("Successfully converted news to post: {} -> {}", news.getTitle(), post.getId());

            } catch (Exception e) {
                errorCount++;
                log.error("Failed to convert news: {} - {}", news.getTitle(), e.getMessage(), e);
            }
        }

        ConversionResult result = ConversionResult.builder()
                .totalProcessed(newsList.size())
                .successCount(successCount)
                .skipCount(skipCount)
                .errorCount(errorCount)
                .build();

        log.info("News to posts conversion completed: {}", result);
        return result;
    }

    /**
     * Convert specified news to post
     */
    @Transactional
    public CommunityPost convertSingleNewsToPost(Long newsId) {
        Optional<News> newsOpt = newsRepository.findById(newsId);
        if (!newsOpt.isPresent()) {
            throw new IllegalArgumentException("News not found: " + newsId);
        }

        News news = newsOpt.get();
        AppUser systemUser = getOrCreateSystemUser();
        Community newsCommunity = getOrCreateNewsCommunity(systemUser);

        if (postExistsWithTitle(news.getTitle())) {
            throw new IllegalStateException("Post already exists: " + news.getTitle());
        }

        CommunityPost post = createPostFromNews(news, newsCommunity, systemUser);
        return postRepository.save(post);
    }

    /**
     * Create post object with automatic translation
     */
    private CommunityPost createPostFromNews(News news, Community community, AppUser author) {
        // Build post content (including AI summary and original link)
        StringBuilder bodyBuilder = new StringBuilder();
        
        // AI intelligent summary (displayed first)
        if (news.getSummary() != null && !news.getSummary().isEmpty()) {
            bodyBuilder.append("📝 **AI Summary**\n\n");
            bodyBuilder.append(news.getSummary()).append("\n\n");
            bodyBuilder.append("---\n\n");
        } else {
            // If no AI summary, use title as temporary summary
            bodyBuilder.append("📝 **News Summary**\n\n");
            bodyBuilder.append(news.getTitle()).append("\n\n");
            bodyBuilder.append("---\n\n");
        }
        
        // Detailed content (if available)
        if (news.getOriginalContent() != null && !news.getOriginalContent().isEmpty()) {
            // If content is too long, truncate to first 500 characters
            String content = news.getOriginalContent();
            if (content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            bodyBuilder.append("📄 **Detailed Content**\n\n");
            bodyBuilder.append(content).append("\n\n");
            bodyBuilder.append("---\n\n");
        }
        
        // Original link (highlighted)
        if (news.getOriginalUrl() != null && !news.getOriginalUrl().isEmpty()) {
            bodyBuilder.append("🔗 **Read Original**: ").append(news.getOriginalUrl());
        } else {
            // Even if no original link, add a placeholder
            bodyBuilder.append("🔗 **Source**: ").append(news.getSource() != null ? news.getSource() : "Unknown");
        }

        String postBody = bodyBuilder.toString();
        String postTitle = news.getTitle();

        // Detect language of the post content
        String combinedText = (postTitle != null ? postTitle + " " : "") + (postBody != null ? postBody : "");
        String detectedLang = languageDetectionService.detectLanguage(combinedText);
        log.info("Detected language for news post: {} (title: {})", detectedLang, postTitle);

        // Build tags
        List<String> tags = new ArrayList<>();
        tags.add("News");
        if (news.getSource() != null) {
            tags.add(news.getSource());
        }
        // Can extract keywords from title as tags
        extractKeywordsFromTitle(news.getTitle(), tags);

        CommunityPost post = new CommunityPost();
        post.setId(java.util.UUID.randomUUID().toString());
        post.setCommunity(community);
        post.setAuthor(author);
        post.setTitle(postTitle);
        post.setBody(postBody);
        post.setTags(tags);
        post.setCategory("News & Information");
        post.setStatus(CommunityPost.Status.APPROVED); // Auto-approved
        post.setOriginalLanguage(detectedLang);

        // Auto-translate to Chinese and English
        try {
            // Extract actual content for translation (removing format markers)
            String actualContent = extractActualContent(postBody);
            String actualTitle = postTitle != null ? postTitle : "";
            
            // Translate the actual content
            TranslationService.TranslationResult translationResult = 
                translationService.translateContent(actualTitle, actualContent, detectedLang);
            
            // Rebuild translated content with original format markers
            String translatedBodyZh = rebuildTranslatedContent(postBody, translationResult.getBodyZh(), detectedLang, "zh");
            String translatedBodyEn = rebuildTranslatedContent(postBody, translationResult.getBodyEn(), detectedLang, "en");
            
            // Set Chinese title translation
            if (translationResult.getTitleZh() != null && !translationResult.getTitleZh().isEmpty()) {
                post.setTitleZh(translationResult.getTitleZh());
            } else if ("zh".equals(detectedLang)) {
                post.setTitleZh(postTitle);
            }
            
            // Set Chinese content translation (with fallback to original if translation fails)
            if (translatedBodyZh != null && !translatedBodyZh.isEmpty() && !translatedBodyZh.equals(postBody)) {
                post.setContentZh(translatedBodyZh);
                log.info("✅ Chinese translation completed for news post: {}", postTitle);
            } else {
                // Fallback: if detected language is Chinese, use original; otherwise use original as fallback
                if ("zh".equals(detectedLang)) {
                    post.setContentZh(postBody);
                } else {
                    post.setContentZh(postBody); // Fallback to original
                    log.warn("⚠️ Chinese translation failed or same as original, using original content as fallback");
                }
            }

            // Set English title translation
            if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                post.setTitleEn(translationResult.getTitleEn());
            } else if ("en".equals(detectedLang)) {
                post.setTitleEn(postTitle);
            }

            // Set English content translation (with fallback to original if translation fails)
            if (translatedBodyEn != null && !translatedBodyEn.isEmpty() && !translatedBodyEn.equals(postBody)) {
                post.setContentEn(translatedBodyEn);
                log.info("✅ English translation completed for news post: {}", postTitle);
            } else {
                // Fallback: if detected language is English, use original; otherwise use original as fallback
                if ("en".equals(detectedLang)) {
                    post.setContentEn(postBody);
                } else {
                    post.setContentEn(postBody); // Fallback to original
                    log.warn("⚠️ English translation failed or same as original, using original content as fallback");
                }
            }
        } catch (Exception e) {
            log.error("❌ Translation failed for news post: {} - {}", postTitle, e.getMessage(), e);
            // Fallback: use original content for both languages
            post.setContentZh(postBody);
            post.setContentEn(postBody);
        }

        return post;
    }

    /**
     * Extract actual content from post body (removing format markers for translation)
     * This extracts only the text content that needs to be translated, preserving structure
     */
    private String extractActualContent(String postBody) {
        if (postBody == null || postBody.isEmpty()) {
            return "";
        }
        
        String content = postBody;
        
        // Remove section headers with emojis and markdown (keep only the actual content)
        // Pattern: emoji + **Section Name** + newlines
        content = content.replaceAll("📝\\s*\\*\\*AI Summary\\*\\*\\s*\n+", "");
        content = content.replaceAll("📝\\s*\\*\\*News Summary\\*\\*\\s*\n+", "");
        content = content.replaceAll("📄\\s*\\*\\*Detailed Content\\*\\*\\s*\n+", "");
        content = content.replaceAll("🔗\\s*\\*\\*Read Original\\*\\*:\\s*", "");
        content = content.replaceAll("🔗\\s*\\*\\*Source\\*\\*:\\s*", "");
        
        // Remove separator lines
        content = content.replaceAll("---+\\s*\n*", "");
        
        // Remove URLs (they should not be translated)
        content = content.replaceAll("https?://[^\\s]+", "");
        
        // Remove markdown bold markers (but keep the text)
        content = content.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        
        // Clean up extra whitespace and newlines
        content = content.replaceAll("\n{3,}", "\n\n");
        content = content.trim();
        
        return content;
    }

    /**
     * Rebuild translated content with original format markers
     * This replaces the actual content while preserving all formatting markers
     */
    private String rebuildTranslatedContent(String originalBody, String translatedContent, String sourceLang, String targetLang) {
        if (translatedContent == null || translatedContent.isEmpty()) {
            return null;
        }
        
        // If source language matches target, return original
        if (sourceLang != null && sourceLang.equals(targetLang)) {
            return originalBody;
        }
        
        if (originalBody == null || originalBody.isEmpty()) {
            return translatedContent;
        }
        
        // Extract the structure from original body
        StringBuilder result = new StringBuilder();
        
        // Check for AI Summary section
        if (originalBody.contains("📝 **AI Summary**")) {
            result.append("📝 **AI Summary**\n\n");
            // Find the content after AI Summary header
            int summaryStart = originalBody.indexOf("📝 **AI Summary**");
            int contentStart = originalBody.indexOf("\n\n", summaryStart) + 2;
            if (contentStart > 1) {
                int nextSection = originalBody.indexOf("📄", contentStart);
                int nextLink = originalBody.indexOf("🔗", contentStart);
                int endPos = originalBody.length();
                if (nextSection > 0 && nextSection < endPos) endPos = nextSection;
                if (nextLink > 0 && nextLink < endPos) endPos = nextLink;
                // Use translated content for the summary part
                result.append(translatedContent);
                // Add remaining sections
                if (endPos < originalBody.length()) {
                    result.append("\n\n");
                    result.append(originalBody.substring(endPos));
                }
            } else {
                result.append(translatedContent);
            }
        } 
        // Check for News Summary section
        else if (originalBody.contains("📝 **News Summary**")) {
            result.append("📝 **News Summary**\n\n");
            result.append(translatedContent);
            // Add remaining sections if any
            int summaryEnd = originalBody.indexOf("📝 **News Summary**") + "📝 **News Summary**".length();
            int nextSection = originalBody.indexOf("📄", summaryEnd);
            int nextLink = originalBody.indexOf("🔗", summaryEnd);
            if (nextSection > 0 || nextLink > 0) {
                int startPos = Math.min(nextSection > 0 ? nextSection : Integer.MAX_VALUE, 
                                       nextLink > 0 ? nextLink : Integer.MAX_VALUE);
                result.append("\n\n");
                result.append(originalBody.substring(startPos));
            }
        }
        // Check for Detailed Content section
        else if (originalBody.contains("📄 **Detailed Content**")) {
            int detailedStart = originalBody.indexOf("📄 **Detailed Content**");
            result.append(originalBody.substring(0, detailedStart + "📄 **Detailed Content**\n\n".length()));
            result.append(translatedContent);
            // Add link section if exists
            int linkStart = originalBody.indexOf("🔗", detailedStart);
            if (linkStart > 0) {
                result.append("\n\n");
                result.append(originalBody.substring(linkStart));
            }
        }
        // If no special sections, just replace content while preserving links
        else {
            // Try to preserve any links at the end
            int linkStart = originalBody.indexOf("🔗");
            if (linkStart > 0) {
                result.append(translatedContent);
                result.append("\n\n");
                result.append(originalBody.substring(linkStart));
            } else {
                result.append(translatedContent);
            }
        }
        
        return result.toString();
    }

    /**
     * Extract keywords from title as tags
     */
    private void extractKeywordsFromTitle(String title, List<String> tags) {
        if (title == null || title.isEmpty()) {
            return;
        }
        
        // Simple keyword extraction (can be improved as needed)
        String[] commonKeywords = {"Thailand", "China", "Korea", "Study Abroad", "Visa", "Rental", "Food", "Travel", "Education", "University"};
        String lowerTitle = title.toLowerCase();
        
        for (String keyword : commonKeywords) {
            if (lowerTitle.contains(keyword.toLowerCase())) {
                if (!tags.contains(keyword)) {
                    tags.add(keyword);
                }
            }
        }
    }

    /**
     * Check if post with same title already exists
     */
    private boolean postExistsWithTitle(String title) {
        if (title == null || title.isEmpty()) {
            return false;
        }
        List<CommunityPost> posts = postRepository.findByTitle(title);
        return !posts.isEmpty();
    }

    /**
     * Find unconverted news
     */
    private List<News> findUnconvertedNews(int limit) {
        // Query latest news, sorted by creation time descending
        List<News> allNews = newsRepository.findAll();
        
        // Sort by creation time descending
        allNews.sort((n1, n2) -> {
            if (n1.getCreateTime() == null && n2.getCreateTime() == null) return 0;
            if (n1.getCreateTime() == null) return 1;
            if (n2.getCreateTime() == null) return -1;
            return n2.getCreateTime().compareTo(n1.getCreateTime());
        });
        
        List<News> unconverted = new ArrayList<>();
        int checkedCount = 0;
        
        log.info("Starting to find unconverted news, total {} news items in database", allNews.size());
        
        for (News news : allNews) {
            checkedCount++;
            if (news.getTitle() == null || news.getTitle().isEmpty()) {
                log.debug("Skipping news without title: ID={}", news.getId());
                continue;
            }
            
            if (postExistsWithTitle(news.getTitle())) {
                log.debug("News already converted, skipping: {}", news.getTitle());
                continue;
            }
            
            unconverted.add(news);
            log.info("Found unconverted news: {} (source: {})", news.getTitle(), news.getSource());
            
            if (unconverted.size() >= limit) {
                break;
            }
        }
        
        log.info("Checked {} news items, found {} unconverted news items", checkedCount, unconverted.size());
        return unconverted;
    }

    /**
     * Get or create system user
     */
    private AppUser getOrCreateSystemUser() {
        Optional<AppUser> userOpt = userRepository.findByEmail(SYSTEM_USER_EMAIL);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // Create system user
        AppUser systemUser = AppUser.create(
                "system_bot",
                SYSTEM_USER_EMAIL,
                "system_password_hash", // System user doesn't need to login
                "System Bot"
        );
        systemUser.setNationality("System");
        systemUser.setStudyingInCountry("Global");

        return userRepository.save(systemUser);
    }

    /**
     * Get or create news community
     */
    private Community getOrCreateNewsCommunity(AppUser creator) {
        List<Community> communities = communityRepository.findAll();
        for (Community community : communities) {
            if (NEWS_COMMUNITY_TITLE.equals(community.getTitle())) {
                return community;
            }
        }

        // Create news community
        Community newsCommunity = Community.create(
                NEWS_COMMUNITY_TITLE,
                "Automatically crawled news and information to help international students stay updated",
                "Global",
                "en"
        );
        newsCommunity.setTags(Arrays.asList("News", "Information", "Auto Update"));
        newsCommunity.setCreatedBy(creator);

        return communityRepository.save(newsCommunity);
    }

    /**
     * Conversion result statistics
     */
    @lombok.Data
    @lombok.Builder
    public static class ConversionResult {
        private int totalProcessed;
        private int successCount;
        private int skipCount;
        private int errorCount;

        @Override
        public String toString() {
            return String.format("Conversion Result - Total: %d, Success: %d, Skipped: %d, Failed: %d",
                    totalProcessed, successCount, skipCount, errorCount);
        }
    }
}

