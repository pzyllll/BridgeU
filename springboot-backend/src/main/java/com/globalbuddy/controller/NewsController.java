package com.globalbuddy.controller;

import com.globalbuddy.dto.NewsBriefDTO;
import com.globalbuddy.model.News;
import com.globalbuddy.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * News RESTful Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository newsRepository;

    /**
     * Get today's news briefing
     * Supports pagination, returns news list with summaries and original links
     * 
     * @param page Page number, starting from 0, default is 0
     * @param size Page size, default is 10
     * @return Paginated news briefing list
     */
    @GetMapping("/daily-briefing")
    public ResponseEntity<Map<String, Object>> getDailyBriefing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        
        try {
            log.info("Fetching today's news briefing, page: {}, size: {}, lang: {}", page, size, lang);

            // Get start and end time of today (using system default timezone)
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59, 999_000_000); // Include milliseconds

            // Convert to Date objects using system default timezone
            ZoneId zoneId = ZoneId.systemDefault();
            Date startDate = Date.from(startOfDay.atZone(zoneId).toInstant());
            Date endDate = Date.from(endOfDay.atZone(zoneId).toInstant());

            log.debug("Querying today's news from {} to {} (timezone: {})", startDate, endDate, zoneId);

            // Create pagination object, sorted by creation time descending
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));

            // Query recent news (last 7 days)
            Page<News> newsPage = newsRepository.findTodayNews(startDate, endDate, pageable);

            // Convert to DTO with language preference
            Page<NewsBriefDTO> dtoPage = newsPage.map(news -> convertToDTO(news, lang));
            
            // Count how many news items have translations
            long zhTranslatedCount = 0;
            long enTranslatedCount = 0;
            for (NewsBriefDTO dto : dtoPage.getContent()) {
                if ("zh".equals(lang) && dto.getTitleZh() != null && !dto.getTitleZh().isEmpty()) {
                    zhTranslatedCount++;
                } else if ("en".equals(lang) && dto.getTitleEn() != null && !dto.getTitleEn().isEmpty()) {
                    enTranslatedCount++;
                }
            }
            if ("zh".equals(lang)) {
                log.info("News items with Chinese translation: {}/{}", zhTranslatedCount, dtoPage.getContent().size());
            } else if ("en".equals(lang)) {
                log.info("News items with English translation: {}/{}", enTranslatedCount, dtoPage.getContent().size());
            }

            // Build pagination info
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("page", dtoPage.getNumber());
            pagination.put("size", dtoPage.getSize());
            pagination.put("totalElements", dtoPage.getTotalElements());
            pagination.put("totalPages", dtoPage.getTotalPages());
            pagination.put("hasNext", dtoPage.hasNext());
            pagination.put("hasPrevious", dtoPage.hasPrevious());

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dtoPage.getContent());
            response.put("pagination", pagination);
            response.put("date", LocalDate.now().toString());

            log.info("Successfully fetched {} today's news items", dtoPage.getTotalElements());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch today's news briefing: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch news briefing: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Convert News entity to NewsBriefDTO
     * 
     * @param news News entity
     * @param lang Language preference (zh/en)
     * @return News briefing DTO
     */
    private NewsBriefDTO convertToDTO(News news, String lang) {
        String title = news.getTitle();
        String summary = news.getSummary();

        log.debug("Converting news {} to DTO with lang: {}, hasTitleZh: {}, hasTitleEn: {}, hasSummaryZh: {}, hasSummaryEn: {}", 
                news.getId(), lang, 
                news.getTitleZh() != null && !news.getTitleZh().isEmpty(),
                news.getTitleEn() != null && !news.getTitleEn().isEmpty(),
                news.getSummaryZh() != null && !news.getSummaryZh().isEmpty(),
                news.getSummaryEn() != null && !news.getSummaryEn().isEmpty());

        // Return translated title and summary based on language preference
        if ("zh".equals(lang)) {
            // Use Chinese translation if available
            if (news.getTitleZh() != null && !news.getTitleZh().isEmpty()) {
                title = news.getTitleZh();
                log.info("✅ Using Chinese title translation for news {}: {} -> {}", news.getId(), 
                        news.getTitle().substring(0, Math.min(50, news.getTitle().length())),
                        title.substring(0, Math.min(50, title.length())));
            } else {
                log.warn("⚠️ Chinese title translation not available for news: {} (original: {}), using original", 
                        news.getId(), news.getTitle().substring(0, Math.min(100, news.getTitle().length())));
            }
            if (news.getSummaryZh() != null && !news.getSummaryZh().isEmpty()) {
                summary = news.getSummaryZh();
                log.info("✅ Using Chinese summary translation for news {} (length: {})", news.getId(), summary.length());
            } else {
                log.warn("⚠️ Chinese summary translation not available for news: {} (original length: {}), using original", 
                        news.getId(), news.getSummary() != null ? news.getSummary().length() : 0);
            }
        } else if ("en".equals(lang)) {
            // Use English translation if available
            if (news.getTitleEn() != null && !news.getTitleEn().isEmpty()) {
                title = news.getTitleEn();
                log.info("✅ Using English title translation for news {}: {} -> {}", news.getId(),
                        news.getTitle().substring(0, Math.min(50, news.getTitle().length())),
                        title.substring(0, Math.min(50, title.length())));
            } else {
                log.warn("⚠️ English title translation not available for news: {} (original: {}), using original", 
                        news.getId(), news.getTitle().substring(0, Math.min(100, news.getTitle().length())));
            }
            if (news.getSummaryEn() != null && !news.getSummaryEn().isEmpty()) {
                summary = news.getSummaryEn();
                log.info("✅ Using English summary translation for news {} (length: {})", news.getId(), summary.length());
            } else {
                log.warn("⚠️ English summary translation not available for news: {} (original length: {}), using original", 
                        news.getId(), news.getSummary() != null ? news.getSummary().length() : 0);
            }
        } else {
            log.warn("⚠️ Unknown language preference: {}, using original content for news: {}", lang, news.getId());
        }
        // If lang is neither zh nor en, use original content

        return NewsBriefDTO.builder()
                .id(news.getId())
                .title(title)
                .summary(summary)
                .titleZh(news.getTitleZh())
                .titleEn(news.getTitleEn())
                .summaryZh(news.getSummaryZh())
                .summaryEn(news.getSummaryEn())
                .originalUrl(news.getOriginalUrl())
                .source(news.getSource())
                .publishDate(news.getPublishDate())
                .createTime(news.getCreateTime())
                .build();
    }
}

