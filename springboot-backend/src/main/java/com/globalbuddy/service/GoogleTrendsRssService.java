package com.globalbuddy.service;

import com.globalbuddy.model.News;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.ParsingFeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Google Trends RSS Service
 * Fetches trending topics from Google Trends RSS to determine news direction
 * Uses ROME library for RSS parsing
 */
@Slf4j
@Service
public class GoogleTrendsRssService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT = 30000; // 30 seconds timeout

    /**
     * Google Trends RSS URLs by region
     * Updated URL format (verified by Gemini):
     * - New format: https://trends.google.com/trending/rss?geo=TH
     */
    public static final String GOOGLE_TRENDS_THAILAND = "https://trends.google.com/trending/rss?geo=TH";
    public static final String GOOGLE_TRENDS_US = "https://trends.google.com/trending/rss?geo=US";
    public static final String GOOGLE_TRENDS_GLOBAL = "https://trends.google.com/trending/rss";

    /**
     * Create a trust-all SSL context for handling SSL certificate issues
     */
    private static SSLContext createTrustAllSSLContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            return sc;
        } catch (Exception e) {
            log.warn("Failed to create trust-all SSL context: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch trending keywords from Google Trends RSS
     * 
     * @param geoCode Geographic code (e.g., "TH" for Thailand, "US" for USA, null for global)
     * @param maxItems Maximum number of trending items to fetch
     * @return List of trending keywords/topics
     */
    public List<String> fetchTrendingKeywords(String geoCode, int maxItems) {
        List<String> keywords = new ArrayList<>();
        
        // Map geoCode to appropriate RSS URL
        String feedUrl = GOOGLE_TRENDS_GLOBAL;
        if (geoCode != null && !geoCode.isEmpty()) {
            switch (geoCode.toUpperCase()) {
                case "TH":
                    feedUrl = GOOGLE_TRENDS_THAILAND;
                    break;
                case "US":
                    feedUrl = GOOGLE_TRENDS_US;
                    break;
                default:
                    // Use new format for other countries
                    feedUrl = "https://trends.google.com/trending/rss?geo=" + geoCode;
                    break;
            }
        }
        
        try {
            log.info("Fetching trending keywords from Google Trends RSS: {}", feedUrl);
            
            // Create URL connection
            URL url = new URL(feedUrl);
            URLConnection connection = url.openConnection();
            
            // Handle HTTPS connections
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                SSLContext sslContext = createTrustAllSSLContext();
                if (sslContext != null) {
                    httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                }
            }
            
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            
            // Check HTTP response code
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                int responseCode = httpConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    log.warn("HTTP error response {} for Google Trends RSS: {}", responseCode, feedUrl);
                    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        log.warn("⚠️ Google Trends RSS not found (404): {}. This is normal - Google Trends RSS service may be deprecated or unavailable. Continuing without trending keywords.", feedUrl);
                        return keywords; // Return empty list, system will continue without trending keywords
                    }
                    // For other errors, also return empty list gracefully
                    log.warn("⚠️ Google Trends RSS unavailable (HTTP {}). Continuing without trending keywords.", responseCode);
                    return keywords;
                }
            }
            
            // Read and parse RSS feed using ROME library
            try (InputStream inputStream = connection.getInputStream()) {
                // Read content for preprocessing
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                // Preprocess XML: remove DOCTYPE if present
                String xmlContent = content.toString();
                xmlContent = xmlContent.replaceAll("<!DOCTYPE[^>]*>", "");
                
                // Create InputStream from preprocessed content
                InputStream processedStream = new java.io.ByteArrayInputStream(
                    xmlContent.getBytes(StandardCharsets.UTF_8));
                
                // Use ROME library's XmlReader with lenient mode
                XmlReader reader = new XmlReader(processedStream, true);
                
                // Parse with ROME's SyndFeedInput
                SyndFeedInput input = new SyndFeedInput();
                input.setPreserveWireFeed(true);
                
                SyndFeed feed = input.build(reader);
                
                log.info("Successfully parsed Google Trends RSS: {} (title: {}, entries: {})", 
                        feedUrl, feed.getTitle(), feed.getEntries().size());
                
                // Extract keywords from feed entries
                int count = 0;
                for (SyndEntry entry : feed.getEntries()) {
                    if (count >= maxItems) {
                        break;
                    }
                    
                    try {
                        String title = entry.getTitle();
                        if (title != null && !title.trim().isEmpty()) {
                            keywords.add(title.trim());
                            count++;
                            log.debug("Extracted trending keyword: {}", title);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to extract keyword from entry: {}", e.getMessage());
                    }
                }
                
                log.info("Successfully extracted {} trending keywords from Google Trends RSS", keywords.size());
            }
            
        } catch (ParsingFeedException e) {
            log.error("Failed to parse Google Trends RSS XML: {} - Error: {}", feedUrl, e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to fetch trending keywords from Google Trends RSS: {} - {}", 
                    feedUrl, e.getMessage(), e);
        }
        
        return keywords;
    }

    /**
     * Fetch trending keywords for Thailand
     * 
     * @param maxItems Maximum number of trending items
     * @return List of trending keywords
     */
    public List<String> fetchThailandTrendingKeywords(int maxItems) {
        return fetchTrendingKeywords("TH", maxItems);
    }

    /**
     * Filter news items based on trending keywords
     * Prioritizes news that contains trending keywords
     * 
     * @param newsList Original news list
     * @param trendingKeywords List of trending keywords
     * @return Filtered and prioritized news list
     */
    public List<News> prioritizeNewsByTrends(List<News> newsList, List<String> trendingKeywords) {
        if (trendingKeywords == null || trendingKeywords.isEmpty()) {
            return newsList;
        }
        
        // Create a set of keywords for faster lookup (case-insensitive)
        Set<String> keywordSet = trendingKeywords.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        List<News> prioritizedNews = new ArrayList<>();
        List<News> nonTrendingNews = new ArrayList<>();
        
        for (News news : newsList) {
            boolean isTrending = false;
            String title = news.getTitle() != null ? news.getTitle().toLowerCase() : "";
            String summary = news.getSummary() != null ? news.getSummary().toLowerCase() : "";
            
            // Check if news title or summary contains any trending keyword
            for (String keyword : keywordSet) {
                if (title.contains(keyword) || summary.contains(keyword)) {
                    isTrending = true;
                    break;
                }
            }
            
            if (isTrending) {
                prioritizedNews.add(news);
            } else {
                nonTrendingNews.add(news);
            }
        }
        
        // Combine: trending news first, then non-trending news
        prioritizedNews.addAll(nonTrendingNews);
        
        log.info("Prioritized {} news items based on {} trending keywords ({} trending, {} non-trending)", 
                prioritizedNews.size(), trendingKeywords.size(), 
                prioritizedNews.size() - nonTrendingNews.size(), nonTrendingNews.size());
        
        return prioritizedNews;
    }
}

