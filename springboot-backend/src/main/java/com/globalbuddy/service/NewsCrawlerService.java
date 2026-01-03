package com.globalbuddy.service;

import com.globalbuddy.model.News;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * News Crawler Service
 * Used to crawl news information from target websites
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCrawlerService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 30000; // 30 seconds timeout (increased timeout)
    private static final String BANGKOK_POST_BASE_URL = "https://www.bangkokpost.com";
    
    private final RssFeedService rssFeedService;
    private final GoogleTrendsRssService googleTrendsRssService;
    private final NewsApiService newsApiService;

    /**
     * Crawl all configured Thai news websites (using RSS)
     * 
     * @return List of news objects
     */
    public List<News> crawlAllThaiNews() {
        List<News> allNews = new ArrayList<>();
        
        try {
            log.info("Starting to fetch news from Google News RSS (Thailand)...");
            
            // ========== 终极解决方案：使用 Google News RSS ==========
            // 优势：
            // 1. 极度稳定：Google 的服务器几乎不会挂 (504)
            // 2. 格式统一：标准的 Atom/RSS 格式，不会有乱七八糟的 XML 错误
            // 3. 内容全：包含了 Matichon, Thairath, Khaosod 等所有主要泰国媒体的内容
            // 4. 一个 URL 搞定：不需要循环遍历多个容易挂掉的 URL
            
            // Google News RSS URL for Thailand (泰语版)
            // ceid=TH:th: 泰国地区，泰语版
            // gl=TH: 地理位置泰国
            String googleNewsRssUrl = "https://news.google.com/rss?ceid=TH:th&hl=th&gl=TH";
            
            // Step 1: Fetch trending keywords from Google Trends RSS (定方向)
            // Note: Google Trends RSS may be unavailable, so we handle failures gracefully
            log.info("Step 1: Fetching trending keywords from Google Trends RSS (Thailand)...");
            List<String> trendingKeywords = new ArrayList<>();
            try {
                trendingKeywords = googleTrendsRssService.fetchThailandTrendingKeywords(20);
                if (!trendingKeywords.isEmpty()) {
                    log.info("✅ Fetched {} trending keywords from Google Trends: {}", 
                            trendingKeywords.size(), trendingKeywords);
                } else {
                    log.warn("⚠️ No trending keywords fetched from Google Trends (service may be unavailable). Continuing without trending keywords.");
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch trending keywords from Google Trends: {}. Continuing without trending keywords.", e.getMessage());
                trendingKeywords = new ArrayList<>(); // Ensure it's not null
            }
            
            // Step 2: Fetch news from Google News RSS (取内容)
            // 这是我们的主要新闻源，非常稳定可靠
            log.info("Step 2: Fetching news from Google News RSS (Thailand)...");
            try {
                // 从 Google News RSS 获取最多 100 条新闻（Google News 会返回很多新闻）
                List<News> googleNews = rssFeedService.fetchNewsFromRss(googleNewsRssUrl, "Google News (Thailand)", 100);
                if (!googleNews.isEmpty()) {
                    log.info("✅ Successfully fetched {} news items from Google News RSS", googleNews.size());
                    allNews.addAll(googleNews);
                } else {
                    log.warn("⚠️ No news items fetched from Google News RSS");
                }
            } catch (Exception e) {
                log.error("❌ Failed to fetch news from Google News RSS: {}", e.getMessage(), e);
            }
            
            // Step 2.3: Fetch news from Google News RSS with international student keywords (专门针对留学生)
            // 这个 RSS 链接使用了搜索功能，包含以下关键词：
            // - นักศึกษาต่างชาติ (外国大学生)
            // - นักเรียนต่างชาติ (外国学生)
            // - ทุนการศึกษา (奖学金)
            // - นักศึกษาจีน (中国留学生)
            // when:30d - 只看最近 30 天的新闻（保证时效性）
            log.info("Step 2.3: Fetching news from Google News RSS (International Students in Thailand)...");
            try {
                String internationalStudentRssUrl = "https://news.google.com/rss/search?q=(%E0%B8%99%E0%B8%B1%E0%B8%81%E0%B8%A8%E0%B8%B6%E0%B8%81%E0%B8%A9%E0%B8%B2%E0%B8%95%E0%B9%88%E0%B8%B2%E0%B8%87%E0%B8%8A%E0%B8%B2%E0%B8%95%E0%B8%B4+OR+%E0%B8%99%E0%B8%B1%E0%B8%81%E0%B9%80%E0%B8%A3%E0%B8%B5%E0%B8%A2%E0%B8%99%E0%B8%95%E0%B9%88%E0%B8%B2%E0%B8%87%E0%B8%8A%E0%B8%B2%E0%B8%95%E0%B8%B4+OR+%E0%B8%97%E0%B8%B8%E0%B8%99%E0%B8%81%E0%B8%B2%E0%B8%A3%E0%B8%A8%E0%B8%B6%E0%B8%81%E0%B8%A9%E0%B8%B2+OR+%E0%B8%99%E0%B8%B1%E0%B8%81%E0%B8%A8%E0%B8%B6%E0%B8%81%E0%B8%A9%E0%B8%B2%E0%B8%88%E0%B8%B5%E0%B8%99)+when:30d&ceid=TH:th&hl=th&gl=TH";
                List<News> studentNews = rssFeedService.fetchNewsFromRss(internationalStudentRssUrl, "Google News (International Students)", 50);
                if (!studentNews.isEmpty()) {
                    log.info("✅ Successfully fetched {} news items from Google News RSS (International Students)", studentNews.size());
                    allNews.addAll(studentNews);
                    log.info("Total news items after adding International Students news: {}", allNews.size());
                } else {
                    log.warn("⚠️ No news items fetched from Google News RSS (International Students)");
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch news from Google News RSS (International Students): {}. Continuing without this source.", e.getMessage());
            }
            
            // Step 2.5: Fetch news from NewsAPI.org (Thailand) as supplement (可选补充)
            log.info("Step 2.5: Fetching news from NewsAPI.org (Thailand) as supplement...");
            try {
                List<News> newsApiNews = newsApiService.fetchThailandHeadlines(30); // Fetch up to 30 articles
                if (!newsApiNews.isEmpty()) {
                    log.info("✅ Fetched {} news items from NewsAPI.org (Thailand)", newsApiNews.size());
                    allNews.addAll(newsApiNews);
                    log.info("Total news items after adding NewsAPI.org: {}", allNews.size());
                } else {
                    log.warn("⚠️ No news items fetched from NewsAPI.org (service may be unavailable or rate limited).");
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to fetch news from NewsAPI.org: {}. Continuing without NewsAPI.org news.", e.getMessage());
            }
            
            // Step 3: Prioritize news based on trending keywords (混合策略)
            // Only prioritize if we have both trending keywords and news items
            if (!trendingKeywords.isEmpty() && !allNews.isEmpty()) {
                log.info("Step 3: Prioritizing news based on {} trending keywords...", trendingKeywords.size());
                allNews = googleTrendsRssService.prioritizeNewsByTrends(allNews, trendingKeywords);
                log.info("News prioritized: {} items total (trending items appear first)", allNews.size());
            } else if (trendingKeywords.isEmpty()) {
                log.info("Step 3: Skipping prioritization (no trending keywords available). News will be returned in original order.");
            }
            
            // Step 4: Statistics and filtering by source
            log.info("Step 4: Analyzing news sources...");
            Map<String, Long> sourceStatistics = getSourceStatistics(allNews);
            logSourceStatistics(sourceStatistics);
            
            // Apply source filtering (if needed)
            // You can configure which sources to include/exclude here
            List<String> allowedSources = getAllowedSources(); // Empty list means all sources are allowed
            List<String> excludedSources = getExcludedSources(); // Sources to exclude
            
            if (!allowedSources.isEmpty() || !excludedSources.isEmpty()) {
                int beforeFilter = allNews.size();
                allNews = filterNewsBySource(allNews, allowedSources, excludedSources);
                int afterFilter = allNews.size();
                if (beforeFilter != afterFilter) {
                    log.info("✅ Filtered news by source: {} -> {} items (removed {} items)", 
                            beforeFilter, afterFilter, beforeFilter - afterFilter);
                }
            }
            
            log.info("✅ Total news items fetched: {}", allNews.size());
            
        } catch (Exception e) {
            log.error("Failed to fetch news from Thai news sources: {}", e.getMessage(), e);
        }
        
        return allNews;
    }

    /**
     * Get all configured Thai news website RSS feed configurations
     * Used for testing and debugging
     * 
     * @return List of RSS feed configurations
     */
    public List<RssFeedService.RssFeedConfig> getAllThaiRssFeedConfigs() {
        List<RssFeedService.RssFeedConfig> rssFeeds = new ArrayList<>();
        
        // 使用 Google News RSS 作为主要新闻源（终极解决方案）
        // 优势：极度稳定、格式统一、内容全面（包含所有主要泰国媒体）
            rssFeeds.add(new RssFeedService.RssFeedConfig(
            "https://news.google.com/rss?ceid=TH:th&hl=th&gl=TH",
            "Google News (Thailand)"
            ));
            
        // 专门针对泰国留学生的 Google News RSS（使用搜索功能）
        // 关键词：外国大学生、外国学生、奖学金、中国留学生
        // 时间范围：最近 30 天
            rssFeeds.add(new RssFeedService.RssFeedConfig(
            "https://news.google.com/rss/search?q=(%E0%B8%99%E0%B8%B1%E0%B8%81%E0%B8%A8%E0%B8%B6%E0%B8%81%E0%B8%A9%E0%B8%B2%E0%B8%95%E0%B9%88%E0%B8%B2%E0%B8%87%E0%B8%8A%E0%B8%B2%E0%B8%95%E0%B8%B4+OR+%E0%B8%99%E0%B8%B1%E0%B8%81%E0%B9%80%E0%B8%A3%E0%B8%B5%E0%B8%A2%E0%B8%99%E0%B8%95%E0%B9%88%E0%B8%B2%E0%B8%87%E0%B8%8A%E0%B8%B2%E0%B8%95%E0%B8%B4+OR+%E0%B8%97%E0%B8%B8%E0%B8%99%E0%B8%81%E0%B8%B2%E0%B8%A3%E0%B8%A8%E0%B8%B6%E0%B8%81%E0%B8%A9%E0%B8%B2+OR+%E0%B8%99%E0%B8%B1%E0%B8%81%E0%B8%A8%E0%B8%B6%E0%B8%81%E0%B8%A9%E0%B8%B2%E0%B8%88%E0%B8%B5%E0%B8%99)+when:30d&ceid=TH:th&hl=th&gl=TH",
            "Google News (International Students)"
        ));
        
        // 注意：之前的各个媒体网站RSS源已全部移除，因为它们无法正常工作
        // （XML解析失败、404错误、504超时等）
        // Google News RSS 已经包含了这些媒体的内容，无需单独配置
        
        return rssFeeds;
    }

    /**
     * Get all configured Chiang Mai University related RSS feed configurations
     * Used for testing and debugging
     * 
     * @return List of RSS feed configurations
     */
    public List<RssFeedService.RssFeedConfig> getChiangMaiUniversityRssFeedConfigs() {
        // 清迈大学相关的RSS源大多不可访问，已移除
        // 如需添加可访问的源，请先通过测试确认其可用性
        List<RssFeedService.RssFeedConfig> rssFeeds = new ArrayList<>();
        return rssFeeds;
    }

    /**
     * Get source statistics from news list
     * Counts how many news items come from each source
     * 
     * @param newsList List of news items
     * @return Map of source name to count
     */
    public Map<String, Long> getSourceStatistics(List<News> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return new HashMap<>();
        }
        
        return newsList.stream()
                .filter(news -> news.getSource() != null && !news.getSource().isEmpty())
                .collect(Collectors.groupingBy(
                    News::getSource,
                    Collectors.counting()
                ));
    }

    /**
     * Log source statistics in a readable format
     * 
     * @param sourceStatistics Map of source name to count
     */
    private void logSourceStatistics(Map<String, Long> sourceStatistics) {
        if (sourceStatistics.isEmpty()) {
            log.info("📊 Source statistics: No sources found");
            return;
        }
        
        log.info("📊 Source statistics ({} unique sources):", sourceStatistics.size());
        
        // Sort by count (descending) for better readability
        sourceStatistics.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    log.info("  - {}: {} articles", entry.getKey(), entry.getValue());
                });
        
        // Calculate total
        long total = sourceStatistics.values().stream().mapToLong(Long::longValue).sum();
        log.info("  Total: {} articles", total);
    }

    /**
     * Get list of allowed sources (whitelist)
     * If empty, all sources are allowed (unless excluded)
     * 
     * @return List of allowed source names (can be partial matches)
     */
    private List<String> getAllowedSources() {
        // Configure allowed sources here
        // Examples:
        // return Arrays.asList("Bangkok Post", "The Nation", "Matichon");
        // Empty list means all sources are allowed
        return new ArrayList<>();
    }

    /**
     * Get list of excluded sources (blacklist)
     * Sources matching these patterns will be filtered out
     * 
     * @return List of excluded source patterns (can be partial matches)
     */
    private List<String> getExcludedSources() {
        // Configure excluded sources here
        // Examples:
        // return Arrays.asList("Spam Source", "Low Quality");
        // Empty list means no sources are excluded
        return new ArrayList<>();
    }

    /**
     * Filter news list by source (whitelist and blacklist)
     * 
     * @param newsList Original news list
     * @param allowedSources List of allowed source patterns (empty = allow all)
     * @param excludedSources List of excluded source patterns (empty = exclude none)
     * @return Filtered news list
     */
    public List<News> filterNewsBySource(List<News> newsList, List<String> allowedSources, List<String> excludedSources) {
        if (newsList == null || newsList.isEmpty()) {
            return newsList;
        }
        
        return newsList.stream()
                .filter(news -> {
                    String source = news.getSource();
                    if (source == null || source.isEmpty()) {
                        // If source is empty, only include if no whitelist is set
                        return allowedSources.isEmpty();
                    }
                    
                    // Check blacklist first (excluded sources)
                    for (String excluded : excludedSources) {
                        if (source.contains(excluded)) {
                            log.debug("Excluding news from source: {} (matched exclusion pattern: {})", source, excluded);
                            return false;
                        }
                    }
                    
                    // Check whitelist (allowed sources)
                    if (allowedSources.isEmpty()) {
                        // No whitelist = all sources allowed (unless excluded)
                        return true;
                    }
                    
                    // Check if source matches any allowed pattern
                    for (String allowed : allowedSources) {
                        if (source.contains(allowed)) {
                            return true;
                        }
                    }
                    
                    // Source not in whitelist
                    log.debug("Excluding news from source: {} (not in whitelist)", source);
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * Crawl Bangkok Post website news list
     * Prefer RSS, fallback to HTML crawling if RSS is unavailable
     *
     * @return List of news objects
     */
    public List<News> crawlBangkokPost() {
        // Try RSS first
        log.info("Attempting to fetch news from Bangkok Post RSS feed...");
        List<News> rssNews = rssFeedService.fetchNewsFromRss(
            "https://www.bangkokpost.com/rss/data/thailand.xml",
            "Bangkok Post",
            20
        );
        
        if (!rssNews.isEmpty()) {
            log.info("Successfully fetched {} Bangkok Post news items from RSS feed", rssNews.size());
            return rssNews;
        }
        
        // Fallback to HTML crawling if RSS fails
        log.warn("RSS feed returned no news, falling back to HTML crawling...");
        return crawlBangkokPostFromHtml();
    }

    /**
     * Crawl Bangkok Post website news list from HTML (fallback method)
     *
     * Try to only crawl specific news articles based on URL rules, avoid crawling category pages.
     *
     * @return List of news objects
     */
    private List<News> crawlBangkokPostFromHtml() {
        String url = "https://www.bangkokpost.com/thailand/general";
        List<News> newsList = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        try {
            log.info("Starting to crawl Bangkok Post: {}", url);

            // Use Jsoup to connect and parse webpage, set User-Agent and timeout
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();

            // Try multiple selectors to find news links
            Elements linkElements = doc.select("a[href*='/thailand/']");
            
            // If not found, try more generic selectors
            if (linkElements.isEmpty()) {
                linkElements = doc.select("article a[href], .story a[href], .article-item a[href]");
            }

            log.info("Found {} candidate links on list page", linkElements.size());

            int filteredCount = 0;
            for (Element linkEl : linkElements) {
                String href = null;
                try {
                    href = linkEl.attr("href");
                    if (href == null || href.isEmpty()) {
                        continue;
                    }

                    // Filter out pure category pages, e.g. /thailand/general /thailand/politics
                    // Rule 1: URL must contain numbers to be considered a specific news article
                    // Rule 2: Or URL contains common news path patterns
                    boolean isArticleUrl = href.matches(".*\\d.*") || 
                                          href.matches(".*/thailand/.*/.*") ||
                                          href.contains("/news/") ||
                                          href.contains("/article/");
                    
                    if (!isArticleUrl) {
                        filteredCount++;
                        continue;
                    }

                    // Normalize to absolute URL
                    String absUrl = linkEl.absUrl("href");
                    if (absUrl == null || absUrl.isEmpty()) {
                        if (href.startsWith("http")) {
                            absUrl = href;
                        } else if (href.startsWith("/")) {
                            absUrl = BANGKOK_POST_BASE_URL + href;
                        } else {
                            absUrl = BANGKOK_POST_BASE_URL + "/" + href;
                        }
                    }

                    // Deduplication
                    if (!seenUrls.add(absUrl)) {
                        continue;
                    }

                    // Extract title - try multiple methods
                    String title = linkEl.text() != null ? linkEl.text().trim() : "";
                    
                    // If link text is empty, try to get from parent or sibling elements
                    if (title.isEmpty()) {
                        // Method 1: Find title from parent element
                        Element parent = linkEl.parent();
                        if (parent != null) {
                            // First try to find h1-h4 title tags
                            Element titleEl = parent.selectFirst("h1, h2, h3, h4");
                            if (titleEl != null) {
                                title = titleEl.text().trim();
                            }
                            // If still empty, try to find other title-related classes
                            if (title.isEmpty()) {
                                titleEl = parent.selectFirst(".title, .headline, .story-title, [class*='title']");
                                if (titleEl != null) {
                                    title = titleEl.text().trim();
                                }
                            }
                            // If still empty, try to extract from parent element text (remove link text)
                            if (title.isEmpty()) {
                                String parentText = parent.text().trim();
                                if (!parentText.isEmpty() && parentText.length() > 5) {
                                    title = parentText;
                                }
                            }
                        }
                        
                        // Method 2: Find from nearest article or story container
                        if (title.isEmpty()) {
                            Element container = linkEl.closest("article, .article, .story, .story-card, .story-item");
                            if (container != null) {
                                Element titleEl = container.selectFirst("h1, h2, h3, h4, .title, .headline, .story-title");
                                if (titleEl != null) {
                                    title = titleEl.text().trim();
                                }
                            }
                        }
                        
                        // Method 3: Extract title from URL (as last resort)
                        if (title.isEmpty() && absUrl != null) {
                            // Try to extract title from URL path
                            String[] parts = absUrl.split("/");
                            if (parts.length > 0) {
                                String lastPart = parts[parts.length - 1];
                                if (lastPart != null && !lastPart.isEmpty() && !lastPart.matches("\\d+")) {
                                    // Replace hyphens in URL with spaces as title
                                    title = lastPart.replaceAll("-", " ").trim();
                                }
                            }
                        }
                    }
                    
                    if (title.isEmpty() || title.length() < 3) {
                        log.debug("Skipping link without title or title too short: {}", absUrl);
                        continue;
                    }

                    // Try to find a short summary text nearby
                    String summary = null;
                    Element card = linkEl.closest("article, .article, .story-card, .story-item, li, .media, .list");
                    if (card != null) {
                        Element summaryEl = card.selectFirst(".summary, .lead, .description, .snippet, p");
                        if (summaryEl != null) {
                            summary = summaryEl.text().trim();
                        }
                    }

                    News news = News.builder()
                            .title(title)
                            .originalUrl(absUrl)
                            .source("Bangkok Post")
                            .summary(summary)
                            .createTime(new Date())
                            .publishDate(new Date())
                            .build();

                    newsList.add(news);
                    log.debug("Added news: {} -> {}", title, absUrl);
                } catch (Exception ex) {
                    // Use href instead of absUrl in catch, because absUrl may not be defined yet
                    String errorUrl = href != null ? href : "Unknown link";
                    log.warn("Error parsing single news link: {} - {}", errorUrl, ex.getMessage());
                }
            }

            log.info("Successfully crawled {} suspected news article links (filtered out {} category page links)", newsList.size(), filteredCount);

        } catch (IOException e) {
            log.error("IO exception occurred while crawling Bangkok Post: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unknown exception occurred while crawling Bangkok Post: {}", e.getMessage(), e);
        }

        return newsList;
    }

    /**
     * 爬取清迈大学相关新闻（优先使用 RSS，如果 RSS 不可用则使用 HTML 爬取）
     * 
     * @return 新闻对象列表
     */
    public List<News> crawlChiangMaiUniversity() {
        List<News> newsList = new ArrayList<>();
        
        try {
            // 优先尝试使用 RSS 订阅源
            log.info("开始从清迈大学相关 RSS 订阅源获取新闻...");
            List<News> rssNews = crawlChiangMaiUniversityFromRss();
            if (!rssNews.isEmpty()) {
                log.info("从 RSS 订阅源成功获取 {} 条新闻", rssNews.size());
                newsList.addAll(rssNews);
            } else {
                log.warn("RSS 订阅源未获取到新闻");
                // 注意：以下 HTML 爬取源已移除，因为它们无法正常工作：
                // 1. 清迈大学校友会 (CMUAC) - 无法找到有效链接
                // 2. 中国驻清迈总领馆 - SSL 证书问题
                // 3. 泰国电子签证 - 无法找到有效链接
            }
            
            log.info("成功获取 {} 条清迈大学相关新闻", newsList.size());
            
        } catch (Exception e) {
            log.error("爬取清迈大学相关新闻时发生异常: {}", e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * 从 RSS 订阅源获取清迈大学相关新闻
     * 
     * @return 新闻对象列表
     */
    private List<News> crawlChiangMaiUniversityFromRss() {
        List<News> newsList = new ArrayList<>();
        
        try {
            // 配置 RSS 订阅源列表
            List<RssFeedService.RssFeedConfig> rssFeeds = new ArrayList<>();
            
            // 注意：以下新闻源已移除，因为它们无法正常工作：
            // 1. 中国驻清迈总领馆 - RSS全部失败，HTML爬取也失败（SSL证书问题）
            // 2. 清迈大学校友会 (CMUAC) - RSS全部404或超时，HTML爬取也没有找到链接
            // 3. 泰国电子签证 - RSS解析失败，HTML爬取也没有找到链接
            
            // 从所有 RSS 订阅源获取新闻
            if (!rssFeeds.isEmpty()) {
                newsList = rssFeedService.fetchNewsFromMultipleRss(rssFeeds, 20);
                log.info("从 RSS 订阅源获取到 {} 条新闻", newsList.size());
            } else {
                log.info("未配置 RSS 订阅源，跳过 RSS 获取");
            }
            
        } catch (Exception e) {
            log.error("从 RSS 订阅源获取清迈大学新闻失败: {}", e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * 爬取清迈大学校友会网站 (cmuac.com)
     * 
     * @deprecated 此方法已废弃 - 该网站无法获取有效新闻（RSS返回404，HTML爬取无有效链接）
     * @return 新闻对象列表
     */
    @Deprecated
    private List<News> crawlCmuac() {
        List<News> newsList = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        
        try {
            String url = "https://cmuac.com/";
            log.info("开始爬取清迈大学校友会网站: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();
            
            // 尝试多种选择器来找到新闻链接
            Elements linkElements = doc.select("a[href]");
            
            log.info("在清迈大学校友会网站发现 {} 个链接", linkElements.size());
            
            int foundCount = 0;
            for (Element linkEl : linkElements) {
                String href = null;
                try {
                    href = linkEl.attr("href");
                    if (href == null || href.isEmpty()) {
                        continue;
                    }
                    
                    // 规范化为绝对链接
                    String absUrl = linkEl.absUrl("href");
                    if (absUrl == null || absUrl.isEmpty()) {
                        if (href.startsWith("http")) {
                            absUrl = href;
                        } else if (href.startsWith("/")) {
                            absUrl = "https://cmuac.com" + href;
                        } else {
                            absUrl = "https://cmuac.com/" + href;
                        }
                    }
                    
                    // 只处理 cmuac.com 域名的链接
                    if (!absUrl.contains("cmuac.com")) {
                        continue;
                    }
                    
                    // 去重
                    if (!seenUrls.add(absUrl)) {
                        continue;
                    }
                    
                    // 过滤掉非新闻链接（排除首页、分类页等）
                    String lowerUrl = absUrl.toLowerCase();
                    if (lowerUrl.endsWith("/") || 
                        lowerUrl.contains("/category/") || 
                        lowerUrl.contains("/tag/") ||
                        lowerUrl.contains("/author/") ||
                        lowerUrl.contains("/page/") ||
                        lowerUrl.contains("/search") ||
                        lowerUrl.equals("https://cmuac.com/") ||
                        lowerUrl.equals("https://cmuac.com")) {
                        continue;
                    }
                    
                    // 提取标题
                    String title = linkEl.text() != null ? linkEl.text().trim() : "";
                    
                    // 如果链接文本为空或太短，尝试从父元素获取
                    if (title.isEmpty() || title.length() < 5) {
                        Element parent = linkEl.parent();
                        if (parent != null) {
                            Element titleEl = parent.selectFirst("h1, h2, h3, h4, h5, .title, .headline, .post-title, .entry-title");
                            if (titleEl != null) {
                                title = titleEl.text().trim();
                            }
                            
                            // 如果还是没有，尝试从最近的 article 或 post 获取
                            if (title.isEmpty()) {
                                Element card = linkEl.closest("article, .post, .news-item, .news-card, .entry");
                                if (card != null) {
                                    titleEl = card.selectFirst("h1, h2, h3, h4, h5, .title, .headline");
                                    if (titleEl != null) {
                                        title = titleEl.text().trim();
                                    }
                                }
                            }
                        }
                    }
                    
                    if (title.isEmpty() || title.length() < 3) {
                        continue;
                    }
                    
                    // 提取摘要
                    String summary = null;
                    Element card = linkEl.closest("article, .post, .news-item, .news-card, .entry, .post-item");
                    if (card != null) {
                        Element summaryEl = card.selectFirst(".summary, .excerpt, .description, .snippet, .content, p");
                        if (summaryEl != null) {
                            summary = summaryEl.text().trim();
                            if (summary.length() > 200) {
                                summary = summary.substring(0, 200) + "...";
                            }
                        }
                    }
                    
                    News news = News.builder()
                            .title(title)
                            .originalUrl(absUrl)
                            .source("清迈大学校友会 (CMUAC)")
                            .summary(summary)
                            .createTime(new Date())
                            .publishDate(new Date())
                            .build();
                    
                    newsList.add(news);
                    foundCount++;
                    log.info("添加清迈大学校友会新闻: {} -> {}", title, absUrl);
                    
                    // 限制每个网站最多抓取15条
                    if (foundCount >= 15) {
                        break;
                    }
                    
                } catch (Exception ex) {
                    log.warn("解析清迈大学校友会新闻链接时出错: {} - {}", href, ex.getMessage());
                }
            }
            
            log.info("从清迈大学校友会网站成功提取 {} 条新闻", foundCount);
            
        } catch (IOException e) {
            log.warn("爬取清迈大学校友会网站失败: {}", e.getMessage());
        } catch (Exception e) {
            log.error("处理清迈大学校友会网站时出错: {}", e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * 爬取中国驻清迈总领馆网站
     * 
     * @deprecated 此方法已废弃 - 该网站无法获取有效新闻（RSS全部失败，HTML爬取SSL证书问题）
     * @return 新闻对象列表
     */
    @Deprecated
    private List<News> crawlConsulate() {
        List<News> newsList = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        
        try {
            String baseUrl = "https://chiangmai.china-consulate.gov.cn/";
            String[] newsUrls = {
                baseUrl,  // 首页
                baseUrl + "xwdt/",  // 新闻动态
            };
            
            for (String url : newsUrls) {
                try {
                    log.info("开始爬取中国驻清迈总领馆网站: {}", url);
                    
                    // 先尝试使用 Jsoup（如果 SSL 失败，会抛出异常）
                    Document doc;
                    try {
                        doc = Jsoup.connect(url)
                                .userAgent(USER_AGENT)
                                .timeout(TIMEOUT)
                                .followRedirects(true)
                                .get();
                    } catch (javax.net.ssl.SSLException e) {
                        // SSL 证书验证失败，使用 HttpClient 绕过
                        log.warn("Jsoup SSL 验证失败，尝试使用 HttpClient: {}", e.getMessage());
                        
                        SSLContext sslContext = SSLContextBuilder.create()
                                .loadTrustMaterial(new TrustSelfSignedStrategy())
                                .build();
                        
                        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                                sslContext,
                                (hostname, session) -> true
                        );
                        
                        try (CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder.create()
                                        .setSSLSocketFactory(sslSocketFactory)
                                        .build())
                                .build()) {
                            
                            HttpGet request = new HttpGet(url);
                            request.setHeader("User-Agent", USER_AGENT);
                            
                            try (CloseableHttpResponse response = httpClient.execute(request)) {
                                String html = EntityUtils.toString(response.getEntity());
                                doc = Jsoup.parse(html, url);
                            }
                        }
                    }
                    
                    // 查找新闻链接 - 总领馆网站通常有特定的新闻列表结构
                    Elements linkElements = doc.select("a[href*='xwdt'], a[href*='news'], .news-list a, .article-list a, li a");
                    
                    log.info("在中国驻清迈总领馆网站 {} 发现 {} 个候选链接", url, linkElements.size());
                    
                    int foundCount = 0;
                    for (Element linkEl : linkElements) {
                        String href = null;
                        try {
                            href = linkEl.attr("href");
                            if (href == null || href.isEmpty()) {
                                continue;
                            }
                            
                            // 规范化为绝对链接
                            String absUrl = linkEl.absUrl("href");
                            if (absUrl == null || absUrl.isEmpty()) {
                                if (href.startsWith("http")) {
                                    absUrl = href;
                                } else if (href.startsWith("/")) {
                                    absUrl = baseUrl + href.substring(1);
                                } else {
                                    absUrl = baseUrl + href;
                                }
                            }
                            
                            // 只处理总领馆网站的链接
                            if (!absUrl.contains("chiangmai.china-consulate.gov.cn")) {
                                continue;
                            }
                            
                            // 去重
                            if (!seenUrls.add(absUrl)) {
                                continue;
                            }
                            
                            // 过滤掉非新闻链接
                            String lowerUrl = absUrl.toLowerCase();
                            if (lowerUrl.endsWith("/") || 
                                lowerUrl.contains("/index") ||
                                lowerUrl.contains("/more") ||
                                !lowerUrl.contains("xwdt") && !lowerUrl.contains("news")) {
                                continue;
                            }
                            
                            // 提取标题
                            String title = linkEl.text() != null ? linkEl.text().trim() : "";
                            
                            // 如果链接文本为空，尝试从父元素获取
                            if (title.isEmpty() || title.length() < 5) {
                                Element parent = linkEl.parent();
                                if (parent != null) {
                                    Element titleEl = parent.selectFirst("h1, h2, h3, h4, h5, .title, .headline");
                                    if (titleEl != null) {
                                        title = titleEl.text().trim();
                                    }
                                    
                                    // 尝试从最近的 li 或 article 获取
                                    if (title.isEmpty()) {
                                        Element card = linkEl.closest("li, article, .news-item, .article-item");
                                        if (card != null) {
                                            titleEl = card.selectFirst("h1, h2, h3, h4, h5, .title");
                                            if (titleEl != null) {
                                                title = titleEl.text().trim();
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (title.isEmpty() || title.length() < 3) {
                                continue;
                            }
                            
                            // 提取摘要
                            String summary = null;
                            Element card = linkEl.closest("li, article, .news-item, .article-item");
                            if (card != null) {
                                // 提取摘要
                                Element summaryEl = card.selectFirst(".summary, .excerpt, .description, .snippet, p");
                                if (summaryEl != null) {
                                    summary = summaryEl.text().trim();
                                    if (summary.length() > 200) {
                                        summary = summary.substring(0, 200) + "...";
                                    }
                                }
                            }
                            
                            News news = News.builder()
                                    .title(title)
                                    .originalUrl(absUrl)
                                    .source("中国驻清迈总领馆")
                                    .summary(summary)
                                    .createTime(new Date())
                                    .publishDate(new Date())
                                    .build();
                            
                            newsList.add(news);
                            foundCount++;
                            log.info("添加中国驻清迈总领馆新闻: {} -> {}", title, absUrl);
                            
                            // 限制每个页面最多抓取10条
                            if (foundCount >= 10) {
                                break;
                            }
                            
                        } catch (Exception ex) {
                            log.warn("解析中国驻清迈总领馆新闻链接时出错: {} - {}", href, ex.getMessage());
                        }
                    }
                    
                    log.info("从 {} 成功提取 {} 条新闻", url, foundCount);
                    
                } catch (IOException e) {
                    log.warn("爬取中国驻清迈总领馆网站失败: {} - {}", url, e.getMessage());
                } catch (Exception e) {
                    log.error("处理中国驻清迈总领馆网站时出错: {} - {}", url, e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("爬取中国驻清迈总领馆网站时发生异常: {}", e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * 爬取泰国电子签证网站
     * 
     * @deprecated 此方法已废弃 - 该网站无法获取有效新闻（RSS解析失败，HTML爬取无有效链接）
     * @return 新闻对象列表
     */
    @Deprecated
    private List<News> crawlThailandVisa() {
        List<News> newsList = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        
        try {
            String url = "https://www.thaievisa.go.th/";
            log.info("开始爬取泰国电子签证网站: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();
            
            // 查找新闻或公告链接
            Elements linkElements = doc.select("a[href*='news'], a[href*='announcement'], a[href*='notice'], .news a, .announcement a");
            
            log.info("在泰国电子签证网站发现 {} 个候选链接", linkElements.size());
            
            int foundCount = 0;
            for (Element linkEl : linkElements) {
                String href = null;
                try {
                    href = linkEl.attr("href");
                    if (href == null || href.isEmpty()) {
                        continue;
                    }
                    
                    // 规范化为绝对链接
                    String absUrl = linkEl.absUrl("href");
                    if (absUrl == null || absUrl.isEmpty()) {
                        if (href.startsWith("http")) {
                            absUrl = href;
                        } else if (href.startsWith("/")) {
                            absUrl = "https://www.thaievisa.go.th" + href;
                        } else {
                            absUrl = "https://www.thaievisa.go.th/" + href;
                        }
                    }
                    
                    // 只处理签证网站的链接
                    if (!absUrl.contains("thaievisa.go.th")) {
                        continue;
                    }
                    
                    // 去重
                    if (!seenUrls.add(absUrl)) {
                        continue;
                    }
                    
                    // 提取标题
                    String title = linkEl.text() != null ? linkEl.text().trim() : "";
                    
                    if (title.isEmpty() || title.length() < 5) {
                        continue;
                    }
                    
                    // 提取摘要
                    String summary = null;
                    Element card = linkEl.closest("article, .news-item, .announcement-item, li");
                    if (card != null) {
                        Element summaryEl = card.selectFirst(".summary, .excerpt, .description, p");
                        if (summaryEl != null) {
                            summary = summaryEl.text().trim();
                            if (summary.length() > 200) {
                                summary = summary.substring(0, 200) + "...";
                            }
                        }
                    }
                    
                    News news = News.builder()
                            .title(title)
                            .originalUrl(absUrl)
                            .source("泰国电子签证")
                            .summary(summary)
                            .createTime(new Date())
                            .publishDate(new Date())
                            .build();
                    
                    newsList.add(news);
                    foundCount++;
                    log.info("添加泰国电子签证新闻: {} -> {}", title, absUrl);
                    
                    // 限制最多抓取10条
                    if (foundCount >= 10) {
                        break;
                    }
                    
                } catch (Exception ex) {
                    log.warn("解析泰国电子签证新闻链接时出错: {} - {}", href, ex.getMessage());
                }
            }
            
            log.info("从泰国电子签证网站成功提取 {} 条新闻", foundCount);
            
        } catch (IOException e) {
            log.warn("爬取泰国电子签证网站失败: {}", e.getMessage());
        } catch (Exception e) {
            log.error("处理泰国电子签证网站时出错: {}", e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * 从 HTML 元素中提取新闻信息（保留原方法，可能其他地方还在使用）
            
            for (String url : cmuUrls) {
                try {
                    log.info("开始爬取清迈大学新闻: {}", url);
                    
                    Document doc = Jsoup.connect(url)
                            .userAgent(USER_AGENT)
                            .timeout(TIMEOUT)
                            .followRedirects(true)
                            .get();
                    
                    // 尝试多种选择器来找到新闻链接（更通用的选择器）
                    Elements linkElements = doc.select("a[href]");
                    
                    log.info("在清迈大学页面 {} 发现 {} 个链接", url, linkElements.size());
                    
                    int foundCount = 0;
                    for (Element linkEl : linkElements) {
                        String href = null;
                        try {
                            href = linkEl.attr("href");
                            if (href == null || href.isEmpty()) {
                                continue;
                            }
                            
                            // 规范化为绝对链接
                            String absUrl = linkEl.absUrl("href");
                            if (absUrl == null || absUrl.isEmpty()) {
                                if (href.startsWith("http")) {
                                    absUrl = href;
                                } else if (href.startsWith("/")) {
                                    absUrl = CMU_BASE_URL + href;
                                } else {
                                    absUrl = CMU_BASE_URL + "/" + href;
                                }
                            }
                            
                            // 去重
                            if (!seenUrls.add(absUrl)) {
                                continue;
                            }
                            
                            // 更宽松的过滤条件：包含 news、announcement、article 等关键词
                            String lowerUrl = absUrl.toLowerCase();
                            boolean isNewsUrl = lowerUrl.contains("/news/") || 
                                              lowerUrl.contains("/announcement") ||
                                              lowerUrl.contains("/article/") ||
                                              lowerUrl.contains("/press/") ||
                                              lowerUrl.contains("/event/") ||
                                              (lowerUrl.contains("cmu.ac.th") && 
                                               (lowerUrl.contains("news") || lowerUrl.contains("announce")));
                            
                            if (!isNewsUrl) {
                                continue;
                            }
                            
                            // 排除一些明显不是新闻的链接
                            if (lowerUrl.contains("/category/") || 
                                lowerUrl.contains("/tag/") ||
                                lowerUrl.contains("/author/") ||
                                lowerUrl.contains("/page/") ||
                                lowerUrl.endsWith("/news") ||
                                lowerUrl.endsWith("/news/")) {
                                continue;
                            }
                            
                            // 提取标题 - 多种方式
                            String title = linkEl.text() != null ? linkEl.text().trim() : "";
                            
                            // 如果链接文本为空或太短，尝试从父元素获取
                            if (title.isEmpty() || title.length() < 5) {
                                Element parent = linkEl.parent();
                                if (parent != null) {
                                    // 尝试从父元素找标题
                                    Element titleEl = parent.selectFirst("h1, h2, h3, h4, h5, .title, .headline, .news-title, .post-title");
                                    if (titleEl != null) {
                                        title = titleEl.text().trim();
                                    }
                                    
                                    // 如果还是没有，尝试从最近的 article 或 news-item 获取
                                    if (title.isEmpty()) {
                                        Element card = linkEl.closest("article, .news-item, .news-card, .post-item, li");
                                        if (card != null) {
                                            titleEl = card.selectFirst("h1, h2, h3, h4, h5, .title, .headline");
                                            if (titleEl != null) {
                                                title = titleEl.text().trim();
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 如果标题仍然为空，尝试从URL提取
                            if (title.isEmpty()) {
                                // 从URL的最后一部分提取标题
                                String[] parts = absUrl.split("/");
                                if (parts.length > 0) {
                                    String lastPart = parts[parts.length - 1];
                                    if (lastPart != null && !lastPart.isEmpty() && !lastPart.matches("\\d+")) {
                                        title = lastPart.replace('-', ' ').replace('_', ' ').trim();
                                    }
                                }
                            }
                            
                            if (title.isEmpty() || title.length() < 3) {
                                continue;
                            }
                            
                            // 提取摘要
                            String summary = null;
                            Element card = linkEl.closest("article, .news-item, .news-card, .post-item, li, .media, .entry");
                            if (card != null) {
                                Element summaryEl = card.selectFirst(".summary, .excerpt, .description, .snippet, .content, p");
                                if (summaryEl != null) {
                                    summary = summaryEl.text().trim();
                                    // 限制摘要长度
                                    if (summary.length() > 200) {
                                        summary = summary.substring(0, 200) + "...";
                                    }
                                }
                            }
                            
                            News news = News.builder()
                                    .title(title)
                                    .originalUrl(absUrl)
                                    .source("清迈大学 (CMU)")
                                    .summary(summary)
                                    .createTime(new Date())
                                    .publishDate(new Date())
                                    .build();
                            
                            newsList.add(news);
                            foundCount++;
                            log.info("添加清迈大学新闻: {} -> {}", title, absUrl);
                            
                            // 限制每个页面最多抓取20条
                            if (foundCount >= 20) {
                                break;
                            }
                            
                        } catch (Exception ex) {
                            log.warn("解析清迈大学新闻链接时出错: {} - {}", href, ex.getMessage());
                        }
                    }
                    
                    log.info("从 {} 成功提取 {} 条新闻", url, foundCount);
                    
                } catch (IOException e) {
                    log.warn("爬取清迈大学新闻页面失败: {} - {}", url, e.getMessage());
                } catch (Exception e) {
                    log.error("处理清迈大学新闻页面时出错: {} - {}", url, e.getMessage(), e);
                }
            }
            
            log.info("成功爬取 {} 条清迈大学新闻", newsList.size());
            
        } catch (Exception e) {
            log.error("爬取清迈大学新闻时发生异常: {}", e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * 从 HTML 元素中提取新闻信息
     * 
     * @param element HTML 元素
     * @param source 来源网站名称
     * @return News 对象，如果提取失败则返回 null
     */
    private News extractNewsFromElement(Element element, String source) {
        try {
            // 提取标题 - 尝试多种可能的选择器
            String title = null;
            Element titleElement = element.selectFirst("h1, h2, h3, h4, .title, .headline, a[href]");
            if (titleElement != null) {
                title = titleElement.text().trim();
                // 如果标题元素是链接，也可以从链接文本获取
                if (title.isEmpty() && titleElement.tagName().equals("a")) {
                    title = titleElement.text().trim();
                }
            }

            // 提取链接
            String link = null;
            Element linkElement = element.selectFirst("a[href]");
            if (linkElement != null) {
                String href = linkElement.attr("href");
                if (href != null && !href.isEmpty()) {
                    // 处理相对链接
                    if (href.startsWith("/")) {
                        link = BANGKOK_POST_BASE_URL + href;
                    } else if (href.startsWith("http")) {
                        link = href;
                    } else {
                        link = BANGKOK_POST_BASE_URL + "/" + href;
                    }
                }
            }

            // 提取摘要/描述 - 尝试多种可能的选择器
            String summary = null;
            Element summaryElement = element.selectFirst(".summary, .excerpt, .description, .snippet, p");
            if (summaryElement != null) {
                summary = summaryElement.text().trim();
            }

            // 如果标题或链接为空，跳过这条新闻
            if ((title == null || title.isEmpty()) && (link == null || link.isEmpty())) {
                return null;
            }

            // 如果标题为空但链接存在，尝试从链接文本获取标题
            if ((title == null || title.isEmpty()) && linkElement != null) {
                title = linkElement.text().trim();
            }

            // 构建 News 对象
            return News.builder()
                    .title(title)
                    .originalUrl(link)
                    .source(source)
                    .summary(summary) // 这里先存储摘要，后续可以用 AI 生成更详细的总结
                    .createTime(new Date())
                    .publishDate(new Date()) // 如果页面有发布时间，可以在这里提取
                    .build();

        } catch (Exception e) {
            log.warn("提取新闻信息时出错: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 爬取指定 URL 的新闻详情内容
     * 用于获取完整的新闻正文
     * 
     * @param url 新闻详情页 URL
     * @return 新闻正文内容
     */
    public String crawlNewsContent(String url) {
        try {
            log.debug("爬取新闻详情: {}", url);
            
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();

            // 尝试多种选择器来提取正文内容
            Element contentElement = null;
            
            // 方法1: 尝试常见的文章容器
            String[] selectors = {
                "article .article-body",
                "article .story-body",
                ".article-content",
                ".story-content",
                ".article-body",
                ".story-body",
                "article",
                ".content",
                "main article",
                "[class*='article']",
                "[class*='story']"
            };
            
            for (String selector : selectors) {
                contentElement = doc.selectFirst(selector);
                if (contentElement != null) {
                    // 移除脚本、样式、广告等不需要的元素
                    contentElement.select("script, style, .advertisement, .ad, .social-share, .related-articles, nav, header, footer").remove();
                    String text = contentElement.text().trim();
                    if (text.length() > 100) { // 确保内容足够长
                        log.debug("使用选择器 '{}' 成功提取正文，长度: {}", selector, text.length());
                        return text;
                    }
                }
            }

            // 方法2: 如果没有找到特定容器，尝试从 body 中提取主要内容
            Element body = doc.body();
            if (body != null) {
                // 移除导航、页脚、广告等
                body.select("script, style, nav, header, footer, .advertisement, .ad, .sidebar, .related").remove();
                String bodyText = body.text().trim();
                if (bodyText.length() > 100) {
                    log.debug("从 body 提取正文，长度: {}", bodyText.length());
                    return bodyText;
                }
            }

            log.warn("未能提取到足够的正文内容: {}", url);
            return null;

        } catch (IOException e) {
            log.error("爬取新闻详情时发生 IO 异常: {} - {}", url, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("爬取新闻详情时发生未知异常: {} - {}", url, e.getMessage());
            return null;
        }
    }
}

