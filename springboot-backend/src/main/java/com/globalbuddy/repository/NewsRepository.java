package com.globalbuddy.repository;

import com.globalbuddy.model.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
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
    @Query("SELECT n FROM News n WHERE n.publishDate >= :startOfDay AND n.publishDate <= :endOfDay ORDER BY n.publishDate DESC")
    Page<News> findTodayNews(@Param("startOfDay") Date startOfDay, 
                             @Param("endOfDay") Date endOfDay, 
                             Pageable pageable);

    /**
     * Query news by date range only (no source filter)
     * 
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE n.publishDate >= :startDate AND n.publishDate <= :endDate ORDER BY n.publishDate DESC")
    Page<News> findByPublishDateBetweenOrdered(@Param("startDate") Date startDate,
                                               @Param("endDate") Date endDate,
                                               Pageable pageable);

    /**
     * Query news by keyword (searches in title, originalContent, summary, and translations)
     * with date range filter
     * Case-insensitive partial match as per SRS-35
     * 
     * @param keyword Search keyword
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.originalContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.titleZh) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.titleEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.summaryZh) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.summaryEn) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "n.publishDate >= :startDate AND n.publishDate <= :endDate " +
           "ORDER BY n.publishDate DESC")
    Page<News> findByKeywordAndPublishDateBetween(@Param("keyword") String keyword,
                                                  @Param("startDate") Date startDate,
                                                  @Param("endDate") Date endDate,
                                                  Pageable pageable);

    /**
     * Query news by keyword only (no date filter)
     * Searches in title, originalContent, summary, and translations
     * Case-insensitive partial match as per SRS-35
     *
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return Paginated news list
     */
    @Query("SELECT n FROM News n WHERE " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.originalContent) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.titleZh) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.titleEn) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.summaryZh) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.summaryEn) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.publishDate DESC")
    Page<News> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Note: source-based filtering has been removed from the product.
}

