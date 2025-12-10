package com.globalbuddy.scheduler;

import com.globalbuddy.model.News;
import com.globalbuddy.repository.NewsRepository;
import com.globalbuddy.service.AiSummaryService;
import com.globalbuddy.service.LanguageDetectionService;
import com.globalbuddy.service.NewsCrawlerService;
import com.globalbuddy.service.NewsToPostService;
import com.globalbuddy.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * News Scheduler
 * Scheduled task to crawl news daily and generate AI summaries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsCrawlerService newsCrawlerService;
    private final AiSummaryService aiSummaryService;
    private final NewsRepository newsRepository;
    private final NewsToPostService newsToPostService;
    private final LanguageDetectionService languageDetectionService;
    private final TranslationService translationService;

    /**
     * Scheduled task: Execute every day at 8:00 AM
     * Cron expression: second minute hour day month week
     * "0 0 8 * * ?" means execute at 8:00:00 every day
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledCrawlAndSummarize() {
        log.info("========== Starting scheduled news crawling task ==========");
        
        try {
            // Step 1: Fetch latest news list (including all Thai news websites and Chiang Mai University)
            log.info("Step 1: Starting to crawl news list...");
            List<News> newsList = new ArrayList<>();
            
            // Crawl all configured Thai news websites (using RSS)
            log.info("Fetching news from all Thai news website RSS feeds...");
            List<News> thaiNews = newsCrawlerService.crawlAllThaiNews();
            newsList.addAll(thaiNews);
            
            // Crawl Chiang Mai University news
            log.info("Crawling Chiang Mai University news...");
            List<News> cmuNews = newsCrawlerService.crawlChiangMaiUniversity();
            newsList.addAll(cmuNews);
            
            if (newsList == null || newsList.isEmpty()) {
                log.warn("No news crawled, task ended. Possible reasons: 1) Website structure changed 2) Network issue 3) Selector mismatch");
                return;
            }
            
            log.info("Successfully crawled {} news items, starting processing...", newsList.size());

            // Step 2: Iterate through news, generate AI summaries and save
            int successCount = 0;
            int skipCount = 0;
            int errorCount = 0;

            for (News news : newsList) {
                try {
                    // Check if already exists (deduplicate by original URL)
                    if (news.getOriginalUrl() != null && 
                        newsRepository.findByOriginalUrl(news.getOriginalUrl()).isPresent()) {
                        log.debug("News already exists, skipping: {}", news.getTitle());
                        skipCount++;
                        continue;
                    }

                    // Fetch full news content (if needed)
                    if (news.getOriginalContent() == null || news.getOriginalContent().isEmpty()) {
                        if (news.getOriginalUrl() != null) {
                            log.info("Starting to crawl news content: {}", news.getTitle());
                            String content = newsCrawlerService.crawlNewsContent(news.getOriginalUrl());
                            if (content != null && !content.isEmpty()) {
                                news.setOriginalContent(content);
                                log.info("Successfully crawled content, length: {} characters", content.length());
                            } else {
                                log.warn("Failed to crawl content: {}", news.getTitle());
                            }
                        }
                    }

                    // Generate AI summary using AI service (optional, failure doesn't affect saving)
                    log.info("Step 2: Generating AI summary for news: {}", news.getTitle());
                    
                    // Save original summary from RSS before AI processing
                    String originalSummary = news.getSummary();
                    
                    // Prefer using full content for summary, otherwise use title (at least generate a simple summary)
                    String contentForSummary = news.getOriginalContent();
                    if (contentForSummary == null || contentForSummary.isEmpty()) {
                        contentForSummary = originalSummary; // Use RSS summary as input
                    }
                    
                    // If still no content, at least use title to generate a simple summary
                    if (contentForSummary == null || contentForSummary.isEmpty()) {
                        contentForSummary = news.getTitle();
                        log.info("Content is empty, using title to generate summary: {}", news.getTitle());
                    }
                    
                    // Limit content length for AI processing (to avoid API errors with very long content)
                    if (contentForSummary != null && contentForSummary.length() > 2000) {
                        log.info("Content too long ({} chars), truncating to 2000 chars for AI summary", contentForSummary.length());
                        contentForSummary = contentForSummary.substring(0, 2000) + "...";
                    }
                    
                    if (contentForSummary != null && !contentForSummary.isEmpty()) {
                        try {
                            log.info("Calling AI service to generate summary, content length: {}", contentForSummary.length());
                            String aiSummary = aiSummaryService.generateSummary(contentForSummary);
                            if (aiSummary != null && !aiSummary.isEmpty()) {
                                news.setSummary(aiSummary);
                                log.info("✅ AI summary generated successfully: {} - {}", news.getTitle(), aiSummary.length() > 50 ? aiSummary.substring(0, 50) + "..." : aiSummary);
                            } else {
                                log.warn("⚠️ AI returned empty summary: {}", news.getTitle());
                                // Restore original summary if AI failed
                                if (originalSummary != null && !originalSummary.isEmpty()) {
                                    news.setSummary(originalSummary);
                                    log.info("Restored original RSS summary: {}", news.getTitle());
                                }
                            }
                        } catch (Exception e) {
                            log.error("⚠️ Failed to generate AI summary: {} - {}", news.getTitle(), e.getMessage(), e);
                            // If AI summary generation fails, restore original summary (if any)
                            if (originalSummary != null && !originalSummary.isEmpty()) {
                                news.setSummary(originalSummary);
                                log.info("Restored original RSS summary after AI failure: {}", news.getTitle());
                            }
                        }
                    } else {
                        log.warn("⚠️ News content is empty, skipping AI summary generation: {}", news.getTitle());
                    }
                    
                    // If no summary at all, at least keep title as summary
                    if (news.getSummary() == null || news.getSummary().isEmpty()) {
                        news.setSummary(news.getTitle());
                        log.info("Using title as summary: {}", news.getTitle());
                    }

                    // Step 2.5: Translate title and summary to Chinese and English (自动翻译，与帖子一样)
                    try {
                        String titleToTranslate = news.getTitle() != null ? news.getTitle() : "";
                        String summaryToTranslate = news.getSummary() != null ? news.getSummary() : "";
                        
                        if (!titleToTranslate.isEmpty() || !summaryToTranslate.isEmpty()) {
                            log.info("🌐 Starting automatic translation for news: {}", titleToTranslate.substring(0, Math.min(50, titleToTranslate.length())));
                            
                            // Detect language
                            String combinedText = titleToTranslate + " " + summaryToTranslate;
                            String detectedLang = languageDetectionService.detectLanguage(combinedText);
                            log.info("🔍 Detected language for news: {} (title: {})", detectedLang, titleToTranslate.substring(0, Math.min(50, titleToTranslate.length())));
                            
                            // Translate title and summary
                            TranslationService.TranslationResult translationResult = 
                                translationService.translateContent(titleToTranslate, summaryToTranslate, detectedLang);
                            
                            boolean hasTranslation = false;
                            
                            // Set Chinese translations
                            if (translationResult.getTitleZh() != null && !translationResult.getTitleZh().isEmpty()) {
                                // 验证翻译结果确实包含中文字符，而不是泰语或其他语言
                                if (languageDetectionService.containsChinese(translationResult.getTitleZh()) && 
                                    !languageDetectionService.containsThai(translationResult.getTitleZh())) {
                                    news.setTitleZh(translationResult.getTitleZh());
                                    log.info("✅ Chinese title translation: {} -> {}", 
                                            titleToTranslate.substring(0, Math.min(30, titleToTranslate.length())),
                                            translationResult.getTitleZh().substring(0, Math.min(30, translationResult.getTitleZh().length())));
                                    hasTranslation = true;
                                } else {
                                    log.warn("⚠️ Translation result is not Chinese (contains Thai or no Chinese): {} for news: {}", 
                                            translationResult.getTitleZh().substring(0, Math.min(50, translationResult.getTitleZh().length())), news.getTitle());
                                }
                            }
                            
                            // 如果 titleZh 仍然为空，且原始语言是中文，且标题确实包含中文
                            if (news.getTitleZh() == null || news.getTitleZh().isEmpty()) {
                                if ("zh".equals(detectedLang) && languageDetectionService.containsChinese(titleToTranslate) && 
                                    !languageDetectionService.containsThai(titleToTranslate)) {
                                    news.setTitleZh(titleToTranslate);
                                    log.info("✅ News title is already in Chinese, using original");
                                    hasTranslation = true;
                                } else {
                                    log.warn("⚠️ Chinese title translation failed or empty for news: {} (detectedLang: {})", news.getTitle(), detectedLang);
                                }
                            }
                            
                            if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                                news.setSummaryZh(translationResult.getBodyZh());
                                log.info("✅ Chinese summary translation completed (length: {})", translationResult.getBodyZh().length());
                                hasTranslation = true;
                            } else if ("zh".equals(detectedLang)) {
                                news.setSummaryZh(summaryToTranslate);
                                log.info("✅ News summary is already in Chinese, using original");
                                hasTranslation = true;
                            } else {
                                log.warn("⚠️ Chinese summary translation failed or empty for news: {}", news.getTitle());
                            }
                            
                            // Set English translations
                            if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                                news.setTitleEn(translationResult.getTitleEn());
                                log.info("✅ English title translation: {} -> {}", 
                                        titleToTranslate.substring(0, Math.min(30, titleToTranslate.length())),
                                        translationResult.getTitleEn().substring(0, Math.min(30, translationResult.getTitleEn().length())));
                                hasTranslation = true;
                            } else if ("en".equals(detectedLang)) {
                                news.setTitleEn(titleToTranslate);
                                log.info("✅ News title is already in English, using original");
                                hasTranslation = true;
                            } else {
                                log.warn("⚠️ English title translation failed or empty for news: {}", news.getTitle());
                            }
                            
                            if (translationResult.getBodyEn() != null && !translationResult.getBodyEn().isEmpty()) {
                                news.setSummaryEn(translationResult.getBodyEn());
                                log.info("✅ English summary translation completed (length: {})", translationResult.getBodyEn().length());
                                hasTranslation = true;
                            } else if ("en".equals(detectedLang)) {
                                news.setSummaryEn(summaryToTranslate);
                                log.info("✅ News summary is already in English, using original");
                                hasTranslation = true;
                            } else {
                                log.warn("⚠️ English summary translation failed or empty for news: {}", news.getTitle());
                            }
                            
                            if (hasTranslation) {
                                log.info("✅ Automatic translation completed for news: {} (has translations: titleZh={}, titleEn={}, summaryZh={}, summaryEn={})", 
                                        news.getTitle(),
                                        news.getTitleZh() != null && !news.getTitleZh().isEmpty(),
                                        news.getTitleEn() != null && !news.getTitleEn().isEmpty(),
                                        news.getSummaryZh() != null && !news.getSummaryZh().isEmpty(),
                                        news.getSummaryEn() != null && !news.getSummaryEn().isEmpty());
                            } else {
                                log.error("❌ No translations generated for news: {} (detected lang: {})", news.getTitle(), detectedLang);
                            }
                        } else {
                            log.warn("⚠️ News has no content to translate (title and summary are both empty): {}", news.getTitle());
                        }
                    } catch (Exception e) {
                        log.error("❌ Failed to translate news: {} - {}", news.getTitle(), e.getMessage(), e);
                        // Continue saving even if translation fails (新闻仍会保存，但没有翻译)
                        log.warn("⚠️ News will be saved without translations due to translation failure");
                    }

                    // Set crawl time
                    if (news.getCreateTime() == null) {
                        news.setCreateTime(new Date());
                    }
                    
                    // Set publish date (if not set)
                    if (news.getPublishDate() == null) {
                        news.setPublishDate(new Date());
                    }

                    // Step 3: Save to database (save even if no AI summary or translation)
                    log.info("Step 3: Saving news to database: {} (source: {})", news.getTitle(), news.getSource());
                    try {
                        News savedNews = newsRepository.save(news);
                        successCount++;
                        log.info("✅ News saved successfully - ID: {}, Title: {}, Source: {}, Summary: {}", 
                                savedNews.getId(), 
                                savedNews.getTitle(), 
                                savedNews.getSource(),
                                savedNews.getSummary() != null ? "Yes" : "No");
                    } catch (Exception saveEx) {
                        log.error("❌ Failed to save news to database: {} - {}", news.getTitle(), saveEx.getMessage(), saveEx);
                        errorCount++;
                    }

                } catch (Exception e) {
                    log.error("Exception occurred while processing news: {} - {}", news.getTitle(), e.getMessage(), e);
                    errorCount++;
                }
            }

            log.info("========== Scheduled task completed ==========");
            log.info("Statistics - Success: {}, Skipped: {}, Failed: {}", successCount, skipCount, errorCount);
            
            // Step 4: Automatically convert news to posts and push to homepage
            log.info("Step 4: Starting to convert news to posts...");
            try {
                NewsToPostService.ConversionResult conversionResult = newsToPostService.convertNewsToPosts(20);
                log.info("News to posts conversion completed: {}", conversionResult);
            } catch (Exception e) {
                log.error("Failed to automatically convert news to posts: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Scheduled task execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manually trigger task (for testing)
     * Can be called via API or other methods
     */
    public void manualTrigger() {
        log.info("Manually triggering news crawling task");
        scheduledCrawlAndSummarize();
    }
}

