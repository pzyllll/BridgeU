-- 查找这些未翻译成功的帖子
-- 根据标题查找

SELECT 
    id,
    title,
    LEFT(title_zh, 80) as title_zh_preview,
    LEFT(title_en, 80) as title_en_preview,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(title_en) as title_en_length,
    LENGTH(content_zh) as content_zh_length,
    LENGTH(content_en) as content_en_length,
    original_language,
    created_at
FROM community_posts
WHERE title LIKE '%ไชยยงค์%'
   OR title LIKE '%เถ้าแก่น้อย%'
   OR title LIKE '%สสส.%'
   OR title LIKE '%ICONIC Run Fest%'
   OR title LIKE '%โอ-ออ%'
   OR title LIKE '%นัทปง%'
ORDER BY created_at DESC;

