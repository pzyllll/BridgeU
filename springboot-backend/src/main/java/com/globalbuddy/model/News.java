package com.globalbuddy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * News Entity
 * Used to store crawled news information and AI-generated summaries
 */
@Entity
@Table(name = "news")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    /**
     * Primary key ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * News title
     */
    private String title;

    /**
     * Original content
     */
    @Column(columnDefinition = "TEXT")
    private String originalContent;

    /**
     * AI-generated summary
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * Chinese translation of title
     */
    @Column(name = "title_zh", length = 500)
    private String titleZh;

    /**
     * English translation of title
     */
    @Column(name = "title_en", length = 500)
    private String titleEn;

    /**
     * Chinese translation of summary
     */
    @Column(name = "summary_zh", columnDefinition = "TEXT")
    private String summaryZh;

    /**
     * English translation of summary
     */
    @Column(name = "summary_en", columnDefinition = "TEXT")
    private String summaryEn;

    /**
     * Original article URL
     */
    @Column(length = 1000)
    private String originalUrl;

    /**
     * Source website name
     */
    private String source;

    /**
     * Publish date
     */
    private Date publishDate;

    /**
     * Crawl time
     */
    private Date createTime;
}

