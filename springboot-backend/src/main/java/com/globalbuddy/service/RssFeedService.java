package com.globalbuddy.service;

import com.globalbuddy.model.News;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.ParsingFeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
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
    /**
     * 网络环境不稳定时，Google News RSS 可能响应较慢：
     * - 连接超时保持相对保守，避免完全不可达时长时间卡死
     * - 读取超时适当放宽，让大一点的 RSS 响应有足够时间返回
     */
    private static final int CONNECT_TIMEOUT = 15000; // 15 seconds connect timeout
    private static final int READ_TIMEOUT = 90000;    // 90 seconds read timeout

    /**
     * Create a trust-all SSL context for handling SSL certificate issues
     * WARNING: This should only be used in development environments
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
     * Fetch news from RSS feed
     * 
     * @param feedUrl RSS feed URL
     * @param source News source identifier
     * @param maxItems Maximum number of news items to fetch
     * @return List of news objects
     */
    // Track redirect depth to prevent infinite loops
    private static final int MAX_REDIRECTS = 5;
    
    public List<News> fetchNewsFromRss(String feedUrl, String source, int maxItems) {
        return fetchNewsFromRss(feedUrl, source, maxItems, 0);
    }
    
    private List<News> fetchNewsFromRss(String feedUrl, String source, int maxItems, int redirectDepth) {
        List<News> newsList = new ArrayList<>();
        
        // Prevent infinite redirect loops
        if (redirectDepth >= MAX_REDIRECTS) {
            log.warn("Maximum redirect depth ({}) reached for RSS feed: {}", MAX_REDIRECTS, feedUrl);
            return newsList;
        }
        
        try {
            log.info("Starting to fetch news from RSS feed: {} (source: {})", feedUrl, source);
            
            // Create URL connection
            URL url = new URL(feedUrl);
            URLConnection connection = url.openConnection();
            
            // Handle HTTPS connections with SSL certificate issues
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                SSLContext sslContext = createTrustAllSSLContext();
                if (sslContext != null) {
                    httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    log.debug("Using trust-all SSL context for HTTPS connection: {}", feedUrl);
                }
            }
            
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            // Check HTTP response code and handle redirects
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                int responseCode = httpConnection.getResponseCode();
                
                // Handle redirects (301, 302, 307, 308)
                if (responseCode >= 300 && responseCode < 400) {
                    String redirectUrl = httpConnection.getHeaderField("Location");
                    if (redirectUrl != null && !redirectUrl.isEmpty()) {
                        log.info("Following redirect {} -> {} (depth: {})", feedUrl, redirectUrl, redirectDepth + 1);
                        httpConnection.disconnect();
                        // Recursively follow redirect
                        return fetchNewsFromRss(redirectUrl, source, maxItems, redirectDepth + 1);
                    }
                }
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    log.warn("HTTP error response {} for RSS feed: {}", responseCode, feedUrl);
                    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        log.error("RSS feed not found (404): {}", feedUrl);
                        return newsList; // Return empty list
                    }
                }
            }
            
            // Read RSS feed
            try (InputStream inputStream = connection.getInputStream()) {
                // Read entire content as string for preprocessing
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                String xmlContent = content.toString();
                
                // For Google News RSS (Atom format), try direct parsing first
                // Google News RSS is well-formed Atom XML, so it should parse directly
                boolean isGoogleNews = feedUrl != null && feedUrl.contains("news.google.com");
                if (isGoogleNews) {
                    try {
                        log.debug("Attempting direct parse for Google News RSS (Atom format)...");
                        InputStream directStream = new java.io.ByteArrayInputStream(
                            xmlContent.getBytes(StandardCharsets.UTF_8));
                        XmlReader directReader = new XmlReader(directStream, true);
                        SyndFeedInput directInput = new SyndFeedInput();
                        directInput.setPreserveWireFeed(true);
                        SyndFeed feed = directInput.build(directReader);
                        
                        // Success! Process the feed
                        log.info("✅ Successfully parsed Google News RSS (Atom format) directly");
                        return processFeed(feed, source, maxItems);
                    } catch (Exception e) {
                        log.warn("Direct parse failed for Google News RSS, trying with preprocessing: {}", e.getMessage());
                        // Fall through to preprocessing
                    }
                }
                
                // Preprocess XML to fix common issues (for non-Google News feeds or if direct parse failed)
                // Preprocess XML to fix common issues
                
                // 1. Remove DOCTYPE declarations completely (they cause parsing errors)
                xmlContent = xmlContent.replaceAll("<!DOCTYPE[^>]*>", "");
                
                // 2. Fix malformed xmlns:media attributes (may contain < character)
                xmlContent = xmlContent.replaceAll("xmlns:media=\"[^\"]*<[^\"]*\"", "xmlns:media=\"http://search.yahoo.com/mrss/\"");
                
                // 3. Fix duplicate xmlns:media attributes - keep only the first one
                // Find the RSS opening tag
                int rssStart = xmlContent.indexOf("<rss");
                if (rssStart >= 0) {
                    int rssEnd = xmlContent.indexOf(">", rssStart);
                    if (rssEnd > rssStart) {
                        String rssTag = xmlContent.substring(rssStart, rssEnd + 1);
                        
                        // Check if there are multiple xmlns:media declarations
                        String[] parts = rssTag.split("xmlns:media");
                        if (parts.length > 2) {
                            // Find first xmlns:media declaration
                            int firstMediaStart = rssTag.indexOf("xmlns:media");
                            int firstMediaEnd = rssTag.indexOf("\"", firstMediaStart + 11) + 1;
                            if (firstMediaEnd > firstMediaStart) {
                                String firstMedia = rssTag.substring(firstMediaStart, firstMediaEnd);
                                
                                // Remove all xmlns:media declarations
                                String cleanedTag = rssTag.replaceAll("xmlns:media=\"[^\"]+\"\\s*", "");
                                
                                // Insert first xmlns:media back (before the closing >)
                                int insertPos = cleanedTag.lastIndexOf(">");
                                String fixedTag = cleanedTag.substring(0, insertPos) + " " + firstMedia + 
                                    cleanedTag.substring(insertPos);
                                
                                xmlContent = xmlContent.substring(0, rssStart) + fixedTag + 
                                    xmlContent.substring(rssEnd + 1);
                            }
                        }
                    }
                }
                
                // 4. Fix malformed crossorigin attributes (must be followed by =)
                // Pattern: crossorigin without = (e.g., crossorigin crossorigin="anonymous")
                xmlContent = xmlContent.replaceAll("crossorigin\\s+crossorigin=", "crossorigin=");
                xmlContent = xmlContent.replaceAll("crossorigin(?!\\s*[=])", "crossorigin=\"anonymous\"");
                
                // 5. Fix unclosed HTML tags in XML (e.g., <hr> should be <hr/>, <br> should be <br/>)
                xmlContent = xmlContent.replaceAll("<hr(?!\\s*/>)", "<hr/");
                xmlContent = xmlContent.replaceAll("<hr\\s*>", "<hr/>");
                xmlContent = xmlContent.replaceAll("<br(?!\\s*/>)", "<br/");
                xmlContent = xmlContent.replaceAll("<br\\s*>", "<br/>");
                
                // 6. Fix malformed link tags - ensure all link tags are self-closing
                // Link tags in XML/RSS should be self-closing: <link ... /> not <link ...></link>
                
                // Step 1: Fix link tags that are not properly closed (missing > or />)
                // Pattern: <link ... (not followed by > or />, but followed by whitespace or new tag)
                java.util.regex.Pattern linkPattern = java.util.regex.Pattern.compile(
                    "<link([^>]*?)(?<!>)(?!/>)(?=\\s|<[^/]|$)", 
                    java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE
                );
                java.util.regex.Matcher linkMatcher = linkPattern.matcher(xmlContent);
                StringBuffer fixedContent = new StringBuffer();
                while (linkMatcher.find()) {
                    String attributes = linkMatcher.group(1);
                    // Clean up attributes - remove any trailing whitespace or incomplete attributes
                    attributes = attributes.trim();
                    // If attributes don't end with a quote, try to fix it
                    if (!attributes.isEmpty() && !attributes.endsWith("\"") && !attributes.endsWith("'")) {
                        // Check if last attribute is incomplete (e.g., "href=" without value)
                        if (attributes.matches(".*=\\s*$")) {
                            // Remove incomplete attribute
                            attributes = attributes.replaceAll("=\\s*$", "");
                        }
                    }
                    // Make it self-closing
                    linkMatcher.appendReplacement(fixedContent, "<link" + attributes + " />");
                }
                linkMatcher.appendTail(fixedContent);
                xmlContent = fixedContent.toString();
                
                // Step 2: Convert all <link ... > to <link ... /> (self-closing)
                // This handles cases where link tags are written as <link ... > instead of <link ... />
                xmlContent = xmlContent.replaceAll("<link([^>]*?)>", "<link$1 />");
                
                // Step 3: Fix link tags with malformed crossorigin attributes
                xmlContent = xmlContent.replaceAll("<link([^>]*?)crossorigin([^>]*?)(?!/>)(?![^<]*>)", "<link$1crossorigin=\"anonymous\"$2 />");
                
                // Step 4: Remove any remaining malformed link tags (as last resort)
                // This removes link tags that can't be fixed, which is better than failing completely
                xmlContent = xmlContent.replaceAll("<link[^>]*?(?<!>)(?!/>)(?=\\s|<[^/]|$)", "");
                
                // 7. Fix xmlns:media attributes with empty or invalid values
                // Pattern: xmlns:media="" or xmlns:media="<" or other invalid values
                xmlContent = xmlContent.replaceAll("xmlns:media=\"[^\"]*\"", "xmlns:media=\"http://search.yahoo.com/mrss/\"");
                // Remove duplicate xmlns:media declarations (keep only the first)
                if (xmlContent.contains("xmlns:media")) {
                    int firstMediaIndex = xmlContent.indexOf("xmlns:media");
                    if (firstMediaIndex >= 0) {
                        String beforeFirst = xmlContent.substring(0, firstMediaIndex);
                        String fromFirst = xmlContent.substring(firstMediaIndex);
                        // Find the end of the first xmlns:media declaration
                        int firstMediaEnd = fromFirst.indexOf("\"", fromFirst.indexOf("xmlns:media") + 11) + 1;
                        if (firstMediaEnd > 0) {
                            String firstMediaDecl = fromFirst.substring(0, firstMediaEnd);
                            String afterFirst = fromFirst.substring(firstMediaEnd);
                            // Remove all other xmlns:media declarations
                            afterFirst = afterFirst.replaceAll("xmlns:media=\"[^\"]*\"", "");
                            xmlContent = beforeFirst + firstMediaDecl + afterFirst;
                        }
                    }
                }
                
                // 8. Remove any HTML comments that might interfere
                xmlContent = xmlContent.replaceAll("<!--[^>]*-->", "");
                
                // 9. Fix any remaining malformed XML attributes
                // Remove any attribute values that contain unescaped < or >
                xmlContent = xmlContent.replaceAll("(\\w+)=\"([^\"]*<[^\"]*)\"", "$1=\"\"");
                xmlContent = xmlContent.replaceAll("(\\w+)=\"([^\"]*>[^\"]*)\"", "$1=\"\"");
                
                // 10. Clean up XML prolog (remove BOM and ensure proper format)
                // Remove BOM if present
                if (xmlContent.startsWith("\uFEFF")) {
                    xmlContent = xmlContent.substring(1);
                }
                // Ensure XML declaration is properly formatted and followed by root element
                // Remove any content between XML declaration and root element (including whitespace, comments, etc.)
                // More aggressive cleanup: find XML declaration and root element, remove everything in between
                int xmlDeclStart = xmlContent.indexOf("<?xml");
                if (xmlDeclStart >= 0) {
                    int xmlDeclEnd = xmlContent.indexOf("?>", xmlDeclStart);
                    if (xmlDeclEnd > xmlDeclStart) {
                        // Find the first root element (not a comment or processing instruction)
                        int rootElementStart = xmlDeclEnd + 2;
                        while (rootElementStart < xmlContent.length()) {
                            char ch = xmlContent.charAt(rootElementStart);
                            if (ch == '<') {
                                // Check if it's a comment or processing instruction
                                if (rootElementStart + 1 < xmlContent.length()) {
                                    char nextCh = xmlContent.charAt(rootElementStart + 1);
                                    if (nextCh == '!' || nextCh == '?') {
                                        // Skip comments and processing instructions
                                        if (nextCh == '!') {
                                            // Skip comment: <!-- ... -->
                                            int commentEnd = xmlContent.indexOf("-->", rootElementStart + 2);
                                            if (commentEnd > 0) {
                                                rootElementStart = commentEnd + 3;
                                                continue;
                                            }
                                        } else {
                                            // Skip processing instruction: <? ... ?>
                                            int piEnd = xmlContent.indexOf("?>", rootElementStart + 2);
                                            if (piEnd > 0) {
                                                rootElementStart = piEnd + 2;
                                                continue;
                                            }
                                        }
                                    }
                                }
                                // Found root element
                                String xmlDecl = xmlContent.substring(xmlDeclStart, xmlDeclEnd + 2);
                                String rootElementAndRest = xmlContent.substring(rootElementStart);
                                // Ensure there's only a single newline between XML declaration and root element
                                xmlContent = xmlDecl + "\n" + rootElementAndRest;
                                break;
                            } else if (!Character.isWhitespace(ch)) {
                                // Non-whitespace character before root element - remove it
                                String xmlDecl = xmlContent.substring(xmlDeclStart, xmlDeclEnd + 2);
                                String rootElementAndRest = xmlContent.substring(rootElementStart);
                                xmlContent = xmlDecl + "\n" + rootElementAndRest;
                                break;
                            }
                            rootElementStart++;
                        }
                    }
                }
                // Fallback: use regex if the above method didn't work
                xmlContent = xmlContent.replaceAll("(?s)(<\\?xml[^>]*\\?>)\\s*[^<]*?(<[^?!])", "$1\n$2");
                
                // Create InputStream from preprocessed content
                InputStream processedStream = new java.io.ByteArrayInputStream(
                    xmlContent.getBytes(StandardCharsets.UTF_8));
                
                // Use XmlReader with lenient mode for tolerant parsing
                XmlReader reader = new XmlReader(processedStream, true); // lenient = true
                
                // Create SyndFeedInput
                SyndFeedInput input = new SyndFeedInput();
                input.setPreserveWireFeed(true);
                
                SyndFeed feed;
                try {
                    feed = input.build(reader);
                } catch (Exception e) {
                    // If parsing still fails, try more aggressive cleanup
                    log.warn("First parse attempt failed for {}, trying aggressive cleanup: {}", feedUrl, e.getMessage());
                    
                    // Step 1: Remove all link tags completely (they're usually in <head> and not critical for RSS content)
                    xmlContent = xmlContent.replaceAll("(?s)<link[^>]*>", "");
                    xmlContent = xmlContent.replaceAll("(?s)<link[^>]*/>", "");
                    
                    // Step 2: Remove entire <head> section if it exists (link tags are usually there)
                    xmlContent = xmlContent.replaceAll("(?s)<head>.*?</head>", "<head></head>");
                    
                    // Step 3: Fix any remaining xmlns:media issues
                    xmlContent = xmlContent.replaceAll("xmlns:media=\"[^\"]*\"", "xmlns:media=\"http://search.yahoo.com/mrss/\"");
                    
                    // Step 4: Remove any remaining malformed tags
                    xmlContent = xmlContent.replaceAll("<([^>]+)(?<!>)(?!/>)(?=\\s|<[^/]|$)", "");
                    
                    // Step 5: Re-clean XML prolog after aggressive cleanup (critical to fix "前言中不允许有内容" error)
                    if (xmlContent.startsWith("\uFEFF")) {
                        xmlContent = xmlContent.substring(1);
                    }
                    int xmlDeclStart1 = xmlContent.indexOf("<?xml");
                    if (xmlDeclStart1 >= 0) {
                        int xmlDeclEnd1 = xmlContent.indexOf("?>", xmlDeclStart1);
                        if (xmlDeclEnd1 > xmlDeclStart1) {
                            int rootElementStart1 = xmlDeclEnd1 + 2;
                            while (rootElementStart1 < xmlContent.length()) {
                                char ch = xmlContent.charAt(rootElementStart1);
                                if (ch == '<') {
                                    if (rootElementStart1 + 1 < xmlContent.length()) {
                                        char nextCh = xmlContent.charAt(rootElementStart1 + 1);
                                        if (nextCh == '!' || nextCh == '?') {
                                            if (nextCh == '!') {
                                                int commentEnd = xmlContent.indexOf("-->", rootElementStart1 + 2);
                                                if (commentEnd > 0) {
                                                    rootElementStart1 = commentEnd + 3;
                                                    continue;
                                                }
                                            } else {
                                                int piEnd = xmlContent.indexOf("?>", rootElementStart1 + 2);
                                                if (piEnd > 0) {
                                                    rootElementStart1 = piEnd + 2;
                                                    continue;
                                                }
                                            }
                                        }
                                    }
                                    String xmlDecl = xmlContent.substring(xmlDeclStart1, xmlDeclEnd1 + 2);
                                    String rootElementAndRest = xmlContent.substring(rootElementStart1);
                                    xmlContent = xmlDecl + "\n" + rootElementAndRest;
                                    break;
                                } else if (!Character.isWhitespace(ch)) {
                                    String xmlDecl = xmlContent.substring(xmlDeclStart1, xmlDeclEnd1 + 2);
                                    String rootElementAndRest = xmlContent.substring(rootElementStart1);
                                    xmlContent = xmlDecl + "\n" + rootElementAndRest;
                                    break;
                                }
                                rootElementStart1++;
                            }
                        }
                    }
                    // Fallback regex cleanup
                    xmlContent = xmlContent.replaceAll("(?s)(<\\?xml[^>]*\\?>)\\s*[^<]*?(<[^?!])", "$1\n$2");
                    
                    processedStream = new java.io.ByteArrayInputStream(
                        xmlContent.getBytes(StandardCharsets.UTF_8));
                    reader = new XmlReader(processedStream, true);
                    
                    try {
                        feed = input.build(reader);
                        log.info("Successfully parsed RSS feed after aggressive cleanup: {}", feedUrl);
                    } catch (Exception e2) {
                        // If still fails, try removing the entire <head> section completely
                        log.warn("Second parse attempt also failed for {}, trying to remove <head> section completely: {}", feedUrl, e2.getMessage());
                        xmlContent = xmlContent.replaceAll("(?s)<head>.*?</head>", "");
                        
                        // Clean up XML prolog after removing <head> - critical to fix "前言中不允许有内容" error
                        // Remove BOM if present
                        if (xmlContent.startsWith("\uFEFF")) {
                            xmlContent = xmlContent.substring(1);
                        }
                        
                        // Find XML declaration and root element, remove everything in between
                        // More thorough cleanup: skip comments and processing instructions
                        int xmlDeclStart2 = xmlContent.indexOf("<?xml");
                        if (xmlDeclStart2 >= 0) {
                            int xmlDeclEnd2 = xmlContent.indexOf("?>", xmlDeclStart2);
                            if (xmlDeclEnd2 > xmlDeclStart2) {
                                int rootElementStart2 = xmlDeclEnd2 + 2;
                                while (rootElementStart2 < xmlContent.length()) {
                                    char ch = xmlContent.charAt(rootElementStart2);
                                    if (ch == '<') {
                                        // Check if it's a comment or processing instruction
                                        if (rootElementStart2 + 1 < xmlContent.length()) {
                                            char nextCh = xmlContent.charAt(rootElementStart2 + 1);
                                            if (nextCh == '!' || nextCh == '?') {
                                                // Skip comments and processing instructions
                                                if (nextCh == '!') {
                                                    // Skip comment: <!-- ... -->
                                                    int commentEnd = xmlContent.indexOf("-->", rootElementStart2 + 2);
                                                    if (commentEnd > 0) {
                                                        rootElementStart2 = commentEnd + 3;
                                                        continue;
                                                    }
                                                } else {
                                                    // Skip processing instruction: <? ... ?>
                                                    int piEnd = xmlContent.indexOf("?>", rootElementStart2 + 2);
                                                    if (piEnd > 0) {
                                                        rootElementStart2 = piEnd + 2;
                                                        continue;
                                                    }
                                                }
                                            }
                                        }
                                        // Found root element
                                        String xmlDecl = xmlContent.substring(xmlDeclStart2, xmlDeclEnd2 + 2);
                                        String rootElementAndRest = xmlContent.substring(rootElementStart2);
                                        // Ensure there's only a single newline between XML declaration and root element
                                        xmlContent = xmlDecl + "\n" + rootElementAndRest;
                                        break;
                                    } else if (!Character.isWhitespace(ch)) {
                                        // Non-whitespace character before root element - remove it
                                        String xmlDecl = xmlContent.substring(xmlDeclStart2, xmlDeclEnd2 + 2);
                                        String rootElementAndRest = xmlContent.substring(rootElementStart2);
                                        xmlContent = xmlDecl + "\n" + rootElementAndRest;
                                        break;
                                    }
                                    rootElementStart2++;
                                }
                            }
                        }
                        // Fallback: use regex to clean up
                        xmlContent = xmlContent.replaceAll("(?s)(<\\?xml[^>]*\\?>)\\s*[^<]*?(<[^?!])", "$1\n$2");
                        
                        processedStream = new java.io.ByteArrayInputStream(
                            xmlContent.getBytes(StandardCharsets.UTF_8));
                        reader = new XmlReader(processedStream, true);
                        feed = input.build(reader);
                    }
                }
                
                log.info("Successfully parsed RSS feed: {} (title: {}, entries: {})", 
                        feedUrl, feed.getTitle(), feed.getEntries().size());
                
                newsList = processFeed(feed, source, maxItems);
                
                log.info("Successfully fetched {} news items from RSS feed {}", newsList.size(), feedUrl);
            }
            
        } catch (FileNotFoundException e) {
            log.error("RSS feed file not found (404): {} - {}", feedUrl, e.getMessage());
        } catch (ParsingFeedException e) {
            log.error("Failed to parse RSS feed XML: {} - Error: {}", feedUrl, e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
            }
        } catch (javax.net.ssl.SSLHandshakeException e) {
            log.error("SSL handshake failed for RSS feed: {} - {}", feedUrl, e.getMessage());
            log.error("This may indicate an SSL certificate issue. Consider updating the certificate or using a different feed URL.");
        } catch (java.net.UnknownHostException e) {
            log.error("Unknown host for RSS feed: {} - {}", feedUrl, e.getMessage());
        } catch (java.net.SocketTimeoutException e) {
            log.error("Connection timeout for RSS feed: {} - {}", feedUrl, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to fetch news from RSS feed: {} - {} (Exception type: {})", 
                    feedUrl, e.getMessage(), e.getClass().getSimpleName(), e);
        }
        
        return newsList;
    }

    /**
     * Process parsed feed and convert entries to News objects
     */
    private List<News> processFeed(SyndFeed feed, String source, int maxItems) {
        List<News> newsList = new ArrayList<>();
                
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
                
        return newsList;
    }

    /**
     * Extract original media source from RSS entry
     * For Google News RSS, tries to extract the original media name from various fields
     * 
     * @param entry RSS entry
     * @param link Article URL
     * @return Original media name (e.g., "Bangkok Post", "The Nation"), or null if not found
     */
    private String extractOriginalMedia(SyndEntry entry, String link) {
        try {
            // Method 1: Try to get from entry source (if available)
            if (entry.getSource() != null && entry.getSource().getTitle() != null) {
                String sourceTitle = entry.getSource().getTitle();
                if (!sourceTitle.isEmpty() && !sourceTitle.equals("Google News")) {
                    return sourceTitle;
                }
            }
            
            // Method 2: Extract from Google News URL or link domain
            // Google News URLs often contain the original source in the format:
            // https://news.google.com/rss/articles/...?oc=5&hl=th&gl=TH&ceid=TH:th
            // Or the actual article URL might be embedded
            if (link != null) {
                try {
                    java.net.URL url = new java.net.URL(link);
                    String host = url.getHost();
                    
                    // Extract domain name and try to match known Thai media
                    // Examples: www.bangkokpost.com -> "Bangkok Post"
                    //           www.nationthailand.com -> "The Nation Thailand"
                    if (host.contains("bangkokpost.com")) {
                        return "Bangkok Post";
                    } else if (host.contains("nationthailand.com") || host.contains("nationmultimedia.com")) {
                        return "The Nation Thailand";
                    } else if (host.contains("matichon.co.th")) {
                        return "Matichon";
                    } else if (host.contains("khaosod.co.th")) {
                        return "Khaosod";
                    } else if (host.contains("thairath.co.th")) {
                        return "Thairath";
                    } else if (host.contains("sanook.com")) {
                        return "Sanook";
                    } else if (host.contains("thaipbs.or.th")) {
                        return "Thai PBS";
                    } else if (host.contains("thethaiger.com")) {
                        return "The Thaiger";
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse URL for source extraction: {}", link);
                }
            }
            
            // Method 3: Extract from title (some feeds include source in title like "Source: Title")
            String title = entry.getTitle();
            if (title != null) {
                // Pattern: "Source Name: Article Title" or "Article Title - Source Name"
                if (title.contains(" - ")) {
                    String[] parts = title.split(" - ", 2);
                    if (parts.length == 2) {
                        String possibleSource = parts[1].trim();
                        // Check if it looks like a source name (short, no special chars)
                        if (possibleSource.length() < 50 && !possibleSource.contains("http")) {
                            return possibleSource;
                        }
                    }
                }
                if (title.contains(": ")) {
                    String[] parts = title.split(": ", 2);
                    if (parts.length == 2) {
                        String possibleSource = parts[0].trim();
                        if (possibleSource.length() < 50 && !possibleSource.contains("http")) {
                            return possibleSource;
                        }
                    }
                }
            }
            
            // Method 4: Extract from description if it contains source info
            if (entry.getDescription() != null) {
                String description = entry.getDescription().getValue();
                if (description != null) {
                    // Look for patterns like "Source: ..." or "via ..."
                    java.util.regex.Pattern sourcePattern = java.util.regex.Pattern.compile(
                        "(?:Source|来源|via|from)[:：]\\s*([^<\\n]+)", 
                        java.util.regex.Pattern.CASE_INSENSITIVE
                    );
                    java.util.regex.Matcher matcher = sourcePattern.matcher(description);
                    if (matcher.find()) {
                        String foundSource = matcher.group(1).trim();
                        if (foundSource.length() < 50) {
                            return foundSource;
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract original media source: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch cover image URL from news article page
     * Extracts og:image or twitter:image meta tags for high-quality cover images
     * 
     * @param newsUrl News article URL
     * @return Cover image URL, or null if not found or failed
     */
    private String fetchCoverImage(String newsUrl) {
        try {
            // Google News links are redirect links, Jsoup automatically follows redirects
            // Must use User-Agent to avoid being blocked by some websites
            Document doc = Jsoup.connect(newsUrl)
                    .userAgent(USER_AGENT)
                    .timeout(5000) // 5 second timeout to prevent hanging
                    .followRedirects(true) // Follow redirects (important for Google News links)
                    .get();

            // Method 1: Try to get Open Graph image (high quality, preferred)
            Element ogImage = doc.select("meta[property=og:image]").first();
            if (ogImage != null) {
                String imageUrl = ogImage.attr("content");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Handle relative URLs
                    if (imageUrl.startsWith("//")) {
                        imageUrl = "https:" + imageUrl;
                    } else if (imageUrl.startsWith("/")) {
                        try {
                            URL url = new URL(newsUrl);
                            imageUrl = url.getProtocol() + "://" + url.getHost() + imageUrl;
                        } catch (Exception e) {
                            log.debug("Failed to resolve relative image URL: {}", imageUrl);
                        }
                    }
                    return imageUrl;
                }
            }

            // Method 2: Fallback to Twitter Card image
            Element twitterImage = doc.select("meta[name=twitter:image]").first();
            if (twitterImage != null) {
                String imageUrl = twitterImage.attr("content");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Handle relative URLs
                    if (imageUrl.startsWith("//")) {
                        imageUrl = "https:" + imageUrl;
                    } else if (imageUrl.startsWith("/")) {
                        try {
                            URL url = new URL(newsUrl);
                            imageUrl = url.getProtocol() + "://" + url.getHost() + imageUrl;
                        } catch (Exception e) {
                            log.debug("Failed to resolve relative image URL: {}", imageUrl);
                        }
                    }
                    return imageUrl;
                }
            }

            // Method 3: Try to find the first large image in article content
            // This is a fallback if meta tags are not available
            Element articleImage = doc.select("article img, .article img, .content img, main img").first();
            if (articleImage != null) {
                String imageUrl = articleImage.attr("src");
                if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("data:")) {
                    // Handle relative URLs
                    if (imageUrl.startsWith("//")) {
                        imageUrl = "https:" + imageUrl;
                    } else if (imageUrl.startsWith("/")) {
                        try {
                            URL url = new URL(newsUrl);
                            imageUrl = url.getProtocol() + "://" + url.getHost() + imageUrl;
                        } catch (Exception e) {
                            log.debug("Failed to resolve relative image URL: {}", imageUrl);
                        }
                    }
                    return imageUrl;
                }
            }

        } catch (java.net.SocketTimeoutException e) {
            log.debug("Timeout while fetching cover image from: {}", newsUrl);
        } catch (java.io.IOException e) {
            log.debug("IO error while fetching cover image from {}: {}", newsUrl, e.getMessage());
        } catch (Exception e) {
            // Don't log every failure as it's common (network timeout, anti-crawling, etc.)
            log.debug("Failed to fetch cover image from {}: {}", newsUrl, e.getMessage());
        }
        
        // Return null if no image found (don't use default image URL, let frontend handle it)
        return null;
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
            
            // Extract original media source from link or entry
            // For Google News RSS, try to extract original media name from URL or other fields
            String originalMedia = extractOriginalMedia(entry, link);
            String finalSource = source;
            if (originalMedia != null && !originalMedia.isEmpty()) {
                // Combine RSS source and original media: "Google News (Thailand) - Bangkok Post"
                finalSource = source + " - " + originalMedia;
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
            
            // Fetch cover image from the article page
            // Note: This may slow down the process, but provides high-quality images
            String coverImageUrl = null;
            if (link != null && !link.isEmpty()) {
                try {
                    coverImageUrl = fetchCoverImage(link);
                    if (coverImageUrl != null && !coverImageUrl.isEmpty()) {
                        log.debug("Fetched cover image for news: {} -> {}", title, coverImageUrl);
                    }
                } catch (Exception e) {
                    // Don't fail the entire process if image fetching fails
                    log.debug("Failed to fetch cover image for {}: {}", link, e.getMessage());
                }
            }
            
            News news = News.builder()
                    .title(title.trim())
                    .originalUrl(link)
                    .source(finalSource)
                    .summary(summary)
                    .coverImageUrl(coverImageUrl)
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
        
        // Group configs by source to handle multiple URLs for the same source
        java.util.Map<String, List<RssFeedConfig>> sourceGroups = new java.util.HashMap<>();
        for (RssFeedConfig config : feedConfigs) {
            sourceGroups.computeIfAbsent(config.getSource(), k -> new ArrayList<>()).add(config);
        }
        
        // For each source, try URLs until one succeeds
        for (java.util.Map.Entry<String, List<RssFeedConfig>> entry : sourceGroups.entrySet()) {
            String source = entry.getKey();
            List<RssFeedConfig> configs = entry.getValue();
            
            boolean success = false;
            for (RssFeedConfig config : configs) {
            try {
                List<News> news = fetchNewsFromRss(config.getUrl(), config.getSource(), maxItemsPerFeed);
                    if (!news.isEmpty()) {
                allNews.addAll(news);
                        success = true;
                        log.debug("Successfully fetched {} news items from {} using URL: {}", 
                                news.size(), source, config.getUrl());
                        break; // Use first successful URL
                    }
            } catch (Exception e) {
                    log.debug("Failed to fetch from {} (URL: {}): {}", source, config.getUrl(), e.getMessage());
                    // Continue to next URL
                }
            }
            
            if (!success && configs.size() > 0) {
                log.warn("All URLs failed for source: {} (tried {} URLs)", source, configs.size());
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

