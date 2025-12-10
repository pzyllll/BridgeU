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
}

