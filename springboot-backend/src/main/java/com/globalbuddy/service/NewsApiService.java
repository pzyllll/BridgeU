package com.globalbuddy.service;

import com.globalbuddy.model.News;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * NewsAPI.org Service
 * Fetches news from NewsAPI.org for Thailand
 */
@Slf4j
@Service
public class NewsApiService {

    private static final String API_KEY = "1c2bae1e58584a83a65437b281a6a271";
    private static final String BASE_URL = "https://newsapi.org/v2";
    private static final String COUNTRY_CODE = "th"; // Thailand
    private static final int MAX_ARTICLES = 50; // Maximum articles to fetch per request
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 30000; // 30 seconds
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    private final SimpleDateFormat dateFormat3 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    /**
     * Fetch top headlines from Thailand
     * 
     * @param maxItems Maximum number of articles to fetch
     * @return List of News objects
     */
    public List<News> fetchThailandHeadlines(int maxItems) {
        List<News> newsList = new ArrayList<>();
        
        try {
            // Build API URL
            String apiUrl = BASE_URL + "/top-headlines?country=" + COUNTRY_CODE + 
                          "&pageSize=" + Math.min(maxItems, MAX_ARTICLES) + 
                          "&apiKey=" + API_KEY;
            
            log.info("Fetching news from NewsAPI.org (Thailand): {}", apiUrl.replace(API_KEY, "***"));
            
            // Create connection
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("NewsAPI.org returned error code: {} - {}", responseCode, connection.getResponseMessage());
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    log.error("Invalid API key for NewsAPI.org");
                } else if (responseCode == 429) { // HTTP 429 Too Many Requests
                    log.error("Rate limit exceeded for NewsAPI.org");
                }
                return newsList;
            }
            
            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            // Parse JSON response
            JsonNode rootNode = objectMapper.readTree(response.toString());
            
            // Check status
            String status = rootNode.path("status").asText();
            if (!"ok".equals(status)) {
                String message = rootNode.path("message").asText("Unknown error");
                log.error("NewsAPI.org returned error status: {} - {}", status, message);
                return newsList;
            }
            
            // Get articles
            JsonNode articlesNode = rootNode.path("articles");
            if (!articlesNode.isArray()) {
                log.warn("No articles found in NewsAPI.org response");
                return newsList;
            }
            
            int count = 0;
            for (JsonNode articleNode : articlesNode) {
                if (count >= maxItems) {
                    break;
                }
                
                try {
                    News news = parseArticle(articleNode);
                    if (news != null) {
                        newsList.add(news);
                        count++;
                        log.debug("Parsed article from NewsAPI.org: {} - {}", news.getSource(), news.getTitle());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse article from NewsAPI.org: {}", e.getMessage());
                }
            }
            
            log.info("Successfully fetched {} articles from NewsAPI.org (Thailand)", newsList.size());
            
        } catch (Exception e) {
            log.error("Failed to fetch news from NewsAPI.org: {}", e.getMessage(), e);
        }
        
        return newsList;
    }

    /**
     * Parse a single article from NewsAPI.org JSON response
     * 
     * @param articleNode JSON node representing an article
     * @return News object or null if parsing fails
     */
    private News parseArticle(JsonNode articleNode) {
        try {
            News news = new News();
            
            // Title
            String title = articleNode.path("title").asText();
            if (title == null || title.trim().isEmpty() || "null".equals(title)) {
                log.debug("Skipping article with empty title");
                return null;
            }
            news.setTitle(title);
            
            // Description/Summary
            String description = articleNode.path("description").asText();
            if (description != null && !description.isEmpty() && !"null".equals(description)) {
                news.setSummary(description);
            } else {
                // Use title as summary if description is missing
                news.setSummary(title);
            }
            
            // URL
            String url = articleNode.path("url").asText();
            if (url != null && !url.isEmpty() && !"null".equals(url)) {
                news.setOriginalUrl(url);
            }
            
            // Source
            JsonNode sourceNode = articleNode.path("source");
            String sourceName = sourceNode.path("name").asText();
            if (sourceName != null && !sourceName.isEmpty() && !"null".equals(sourceName)) {
                news.setSource("NewsAPI: " + sourceName);
            } else {
                news.setSource("NewsAPI.org");
            }
            
            // Published date
            String publishedAt = articleNode.path("publishedAt").asText();
            if (publishedAt != null && !publishedAt.isEmpty() && !"null".equals(publishedAt)) {
                try {
                    Date publishDate = parseDate(publishedAt);
                    if (publishDate != null) {
                        news.setPublishDate(publishDate);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse published date: {}", publishedAt);
                }
            }
            
            // Content (if available)
            String content = articleNode.path("content").asText();
            if (content != null && !content.isEmpty() && !"null".equals(content)) {
                // Remove [+XXX chars] suffix if present
                content = content.replaceAll("\\[\\+\\d+\\s+chars\\]", "").trim();
                news.setOriginalContent(content);
            } else {
                // Use description as content if content is missing
                news.setOriginalContent(description != null ? description : title);
            }
            
            // Set create time to current time if publish date is not available
            if (news.getPublishDate() == null) {
                news.setPublishDate(new Date());
            }
            news.setCreateTime(new Date());
            
            return news;
            
        } catch (Exception e) {
            log.error("Error parsing article from NewsAPI.org: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse date string from NewsAPI.org format
     * 
     * @param dateStr Date string in various formats
     * @return Date object or null if parsing fails
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr)) {
            return null;
        }
        
        try {
            // Try different date formats
            if (dateStr.contains(".") && dateStr.contains("Z")) {
                return dateFormat2.parse(dateStr);
            } else if (dateStr.endsWith("Z")) {
                return dateFormat.parse(dateStr);
            } else {
                return dateFormat3.parse(dateStr);
            }
        } catch (Exception e) {
            log.debug("Failed to parse date string: {}", dateStr);
            return null;
        }
    }
}

