package com.globalbuddy.repository;

import com.globalbuddy.model.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * News Data Access Layer
 */
@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * Find news by original URL (for deduplication)
     * 
     * @param originalUrl Original article URL
     * @return News object
     */
    Optional<News> findByOriginalUrl(String originalUrl);

    /**
     * Find news list by source website
     * 
     * @param source Source website name
     * @return List of news items
     */
    List<News> findBySource(String source);

    /**
     * Find news by source website and title
     * 
     * @param source Source website name
     * @param title News title
     * @return News object
     */
    Optional<News> findBySourceAndTitle(String source, String title);

    /**
     * Query news within specified date range (with pagination)
     * 
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    Page<News> findByCreateTimeBetween(Date startDate, Date endDate, Pageable pageable);

    /**
     * Query today's news (with pagination)
     * Uses @Query annotation for custom query, queries news from 00:00:00 to 23:59:59 of today
     * 
     * @param startOfDay Start time of today (00:00:00)
     * @param endOfDay End time of today (23:59:59)
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE n.createTime >= :startOfDay AND n.createTime <= :endOfDay ORDER BY n.createTime DESC")
    Page<News> findTodayNews(@Param("startOfDay") Date startOfDay, 
                             @Param("endOfDay") Date endOfDay, 
                             Pageable pageable);

    /**
     * Query news by source and date range
     * Supports partial matching (LIKE) to handle sources like "Google News (Thailand) - Bangkok Post"
     * 
     * @param source Source website name (can be partial match, e.g., "Bangkok Post")
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE n.source LIKE CONCAT('%', :source, '%') AND n.publishDate >= :startDate AND n.publishDate <= :endDate ORDER BY n.createTime DESC")
    Page<News> findBySourceAndCreateTimeBetween(@Param("source") String source,
                                                 @Param("startDate") Date startDate,
                                                 @Param("endDate") Date endDate,
                                                 Pageable pageable);

    /**
     * Query news by date range only (no source filter)
     * 
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE n.publishDate >= :startDate AND n.publishDate <= :endDate ORDER BY n.createTime DESC")
    Page<News> findByCreateTimeBetweenOrdered(@Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate,
                                               Pageable pageable);

    /**
     * Get distinct news sources
     * 
     * @return List of distinct source names
     */
    @Query("SELECT DISTINCT n.source FROM News n WHERE n.source IS NOT NULL ORDER BY n.source")
    List<String> findDistinctSources();

    /**
     * Query news by keyword (searches in title, originalContent, summary, and translations)
     * with date range filter
     * 
     * @param keyword Search keyword
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE " +
           "(n.title LIKE CONCAT('%', :keyword, '%') OR " +
           "n.originalContent LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summary LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleEn LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryEn LIKE CONCAT('%', :keyword, '%')) AND " +
           "n.publishDate >= :startDate AND n.publishDate <= :endDate " +
           "ORDER BY n.createTime DESC")
    Page<News> findByKeywordAndCreateTimeBetween(@Param("keyword") String keyword,
                                                  @Param("startDate") Date startDate,
                                                  @Param("endDate") Date endDate,
                                                  Pageable pageable);

    /**
     * Query news by keyword, source, and date range
     * Searches in title, originalContent, summary, and translations
     * 
     * @param keyword Search keyword
     * @param source Source website name
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE " +
           "(n.title LIKE CONCAT('%', :keyword, '%') OR " +
           "n.originalContent LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summary LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleEn LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryEn LIKE CONCAT('%', :keyword, '%')) AND " +
           "n.source LIKE CONCAT('%', :source, '%') AND " +
           "n.publishDate >= :startDate AND n.publishDate <= :endDate " +
           "ORDER BY n.createTime DESC")
    Page<News> findByKeywordAndSourceAndCreateTimeBetween(@Param("keyword") String keyword,
                                                            @Param("source") String source,
                                                            @Param("startDate") Date startDate,
                                                            @Param("endDate") Date endDate,
                                                            Pageable pageable);

    /**
     * Query news by keyword only (no date filter)
     * Searches in title, originalContent, summary, and translations
     *
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE " +
           "(n.title LIKE CONCAT('%', :keyword, '%') OR " +
           "n.originalContent LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summary LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleEn LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryEn LIKE CONCAT('%', :keyword, '%')) " +
           "ORDER BY n.createTime DESC")
    Page<News> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Query news by keyword and source only (no date filter)
     * Searches in title, originalContent, summary, and translations
     *
     * @param keyword Search keyword
     * @param source Source website name
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE " +
           "(n.title LIKE CONCAT('%', :keyword, '%') OR " +
           "n.originalContent LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summary LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.titleEn LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryZh LIKE CONCAT('%', :keyword, '%') OR " +
           "n.summaryEn LIKE CONCAT('%', :keyword, '%')) AND " +
           "n.source LIKE CONCAT('%', :source, '%') " +
           "ORDER BY n.createTime DESC")
    Page<News> findByKeywordAndSource(@Param("keyword") String keyword,
                                      @Param("source") String source,
                                      Pageable pageable);

    /**
     * Query news by source only (no date filter)
     *
     * @param source Source website name
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE n.source LIKE CONCAT('%', :source, '%') ORDER BY n.createTime DESC")
    Page<News> findBySourceOrdered(@Param("source") String source, Pageable pageable);
}

