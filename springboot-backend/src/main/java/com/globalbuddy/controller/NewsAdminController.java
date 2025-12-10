package com.globalbuddy.controller;

import com.globalbuddy.model.News;
import com.globalbuddy.repository.NewsRepository;
import com.globalbuddy.scheduler.NewsScheduler;
import com.globalbuddy.service.AiSummaryService;
import com.globalbuddy.service.LanguageDetectionService;
import com.globalbuddy.service.NewsCrawlerService;
import com.globalbuddy.service.NewsToPostService;
import com.globalbuddy.service.PostMigrationService;
import com.globalbuddy.service.RssFeedService;
import com.globalbuddy.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * News Admin Controller
 * Provides APIs to manually trigger news crawling and AI summarization for debugging and manual data refresh
 */
@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsAdminController {

    private final NewsScheduler newsScheduler;
    private final AiSummaryService aiSummaryService;
    private final NewsToPostService newsToPostService;
    private final NewsCrawlerService newsCrawlerService;
    private final RssFeedService rssFeedService;
    private final PostMigrationService postMigrationService;
    private final TranslationService translationService;
    private final LanguageDetectionService languageDetectionService;
    private final NewsRepository newsRepository;

    /**
     * Manually trigger news refresh task:
     * - Crawl latest news
     * - Call AI to generate summary
     * - Save to database
     *
     * Usage:
     *   POST http://localhost:8080/api/news/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshNews() {
        log.info("Received manual news refresh request");
        Map<String, Object> resp = new HashMap<>();
        try {
            long start = System.currentTimeMillis();
            newsScheduler.manualTrigger();
            long cost = System.currentTimeMillis() - start;

            resp.put("success", true);
            resp.put("message", "News crawling and AI summarization task triggered");
            resp.put("costMs", cost);

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("Manual news refresh failed: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Manual news refresh failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Test AI summary functionality
     * Used to debug if AI service is working properly
     * 
     * POST /api/news/test-ai
     * Body: {"text": "Text content to summarize"}
     */
    @PostMapping("/test-ai")
    public ResponseEntity<Map<String, Object>> testAi(@RequestBody Map<String, String> request) {
        log.info("Received AI test request");
        Map<String, Object> resp = new HashMap<>();
        try {
            String text = request.get("text");
            if (text == null || text.isEmpty()) {
                text = "This is a test news content to verify if the AI summary functionality is working properly.";
            }
            
            String summary = aiSummaryService.generateSummary(text);
            
            resp.put("success", true);
            resp.put("input", text);
            resp.put("summary", summary);
            resp.put("inputLength", text.length());
            resp.put("summaryLength", summary != null ? summary.length() : 0);
            
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("AI test failed: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "AI test failed: " + e.getMessage());
            resp.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Test translation functionality
     * Used to verify if DashScope API Key is correctly configured and working
     * 
     * POST /api/news/test-translation
     * Body (optional): {"text": "Text to translate", "sourceLang": "en", "targetLang": "zh"}
     */
    @PostMapping("/test-translation")
    public ResponseEntity<Map<String, Object>> testTranslation(@RequestBody(required = false) Map<String, String> request) {
        log.info("Received translation test request");
        Map<String, Object> resp = new HashMap<>();
        
        try {
            String testText = "Hello, this is a test message.";
            String sourceLang = "en";
            String targetLang = "zh";
            
            if (request != null) {
                testText = request.getOrDefault("text", testText);
                sourceLang = request.getOrDefault("sourceLang", sourceLang);
                targetLang = request.getOrDefault("targetLang", targetLang);
            }
            
            log.info("Testing translation: {} -> {} (text: {})", sourceLang, targetLang, testText);
            
            String translatedText;
            if ("zh".equals(targetLang)) {
                translatedText = translationService.translateToChinese(testText, sourceLang);
            } else if ("en".equals(targetLang)) {
                translatedText = translationService.translateToEnglish(testText, sourceLang);
            } else {
                resp.put("success", false);
                resp.put("message", "Unsupported target language. Use 'zh' or 'en'");
                return ResponseEntity.badRequest().body(resp);
            }
            
            if (translatedText == null) {
                resp.put("success", false);
                resp.put("message", "Translation returned null. API call may have failed.");
                resp.put("input", testText);
                resp.put("sourceLang", sourceLang);
                resp.put("targetLang", targetLang);
                return ResponseEntity.internalServerError().body(resp);
            }
            
            resp.put("success", true);
            resp.put("message", "Translation API test successful");
            resp.put("input", testText);
            resp.put("sourceLang", sourceLang);
            resp.put("targetLang", targetLang);
            resp.put("translated", translatedText);
            resp.put("inputLength", testText.length());
            resp.put("translatedLength", translatedText.length());
            
            log.info("✅ Translation test successful: {} -> {}", testText, translatedText);
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("❌ Translation test failed: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Translation test failed: " + e.getMessage());
            resp.put("error", e.getClass().getSimpleName());
            resp.put("errorDetails", e.getMessage());
            
            // Check if it's an API key issue
            if (e.getMessage() != null && (e.getMessage().contains("API Key") || 
                e.getMessage().contains("dashscope.api.key") ||
                e.getMessage().contains("NoApiKeyException"))) {
                resp.put("apiKeyIssue", true);
                resp.put("suggestion", "Please check if dashscope.api.key is correctly configured in application.properties");
            }
            
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Manually trigger: Crawl news and automatically convert to posts
     * 
     * Process:
     * 1. Crawl latest news
     * 2. Generate AI summary
     * 3. Save to news table
     * 4. Automatically convert to community posts
     * 5. Push to frontend for display
     * 
     * POST /api/news/crawl-and-convert
     * Body (optional): {"limit": 10}  // Maximum number of news items to convert
     */
    @PostMapping("/crawl-and-convert")
    public ResponseEntity<Map<String, Object>> crawlAndConvertToPosts(
            @RequestBody(required = false) Map<String, Object> request) {
        log.info("Received crawl and convert to posts request");
        Map<String, Object> resp = new HashMap<>();
        
        try {
            long start = System.currentTimeMillis();
            
            // Step 1: Crawl news first (including all Thai news websites and Chiang Mai University)
            log.info("Step 1: Starting to crawl news (all Thai news websites + Chiang Mai University)...");
            newsScheduler.manualTrigger();
            
            // Step 2: Convert to posts
            int limit = 10;
            if (request != null && request.containsKey("limit")) {
                Object limitObj = request.get("limit");
                if (limitObj instanceof Number) {
                    limit = ((Number) limitObj).intValue();
                }
            }
            
            log.info("Step 2: Starting to convert news to posts, limit: {}", limit);
            NewsToPostService.ConversionResult result = newsToPostService.convertNewsToPosts(limit);
            
            long cost = System.currentTimeMillis() - start;
            
            resp.put("success", true);
            resp.put("message", "Crawl and conversion completed");
            resp.put("crawlCostMs", cost);
            resp.put("conversionResult", Map.of(
                    "totalProcessed", result.getTotalProcessed(),
                    "successCount", result.getSuccessCount(),
                    "skipCount", result.getSkipCount(),
                    "errorCount", result.getErrorCount()
            ));
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("Failed to crawl and convert to posts: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Failed to crawl and convert to posts: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Only convert already crawled news to posts (without re-crawling)
     * 
     * POST /api/news/convert-to-posts
     * Body (optional): {"limit": 10}
     */
    @PostMapping("/convert-to-posts")
    public ResponseEntity<Map<String, Object>> convertToPosts(
            @RequestBody(required = false) Map<String, Object> request) {
        log.info("Received convert news to posts request");
        Map<String, Object> resp = new HashMap<>();
        
        try {
            int limit = 10;
            if (request != null && request.containsKey("limit")) {
                Object limitObj = request.get("limit");
                if (limitObj instanceof Number) {
                    limit = ((Number) limitObj).intValue();
                }
            }
            
            NewsToPostService.ConversionResult result = newsToPostService.convertNewsToPosts(limit);
            
            resp.put("success", true);
            resp.put("message", "Conversion completed");
            resp.put("result", Map.of(
                    "totalProcessed", result.getTotalProcessed(),
                    "successCount", result.getSuccessCount(),
                    "skipCount", result.getSkipCount(),
                    "errorCount", result.getErrorCount()
            ));
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("Failed to convert news to posts: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Conversion failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Test RSS feed
     * Used to test and verify if RSS feed is available
     * 
     * POST /api/news/test-rss
     * Body: {"url": "https://example.com/rss.xml", "source": "Source name"}
     */
    @PostMapping("/test-rss")
    public ResponseEntity<Map<String, Object>> testRss(@RequestBody Map<String, String> request) {
        log.info("Received RSS feed test request");
        Map<String, Object> resp = new HashMap<>();
        
        try {
            String feedUrl = request.get("url");
            String source = request.getOrDefault("source", "Test Source");
            
            if (feedUrl == null || feedUrl.isEmpty()) {
                resp.put("success", false);
                resp.put("message", "RSS URL cannot be empty");
                return ResponseEntity.badRequest().body(resp);
            }
            
            long start = System.currentTimeMillis();
            
            // Call RSS service
            List<com.globalbuddy.model.News> newsList = rssFeedService.fetchNewsFromRss(feedUrl, source, 10);
            
            long cost = System.currentTimeMillis() - start;
            
            // Build response
            List<Map<String, Object>> newsData = new ArrayList<>();
            for (com.globalbuddy.model.News news : newsList) {
                Map<String, Object> newsMap = new HashMap<>();
                newsMap.put("title", news.getTitle());
                newsMap.put("url", news.getOriginalUrl());
                newsMap.put("source", news.getSource());
                newsMap.put("summary", news.getSummary());
                newsMap.put("publishDate", news.getPublishDate());
                newsData.add(newsMap);
            }
            
            resp.put("success", true);
            resp.put("message", "RSS feed test completed");
            resp.put("feedUrl", feedUrl);
            resp.put("count", newsList.size());
            resp.put("costMs", cost);
            resp.put("news", newsData);
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("RSS feed test failed: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Test failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Test Chiang Mai University news crawling
     * Used to debug and verify if crawler is working properly
     * 
     * POST /api/news/test-cmu-crawl
     */
    @PostMapping("/test-cmu-crawl")
    public ResponseEntity<Map<String, Object>> testCmuCrawl() {
        log.info("Received Chiang Mai University news crawling test request");
        Map<String, Object> resp = new HashMap<>();
        
        try {
            long start = System.currentTimeMillis();
            
            // Directly call crawler service
            List<com.globalbuddy.model.News> newsList = newsCrawlerService.crawlChiangMaiUniversity();
            
            long cost = System.currentTimeMillis() - start;
            
            // Build response
            List<Map<String, Object>> newsData = new ArrayList<>();
            for (com.globalbuddy.model.News news : newsList) {
                Map<String, Object> newsMap = new HashMap<>();
                newsMap.put("title", news.getTitle());
                newsMap.put("url", news.getOriginalUrl());
                newsMap.put("source", news.getSource());
                newsMap.put("summary", news.getSummary());
                newsData.add(newsMap);
            }
            
            resp.put("success", true);
            resp.put("message", "Chiang Mai University news crawling test completed");
            resp.put("count", newsList.size());
            resp.put("costMs", cost);
            resp.put("news", newsData);
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("Chiang Mai University news crawling test failed: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Test failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * One-time migration: Translate existing posts that don't have translations
     * 
     * This is a one-time data migration endpoint to translate existing posts.
     * After migration is complete, new posts will be automatically translated.
     * 
     * POST /api/news/migrate-posts?limit=100&force=false
     * 
     * @param limit Maximum number of posts to process (0 = all, default 100)
     * @param force If true, retranslate all posts even if they already have translations (default false)
     */
    @PostMapping("/migrate-posts")
    public ResponseEntity<Map<String, Object>> migratePosts(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "false") boolean force) {
        log.info("Received post migration request, limit: {}, force: {}", 
                limit == 0 ? "unlimited" : limit, force);
        Map<String, Object> resp = new HashMap<>();
        
        try {
            long start = System.currentTimeMillis();
            PostMigrationService.MigrationResult result = postMigrationService.translateUntranslatedPosts(limit, force);
            long cost = System.currentTimeMillis() - start;
            
            resp.put("success", true);
            resp.put("message", "Post migration completed");
            resp.put("totalProcessed", result.getTotalProcessed());
            resp.put("successCount", result.getSuccessCount());
            resp.put("skipCount", result.getSkipCount());
            resp.put("errorCount", result.getErrorCount());
            resp.put("limit", limit);
            resp.put("costMs", cost);
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("Post migration failed: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Migration failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Clear all translation fields (content_zh and content_en) from posts table
     * 
     * This endpoint clears all translation data to allow for a fresh translation.
     * After clearing, you should run migrate-posts to retranslate all posts.
     * 
     * POST /api/news/clear-translations
     */
    @PostMapping("/clear-translations")
    public ResponseEntity<Map<String, Object>> clearTranslations() {
        log.info("Received clear translations request");
        Map<String, Object> resp = new HashMap<>();
        
        try {
            long start = System.currentTimeMillis();
            int clearedCount = postMigrationService.clearAllTranslations();
            long cost = System.currentTimeMillis() - start;
            
            resp.put("success", true);
            resp.put("message", "All translation fields cleared successfully");
            resp.put("clearedCount", clearedCount);
            resp.put("costMs", cost);
            
            log.info("✅ Cleared translations for {} posts in {} ms", clearedCount, cost);
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("Failed to clear translations: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "Failed to clear translations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Translate existing news items that don't have translations
     * 
     * This endpoint translates existing news items in the news table.
     * It will translate title and summary to Chinese and English.
     * 
     * POST /api/news/translate-news?limit=100&force=false
     * 
     * @param limit Maximum number of news items to process (0 = all, default 100)
     * @param force If true, retranslate all news even if they already have translations (default false)
     */
    @PostMapping("/translate-news")
    public ResponseEntity<Map<String, Object>> translateNews(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "false") boolean force) {
        log.info("Received news translation request, limit: {}, force: {}", 
                limit == 0 ? "unlimited" : limit, force);
        Map<String, Object> resp = new HashMap<>();
        
        try {
            long start = System.currentTimeMillis();
            
            // Get all news items
            List<News> allNews = newsRepository.findAll();
            if (limit > 0 && allNews.size() > limit) {
                allNews = allNews.subList(0, limit);
            }
            
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;
            
            for (News news : allNews) {
                try {
                    // Skip if already has translations (unless force retranslate)
                    if (!force && hasNewsTranslations(news)) {
                        log.debug("News already has translations, skipping: {}", news.getId());
                        skipCount++;
                        continue;
                    }
                    
                    // Detect language if needed
                    String titleToTranslate = news.getTitle() != null ? news.getTitle() : "";
                    String summaryToTranslate = news.getSummary() != null ? news.getSummary() : "";
                    
                    if (titleToTranslate.isEmpty() && summaryToTranslate.isEmpty()) {
                        log.warn("News has no content to translate, skipping: {}", news.getId());
                        skipCount++;
                        continue;
                    }
                    
                    String combinedText = titleToTranslate + " " + summaryToTranslate;
                    String detectedLang = languageDetectionService.detectLanguage(combinedText);
                    log.info("Detected language for news {}: {}", news.getId(), detectedLang);
                    
                    // 检查标题的实际语言：如果标题是泰语但 detectedLang 是中文，需要修正
                    boolean titleIsThai = languageDetectionService.containsThai(titleToTranslate) && 
                                          !languageDetectionService.containsChinese(titleToTranslate);
                    boolean titleIsChinese = languageDetectionService.containsChinese(titleToTranslate) && 
                                             !languageDetectionService.containsThai(titleToTranslate);
                    
                    // 如果标题是泰语，但 detectedLang 不是泰语，需要强制翻译
                    String langForTranslation = detectedLang;
                    if (titleIsThai && !"th".equals(detectedLang)) {
                        log.warn("⚠️ Title is Thai but detectedLang is {}, forcing translation from Thai", detectedLang);
                        langForTranslation = "th";
                    } else if (titleIsChinese && !"zh".equals(detectedLang)) {
                        log.warn("⚠️ Title is Chinese but detectedLang is {}, forcing translation from Chinese", detectedLang);
                        langForTranslation = "zh";
                    }
                    
                    // Translate title and summary (使用修正后的语言)
                    TranslationService.TranslationResult translationResult = 
                        translationService.translateContent(titleToTranslate, summaryToTranslate, langForTranslation);
                    
                    log.info("🔍 Translation result for news {}: titleZh={}, bodyZh={}, titleEn={}, bodyEn={}", 
                            news.getId(),
                            translationResult.getTitleZh() != null ? translationResult.getTitleZh().substring(0, Math.min(50, translationResult.getTitleZh().length())) : "null",
                            translationResult.getBodyZh() != null ? "length=" + translationResult.getBodyZh().length() : "null",
                            translationResult.getTitleEn() != null ? translationResult.getTitleEn().substring(0, Math.min(50, translationResult.getTitleEn().length())) : "null",
                            translationResult.getBodyEn() != null ? "length=" + translationResult.getBodyEn().length() : "null");
                    
                    boolean hasTranslation = false;
                    
                    // Set Chinese translations
                    if (translationResult.getTitleZh() != null && !translationResult.getTitleZh().isEmpty()) {
                        // 验证翻译结果确实包含中文字符，而不是泰语或其他语言
                        if (languageDetectionService.containsChinese(translationResult.getTitleZh()) && 
                            !languageDetectionService.containsThai(translationResult.getTitleZh())) {
                            news.setTitleZh(translationResult.getTitleZh());
                            log.info("✅ Set titleZh for news {}: {}", news.getId(), translationResult.getTitleZh().substring(0, Math.min(50, translationResult.getTitleZh().length())));
                            hasTranslation = true;
                        } else {
                            log.warn("⚠️ Translation result is not Chinese (contains Thai or no Chinese): {} for news: {}", 
                                    translationResult.getTitleZh().substring(0, Math.min(50, translationResult.getTitleZh().length())), news.getId());
                        }
                    }
                    
                    // 如果 titleZh 仍然为空，且原始语言是中文，且标题确实包含中文
                    if (news.getTitleZh() == null || news.getTitleZh().isEmpty()) {
                        if ("zh".equals(detectedLang) && languageDetectionService.containsChinese(titleToTranslate) && 
                            !languageDetectionService.containsThai(titleToTranslate)) {
                            news.setTitleZh(titleToTranslate);
                            log.info("✅ News {} is already in Chinese, using original title", news.getId());
                            hasTranslation = true;
                        } else {
                            log.warn("⚠️ TitleZh is null or empty for news {} (detectedLang: {}, title contains Chinese: {}, title contains Thai: {})", 
                                    news.getId(), detectedLang, 
                                    languageDetectionService.containsChinese(titleToTranslate),
                                    languageDetectionService.containsThai(titleToTranslate));
                        }
                    }
                    
                    if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                        news.setSummaryZh(translationResult.getBodyZh());
                        log.info("✅ Set summaryZh for news {} (length: {})", news.getId(), translationResult.getBodyZh().length());
                        hasTranslation = true;
                    } else if ("zh".equals(detectedLang)) {
                        news.setSummaryZh(summaryToTranslate);
                        log.info("✅ News {} summary is already in Chinese, using original", news.getId());
                        hasTranslation = true;
                    } else {
                        log.warn("⚠️ SummaryZh is null or empty for news {} (detectedLang: {})", news.getId(), detectedLang);
                    }
                    
                    // Set English translations
                    if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                        news.setTitleEn(translationResult.getTitleEn());
                        log.info("✅ Set titleEn for news {}: {}", news.getId(), translationResult.getTitleEn().substring(0, Math.min(50, translationResult.getTitleEn().length())));
                        hasTranslation = true;
                    } else if ("en".equals(detectedLang)) {
                        news.setTitleEn(titleToTranslate);
                        log.info("✅ News {} is already in English, using original title", news.getId());
                        hasTranslation = true;
                    }
                    
                    if (translationResult.getBodyEn() != null && !translationResult.getBodyEn().isEmpty()) {
                        news.setSummaryEn(translationResult.getBodyEn());
                        log.info("✅ Set summaryEn for news {} (length: {})", news.getId(), translationResult.getBodyEn().length());
                        hasTranslation = true;
                    } else if ("en".equals(detectedLang)) {
                        news.setSummaryEn(summaryToTranslate);
                        log.info("✅ News {} summary is already in English, using original", news.getId());
                        hasTranslation = true;
                    }
                    
                    // Save the updated news
                    if (hasTranslation) {
                        News savedNews = newsRepository.save(news);
                        log.info("✅ Saved news {} with translations: titleZh={}, summaryZh={}, titleEn={}, summaryEn={}", 
                                savedNews.getId(),
                                savedNews.getTitleZh() != null && !savedNews.getTitleZh().isEmpty(),
                                savedNews.getSummaryZh() != null && !savedNews.getSummaryZh().isEmpty(),
                                savedNews.getTitleEn() != null && !savedNews.getTitleEn().isEmpty(),
                                savedNews.getSummaryEn() != null && !savedNews.getSummaryEn().isEmpty());
                        successCount++;
                    } else {
                        log.error("❌ No translations generated for news {} (detectedLang: {})", news.getId(), detectedLang);
                        errorCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("❌ Failed to translate news {}: {}", news.getId(), e.getMessage(), e);
                    errorCount++;
                }
            }
            
            long cost = System.currentTimeMillis() - start;
            
            resp.put("success", true);
            resp.put("message", "News translation completed");
            resp.put("totalProcessed", allNews.size());
            resp.put("successCount", successCount);
            resp.put("skipCount", skipCount);
            resp.put("errorCount", errorCount);
            resp.put("limit", limit);
            resp.put("costMs", cost);
            
            log.info("✅ News translation completed: {} processed, {} success, {} skipped, {} errors, cost {} ms",
                    allNews.size(), successCount, skipCount, errorCount, cost);
            
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            log.error("News translation failed: {}", e.getMessage(), e);
            resp.put("success", false);
            resp.put("message", "News translation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(resp);
        }
    }
    
    /**
     * Check if news item already has translations
     */
    private boolean hasNewsTranslations(News news) {
        return (news.getTitleZh() != null && !news.getTitleZh().isEmpty()) ||
               (news.getTitleEn() != null && !news.getTitleEn().isEmpty()) ||
               (news.getSummaryZh() != null && !news.getSummaryZh().isEmpty()) ||
               (news.getSummaryEn() != null && !news.getSummaryEn().isEmpty());
    }

}


