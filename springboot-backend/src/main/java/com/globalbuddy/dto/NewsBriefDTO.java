package com.globalbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 新闻简报 DTO
 * 用于返回新闻的简要信息（标题、摘要、原链接等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsBriefDTO {

    /**
     * 新闻 ID
     */
    private Long id;

    /**
     * 新闻标题
     */
    private String title;

    /**
     * AI 生成的总结
     */
    private String summary;

    /**
     * 中文标题翻译
     */
    private String titleZh;

    /**
     * 英文标题翻译
     */
    private String titleEn;

    /**
     * 中文摘要翻译
     */
    private String summaryZh;

    /**
     * 英文摘要翻译
     */
    private String summaryEn;

    /**
     * 原文链接
     */
    private String originalUrl;

    /**
     * 来源网站名称
     */
    private String source;

    /**
     * 发布时间
     */
    private Date publishDate;

    /**
     * 抓取时间
     */
    private Date createTime;
}

