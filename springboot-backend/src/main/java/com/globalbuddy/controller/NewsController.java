package com.globalbuddy.controller;

import com.globalbuddy.dto.NewsBriefDTO;
import com.globalbuddy.model.News;
import com.globalbuddy.repository.NewsRepository;
import com.globalbuddy.service.LanguageDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * News RESTful Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository newsRepository;
    private final LanguageDetectionService languageDetectionService;

    /**
     * Get today's news briefing
     * Supports pagination, filtering by source and date, returns news list with summaries and original links
     * 
     * @param page Page number, starting from 0, default is 0
     * @param size Page size, default is 10
     * @param lang Language preference (zh/en), default is en
     * @param source News source filter (optional, e.g., "Bangkok Post")
     * @param startDate Start date filter (optional, format: yyyy-MM-dd)
     * @param endDate End date filter (optional, format: yyyy-MM-dd)
     * @return Paginated news briefing list
     */
    @GetMapping("/daily-briefing")
    public ResponseEntity<Map<String, Object>> getDailyBriefing(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "en") String lang,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        
        try {
            log.info("Fetching news briefing, page: {}, size: {}, lang: {}, source: {}, startDate: {}, endDate: {}, keyword: {}", 
                    page, size, lang, source, startDate, endDate, keyword);

            // Parse date filters
            Date startDateFilter = null;
            Date endDateFilter = null;
            ZoneId zoneId = ZoneId.systemDefault();
            boolean userProvidedStartDate = false;
            boolean userProvidedEndDate = false;
            boolean hasUserDateFilter = false;
            
            if (startDate != null && !startDate.isEmpty()) {
                try {
                    LocalDate localStartDate = LocalDate.parse(startDate);
                    startDateFilter = Date.from(localStartDate.atStartOfDay().atZone(zoneId).toInstant());
                    userProvidedStartDate = true;
                    hasUserDateFilter = true;
                    log.info("User provided startDate: {} -> {}", startDate, startDateFilter);
                } catch (Exception e) {
                    log.warn("Invalid startDate format: {}, ignoring filter", startDate, e);
                }
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                try {
                    LocalDate localEndDate = LocalDate.parse(endDate);
                    endDateFilter = Date.from(localEndDate.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant());
                    userProvidedEndDate = true;
                    hasUserDateFilter = true;
                    log.info("User provided endDate: {} -> {}", endDate, endDateFilter);
                } catch (Exception e) {
                    log.warn("Invalid endDate format: {}, ignoring filter", endDate, e);
                }
            }
            
            // Handle keyword search
            String keywordTrimmed = (keyword != null) ? keyword.trim() : null;
            boolean hasKeyword = keywordTrimmed != null && !keywordTrimmed.isEmpty();
            boolean hasSource = source != null && !source.isEmpty();
            boolean useDateFilter = hasUserDateFilter;

            // Default behavior when NO filters are provided (no date, no keyword, no source):
            // show ALL news (paginated) instead of forcing a recent date window.
            if (!hasUserDateFilter && !hasKeyword && !hasSource) {
                useDateFilter = false;
                log.debug("No filters provided, returning ALL news with pagination (no default date window).");
            } else if (hasUserDateFilter) {
                // If only startDate is provided, set endDate to today
                if (userProvidedStartDate && !userProvidedEndDate) {
                LocalDate today = LocalDate.now();
                endDateFilter = Date.from(today.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant());
                    log.info("Only startDate provided by user, setting endDate to today: {}", endDateFilter);
                } else if (!userProvidedStartDate && userProvidedEndDate && endDateFilter != null) {
                    // If only endDate is provided, set startDate to 30 days ago
                    LocalDate computedEndDate = LocalDate.ofInstant(endDateFilter.toInstant(), zoneId);
                    LocalDate computedStartDate = computedEndDate.minusDays(30);
                    startDateFilter = Date.from(computedStartDate.atStartOfDay().atZone(zoneId).toInstant());
                    log.info("Only endDate provided by user, setting startDate to 30 days before: {}", startDateFilter);
                }

                // Ensure we have both date filters at this point (fallback only if parsing failed)
            if (startDateFilter == null || endDateFilter == null) {
                LocalDate today = LocalDate.now();
                LocalDate sevenDaysAgo = today.minusDays(7);
                startDateFilter = Date.from(sevenDaysAgo.atStartOfDay().atZone(zoneId).toInstant());
                endDateFilter = Date.from(today.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant());
                    log.warn("Date parsing failed, using fallback date range: {} to {}", startDateFilter, endDateFilter);
                }
            }

            // Create pagination object, sorted by publish date descending
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishDate"));

            // Query news with filters
            Page<News> newsPage;
            
            if (hasKeyword) {
                if (hasSource) {
                    if (useDateFilter) {
                        // Filter by keyword, source, and date range on publishDate
                        newsPage = newsRepository.findByKeywordAndSourceAndPublishDateBetween(
                        keywordTrimmed, source, startDateFilter, endDateFilter, pageable);
                        log.info("Using filtered query with keyword: {}, source: {}, publish date range: {} to {}",
                            keywordTrimmed, source, startDateFilter, endDateFilter);
                    } else {
                        // Filter by keyword and source only (all historical news)
                        newsPage = newsRepository.findByKeywordAndSource(keywordTrimmed, source, pageable);
                        log.info("Using filtered query with keyword: {}, source: {} (no date range, search in ALL news)",
                                keywordTrimmed, source);
                    }
                } else {
                    if (useDateFilter) {
                        // Filter by keyword and publish date range
                        newsPage = newsRepository.findByKeywordAndPublishDateBetween(
                        keywordTrimmed, startDateFilter, endDateFilter, pageable);
                        log.info("Using filtered query with keyword: {}, publish date range: {} to {}",
                            keywordTrimmed, startDateFilter, endDateFilter);
                    } else {
                        // Filter by keyword only (all historical news)
                        newsPage = newsRepository.findByKeyword(keywordTrimmed, pageable);
                        log.info("Using filtered query with keyword: {} (no date range, search in ALL news)",
                                keywordTrimmed);
                    }
                }
            } else if (hasSource) {
                if (useDateFilter) {
                    // Filter by source and publish date range
                    newsPage = newsRepository.findBySourceAndPublishDateBetween(source, startDateFilter, endDateFilter, pageable);
                    log.info("Using filtered query with source: {}, publish date range: {} to {}", source, startDateFilter, endDateFilter);
                } else {
                    // Filter by source only (all historical news)
                    newsPage = newsRepository.findBySourceOrdered(source, pageable);
                    log.info("Using filtered query with source: {} (no date range, search in ALL news)", source);
                }
            } else {
                // No keyword/source filters; either user-provided date range OR no date filter (all news)
                if (useDateFilter) {
                    newsPage = newsRepository.findByPublishDateBetweenOrdered(startDateFilter, endDateFilter, pageable);
                    log.info("Using publish date range filter: {} to {}", startDateFilter, endDateFilter);
                } else {
                    newsPage = newsRepository.findAll(pageable);
                    log.info("No date filter provided, returning all news (paginated)");
                }
            }

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
            if (source != null) {
                response.put("filterSource", source);
            }
            if (startDate != null) {
                response.put("filterStartDate", startDate);
            }
            if (endDate != null) {
                response.put("filterEndDate", endDate);
            }

            log.info("Successfully fetched {} news items with filters", dtoPage.getTotalElements());

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
     * Ensures NO Thai content is displayed on the website
     * 
     * @param news News entity
     * @param lang Language preference (zh/en)
     * @return News briefing DTO
     */
    private NewsBriefDTO convertToDTO(News news, String lang) {
        String title = news.getTitle();
        String summary = news.getSummary();
        
        // Check if original content contains Thai (strict check)
        boolean originalTitleIsThai = news.getTitle() != null && languageDetectionService.hasAnyThai(news.getTitle());
        boolean originalSummaryIsThai = news.getSummary() != null && languageDetectionService.hasAnyThai(news.getSummary());
        
        // Double check: also check if summary contains Thai even if initial check passed
        // This is important because summary might contain mixed content
        if (summary != null && !originalSummaryIsThai) {
            originalSummaryIsThai = languageDetectionService.hasAnyThai(summary);
        }

        log.debug("Converting news {} to DTO with lang: {}, hasTitleZh: {}, hasTitleEn: {}, hasSummaryZh: {}, hasSummaryEn: {}, originalTitleIsThai: {}", 
                news.getId(), lang, 
                news.getTitleZh() != null && !news.getTitleZh().isEmpty(),
                news.getTitleEn() != null && !news.getTitleEn().isEmpty(),
                news.getSummaryZh() != null && !news.getSummaryZh().isEmpty(),
                news.getSummaryEn() != null && !news.getSummaryEn().isEmpty(),
                originalTitleIsThai);

        // Return translated title and summary based on language preference
        // IMPORTANT: Never show Thai content on the website
        if ("zh".equals(lang)) {
            // Use Chinese translation if available
            if (news.getTitleZh() != null && !news.getTitleZh().isEmpty()) {
                title = news.getTitleZh();
                log.info("✅ Using Chinese title translation for news {}", news.getId());
            } else {
                // No Chinese translation available
                if (originalTitleIsThai) {
                    // If original is Thai, NEVER show it - use English or placeholder
                if (news.getTitleEn() != null && !news.getTitleEn().isEmpty()) {
                    title = news.getTitleEn();
                    log.warn("⚠️ Chinese title not available, using English fallback for Thai news: {}", news.getId());
                } else {
                    title = "[新闻标题翻译中...]";
                    log.error("❌ No translation available for Thai news title: {}", news.getId());
                }
            } else {
                    // Original is not Thai (English or other), but user selected Chinese
                    // Use English translation if available, otherwise use placeholder
                    if (news.getTitleEn() != null && !news.getTitleEn().isEmpty()) {
                        title = news.getTitleEn();
                        log.warn("⚠️ Chinese title not available for news: {}, using English fallback", news.getId());
                    } else {
                        // Check if original contains any Thai characters (double check)
                        if (languageDetectionService.hasAnyThai(title)) {
                            title = "[新闻标题翻译中...]";
                            log.error("❌ Original title contains Thai, but no translation available: {}", news.getId());
                        } else {
                            // Original is English/other, use it as fallback
                log.debug("⚠️ Chinese title translation not available for news: {}, using original (non-Thai)", news.getId());
                        }
                    }
                }
            }
            
            if (news.getSummaryZh() != null && !news.getSummaryZh().isEmpty()) {
                summary = news.getSummaryZh();
                log.info("✅ Using Chinese summary translation for news {}", news.getId());
            } else {
                // No Chinese translation available
                if (originalSummaryIsThai) {
                    // If original is Thai, NEVER show it - use English or placeholder
                if (news.getSummaryEn() != null && !news.getSummaryEn().isEmpty()) {
                    summary = news.getSummaryEn();
                    log.warn("⚠️ Chinese summary not available, using English fallback for Thai news: {}", news.getId());
                } else {
                    summary = "[新闻内容翻译中...]";
                    log.error("❌ No translation available for Thai news summary: {}", news.getId());
                }
            } else {
                    // Original is not Thai (English or other), but user selected Chinese
                    // Use English translation if available, otherwise use placeholder
                    if (news.getSummaryEn() != null && !news.getSummaryEn().isEmpty()) {
                        summary = news.getSummaryEn();
                        log.warn("⚠️ Chinese summary not available for news: {}, using English fallback", news.getId());
                    } else {
                        // Check if original contains any Thai characters (double check)
                        if (languageDetectionService.hasAnyThai(summary)) {
                            summary = "[新闻内容翻译中...]";
                            log.error("❌ Original summary contains Thai, but no translation available: {}", news.getId());
                        } else {
                            // Original is English/other, use it as fallback
                log.debug("⚠️ Chinese summary translation not available for news: {}, using original (non-Thai)", news.getId());
                        }
                    }
                }
            }
        } else if ("en".equals(lang)) {
            // Use English translation if available
            if (news.getTitleEn() != null && !news.getTitleEn().isEmpty()) {
                title = news.getTitleEn();
                log.info("✅ Using English title translation for news {}", news.getId());
            } else {
                // Check if original content is Chinese
                boolean originalTitleIsChinese = languageDetectionService.containsChinese(news.getTitle()) && 
                                                 !languageDetectionService.containsThai(news.getTitle());
                
                if (originalTitleIsThai) {
                // If original is Thai and no English translation, try Chinese as fallback
                if (news.getTitleZh() != null && !news.getTitleZh().isEmpty()) {
                    title = news.getTitleZh();
                    log.warn("⚠️ English title not available, using Chinese fallback for Thai news: {}", news.getId());
                } else {
                    title = "[News title translating...]";
                    log.error("❌ No translation available for Thai news title: {}", news.getId());
                }
                } else if (originalTitleIsChinese) {
                    // If original is Chinese and no English translation, show placeholder
                    title = "[News title translating...]";
                    log.warn("⚠️ English title not available for Chinese news: {}, showing placeholder", news.getId());
            } else {
                    // Original is English or other language, use it directly
                    log.debug("⚠️ English title translation not available for news: {}, using original (English/other)", news.getId());
                }
            }
            
            if (news.getSummaryEn() != null && !news.getSummaryEn().isEmpty()) {
                summary = news.getSummaryEn();
                log.info("✅ Using English summary translation for news {}", news.getId());
            } else {
                // Check if original content is Chinese
                boolean originalSummaryIsChinese = languageDetectionService.containsChinese(news.getSummary()) && 
                                                  !languageDetectionService.containsThai(news.getSummary());
                
                if (originalSummaryIsThai) {
                // If original is Thai and no English translation, try Chinese as fallback
                if (news.getSummaryZh() != null && !news.getSummaryZh().isEmpty()) {
                    summary = news.getSummaryZh();
                    log.warn("⚠️ English summary not available, using Chinese fallback for Thai news: {}", news.getId());
                } else {
                    summary = "[News content translating...]";
                    log.error("❌ No translation available for Thai news summary: {}", news.getId());
                }
                } else if (originalSummaryIsChinese) {
                    // If original is Chinese and no English translation, show placeholder
                    summary = "[News content translating...]";
                    log.warn("⚠️ English summary not available for Chinese news: {}, showing placeholder", news.getId());
            } else {
                    // Original is English or other language, use it directly
                    log.debug("⚠️ English summary translation not available for news: {}, using original (English/other)", news.getId());
                }
            }
        } else {
            // Default to English for unknown language preference
            log.warn("⚠️ Unknown language preference: {}, defaulting to English for news: {}", lang, news.getId());
            if (news.getTitleEn() != null && !news.getTitleEn().isEmpty()) {
                title = news.getTitleEn();
            } else if (originalTitleIsThai && news.getTitleZh() != null && !news.getTitleZh().isEmpty()) {
                title = news.getTitleZh();
            }
            if (news.getSummaryEn() != null && !news.getSummaryEn().isEmpty()) {
                summary = news.getSummaryEn();
            } else if (originalSummaryIsThai && news.getSummaryZh() != null && !news.getSummaryZh().isEmpty()) {
                summary = news.getSummaryZh();
            }
        }

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
                .coverImageUrl(news.getCoverImageUrl())
                .publishDate(news.getPublishDate())
                .createTime(news.getCreateTime())
                .build();
    }

    /**
     * Get single news detail by ID
     * 
     * @param id News ID
     * @param lang Language preference (zh/en), default is en
     * @return News detail with full content
     */
    @GetMapping("/daily-briefing/{id}")
    public ResponseEntity<Map<String, Object>> getNewsDetail(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        
        try {
            log.info("Fetching news detail, id: {}, lang: {}", id, lang);
            
            News news = newsRepository.findById(id).orElse(null);
            
            if (news == null) {
                log.warn("News not found with id: {}", id);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "News not found with id: " + id);
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            // Convert to DTO with language preference
            NewsBriefDTO dto = convertToDTO(news, lang);
            
            // Add original content (only if not Thai, since we don't display Thai content)
            String originalContent = news.getOriginalContent();
            String displayContent = null;
            
            if (originalContent != null && !originalContent.isEmpty()) {
                boolean originalContentIsThai = languageDetectionService.hasAnyThai(originalContent);
                
                if (!originalContentIsThai) {
                    // Only show original content if it's not Thai
                    displayContent = originalContent;
                }
                // If content is Thai, we don't display it (user should use translated summary)
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", dto);
            response.put("originalContent", displayContent); // null if Thai or empty
            
            log.info("Successfully fetched news detail: {}", id);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch news detail: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch news detail: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get list of available news sources for filtering
     * Extracts original media names from sources like "Google News (Thailand) - Bangkok Post"
     * Returns clean source names like "Bangkok Post", "The Nation Thailand", etc.
     * 
     * @return List of distinct original media sources
     */
    @GetMapping("/sources")
    public ResponseEntity<Map<String, Object>> getNewsSources() {
        try {
            List<String> allSources = newsRepository.findDistinctSources();
            
            // Extract original media names from sources
            // Format: "Google News (Thailand) - Bangkok Post" -> "Bangkok Post"
            // Format: "NewsAPI.org - The Nation Thailand" -> "The Nation Thailand"
            Set<String> originalMediaNames = new HashSet<>();
            
            for (String source : allSources) {
                if (source == null || source.isEmpty()) {
                    continue;
                }
                
                // Extract original media name (after " - ")
                String originalMedia = extractOriginalMediaName(source);
                if (originalMedia != null && !originalMedia.isEmpty()) {
                    originalMediaNames.add(originalMedia);
                } else {
                    // If no " - " separator, use the source as-is (for direct sources)
                    originalMediaNames.add(source);
                }
            }
            
            // Sort alphabetically
            List<String> sortedSources = new ArrayList<>(originalMediaNames);
            Collections.sort(sortedSources);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", sortedSources);
            log.info("Found {} distinct original media sources (from {} total sources)", 
                    sortedSources.size(), allSources.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch news sources: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch news sources: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Extract original media name from source string
     * Examples:
     *   "Google News (Thailand) - Bangkok Post" -> "Bangkok Post"
     *   "NewsAPI.org - The Nation Thailand" -> "The Nation Thailand"
     *   "Bangkok Post" -> "Bangkok Post"
     * 
     * @param source Full source string
     * @return Original media name, or null if not found
     */
    private String extractOriginalMediaName(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        
        // Look for " - " separator (common pattern: "RSS Source - Original Media")
        int separatorIndex = source.lastIndexOf(" - ");
        if (separatorIndex >= 0 && separatorIndex < source.length() - 3) {
            String originalMedia = source.substring(separatorIndex + 3).trim();
            if (!originalMedia.isEmpty()) {
                return originalMedia;
            }
        }
        
        // If no separator, return the source as-is
        return source;
    }
}

