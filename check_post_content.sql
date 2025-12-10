-- 检查帖子内容情况
-- 查看前10个帖子的内容字段

SELECT 
    id,
    title,
    LENGTH(body) as body_length,
    LENGTH(content_zh) as content_zh_length,
    LENGTH(content_en) as content_en_length,
    original_language,
    LEFT(body, 100) as body_preview,
    LEFT(content_zh, 100) as content_zh_preview,
    LEFT(content_en, 100) as content_en_preview,
    created_at
FROM community_posts
ORDER BY created_at DESC
LIMIT 10;

-- 统计内容为空的情况
SELECT 
    COUNT(*) as total_posts,
    SUM(CASE WHEN body IS NULL OR body = '' THEN 1 ELSE 0 END) as empty_body,
    SUM(CASE WHEN content_zh IS NULL OR content_zh = '' THEN 1 ELSE 0 END) as empty_content_zh,
    SUM(CASE WHEN content_en IS NULL OR content_en = '' THEN 1 ELSE 0 END) as empty_content_en,
    SUM(CASE WHEN (body IS NULL OR body = '') AND (content_zh IS NULL OR content_zh = '') AND (content_en IS NULL OR content_en = '') THEN 1 ELSE 0 END) as all_empty
FROM community_posts;

