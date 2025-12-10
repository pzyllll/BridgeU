-- 检查新闻翻译数据
-- 查找标题仍然是泰语的新闻

SELECT 
    id,
    title,
    LEFT(title_zh, 100) as title_zh_preview,
    LEFT(title_en, 100) as title_en_preview,
    LEFT(summary_zh, 100) as summary_zh_preview,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(title_en) as title_en_length,
    LENGTH(summary_zh) as summary_zh_length,
    LENGTH(summary_en) as summary_en_length,
    source,
    create_time
FROM news
WHERE title LIKE '%ไชยยงค์%'
   OR title LIKE '%วอลเลย์บอล%'
   OR title LIKE '%สองพี่น้อง%'
   OR title LIKE '%ก้อนหิน%'
   OR title LIKE '%ไดโนเสาร์%'
ORDER BY create_time DESC
LIMIT 20;

-- 检查所有今天创建的新闻的翻译状态
SELECT 
    COUNT(*) as total_news,
    SUM(CASE WHEN title_zh IS NOT NULL AND title_zh != '' THEN 1 ELSE 0 END) as has_title_zh,
    SUM(CASE WHEN title_en IS NOT NULL AND title_en != '' THEN 1 ELSE 0 END) as has_title_en,
    SUM(CASE WHEN summary_zh IS NOT NULL AND summary_zh != '' THEN 1 ELSE 0 END) as has_summary_zh,
    SUM(CASE WHEN summary_en IS NOT NULL AND summary_en != '' THEN 1 ELSE 0 END) as has_summary_en,
    SUM(CASE WHEN title LIKE '%[ก-๙]%' THEN 1 ELSE 0 END) as thai_title_count
FROM news
WHERE DATE(create_time) = CURDATE();

