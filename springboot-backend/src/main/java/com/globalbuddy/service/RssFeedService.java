package com.globalbuddy.service;

import com.globalbuddy.model.News;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * RSS Feed Service
 * Fetches news using RSS standard format to avoid web scraping restrictions
 */
@Slf4j
@Service
public class RssFeedService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 30000; // 30 seconds timeout

    /**
     * Fetch news from RSS feed
     * 
     * @param feedUrl RSS feed URL
     * @param source News source identifier
     * @param maxItems Maximum number of news items to fetch
     * @return List of news objects
     */
    public List<News> fetchNewsFromRss(String feedUrl, String source, int maxItems) {
        List<News> newsList = new ArrayList<>();
        
        try {
            log.info("Starting to fetch news from RSS feed: {} (source: {})", feedUrl, source);
            
            // Create URL connection
            URL url = new URL(feedUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            // Read RSS feed
            try (InputStream inputStream = connection.getInputStream();
                 XmlReader reader = new XmlReader(inputStream)) {
                
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(reader);
                
                log.info("Successfully parsed RSS feed: {} (title: {}, entries: {})", 
                        feedUrl, feed.getTitle(), feed.getEntries().size());
                
                int count = 0;
                for (SyndEntry entry : feed.getEntries()) {
                    if (count >= maxItems) {
                        break;
                    }
                    
                    try {
                        News news = convertEntryToNews(entry, source);
                        if (news != null) {
                            newsList.add(news);
                            count++;
                            log.debug("Added news from RSS: {} -> {}", news.getTitle(), news.getOriginalUrl());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse RSS entry: {} - {}", entry.getTitle(), e.getMessage());
                    }
                }
                
                log.info("Successfully fetched {} news items from RSS feed {}", newsList.size(), feedUrl);
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch news from RSS feed: {} - {}", feedUrl, e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * Convert RSS entry to News object
     */
    private News convertEntryToNews(SyndEntry entry, String source) {
        try {
            String title = entry.getTitle();
            if (title == null || title.trim().isEmpty()) {
                return null;
            }
            
            String link = null;
            if (entry.getLink() != null) {
                link = entry.getLink();
            } else if (entry.getUri() != null) {
                link = entry.getUri();
            }
            
            // Extract summary/description
            String summary = null;
            if (entry.getDescription() != null) {
                summary = entry.getDescription().getValue();
                if (summary != null) {
                    // Remove HTML tags
                    summary = summary.replaceAll("<[^>]+>", "").trim();
                    // Limit length
                    if (summary.length() > 500) {
                        summary = summary.substring(0, 500) + "...";
                    }
                }
            }
            
            // If no description in RSS, use title as initial summary (will be replaced by AI summary later)
            if (summary == null || summary.isEmpty()) {
                summary = title; // Use title as placeholder, will be replaced by AI summary
                log.debug("RSS entry has no description, using title as placeholder: {}", title);
            }
            
            // Extract publish date
            Date publishDate = entry.getPublishedDate();
            if (publishDate == null) {
                publishDate = entry.getUpdatedDate();
            }
            if (publishDate == null) {
                publishDate = new Date();
            }
            
            News news = News.builder()
                    .title(title.trim())
                    .originalUrl(link)
                    .source(source)
                    .summary(summary)
                    .publishDate(publishDate)
                    .createTime(new Date())
                    .build();
            
            return news;
            
        } catch (Exception e) {
            log.error("Failed to convert RSS entry: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetch news from multiple RSS feeds in batch
     * 
     * @param feedConfigs List of RSS feed configurations (each contains URL and source identifier)
     * @param maxItemsPerFeed Maximum number of items per feed
     * @return Merged list of news items
     */
    public List<News> fetchNewsFromMultipleRss(List<RssFeedConfig> feedConfigs, int maxItemsPerFeed) {
        List<News> allNews = new ArrayList<>();
        
        for (RssFeedConfig config : feedConfigs) {
            try {
                List<News> news = fetchNewsFromRss(config.getUrl(), config.getSource(), maxItemsPerFeed);
                allNews.addAll(news);
            } catch (Exception e) {
                log.error("Failed to fetch news from RSS feed: {} - {}", config.getUrl(), e.getMessage());
            }
        }
        
        return allNews;
    }

    /**
     * RSS feed configuration class
     */
    public static class RssFeedConfig {
        private String url;
        private String source;

        public RssFeedConfig(String url, String source) {
            this.url = url;
            this.source = source;
        }

        public String getUrl() {
            return url;
        }

        public String getSource() {
            return source;
        }
    }
}

