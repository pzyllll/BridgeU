-- 检查 news 表的翻译数据
-- 用于诊断每日简报语言显示问题

USE bridgeu;

-- 1. 检查 news 表的总数和翻译字段的填充情况
SELECT 
    COUNT(*) as total_news,
    COUNT(title_zh) as has_title_zh,
    COUNT(title_en) as has_title_en,
    COUNT(summary_zh) as has_summary_zh,
    COUNT(summary_en) as has_summary_en,
    COUNT(CASE WHEN title_zh IS NOT NULL AND title_en IS NOT NULL THEN 1 END) as has_both_titles,
    COUNT(CASE WHEN summary_zh IS NOT NULL AND summary_en IS NOT NULL THEN 1 END) as has_both_summaries
FROM news;

-- 2. 查看今天的新闻及其翻译情况（最近 10 条）
SELECT 
    id,
    LEFT(title, 50) as title,
    CASE 
        WHEN title_zh IS NULL OR title_zh = '' THEN '❌ 无中文标题'
        ELSE CONCAT('✅ 有中文标题: ', LEFT(title_zh, 30), '...')
    END as title_zh_status,
    CASE 
        WHEN title_en IS NULL OR title_en = '' THEN '❌ 无英文标题'
        ELSE CONCAT('✅ 有英文标题: ', LEFT(title_en, 30), '...')
    END as title_en_status,
    CASE 
        WHEN summary_zh IS NULL OR summary_zh = '' THEN '❌ 无中文摘要'
        ELSE CONCAT('✅ 有中文摘要 (长度: ', LENGTH(summary_zh), ')')
    END as summary_zh_status,
    CASE 
        WHEN summary_en IS NULL OR summary_en = '' THEN '❌ 无英文摘要'
        ELSE CONCAT('✅ 有英文摘要 (长度: ', LENGTH(summary_en), ')')
    END as summary_en_status,
    create_time
FROM news
ORDER BY create_time DESC
LIMIT 10;

-- 3. 统计没有翻译的新闻数量
SELECT 
    COUNT(*) as news_without_translations
FROM news
WHERE (title_zh IS NULL OR title_zh = '')
   AND (title_en IS NULL OR title_en = '')
   AND (summary_zh IS NULL OR summary_zh = '')
   AND (summary_en IS NULL OR summary_en = '');

