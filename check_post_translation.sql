-- 检查帖子翻译字段
-- 替换 POST_ID 为实际的帖子ID

SELECT 
    id,
    title,
    title_zh,
    title_en,
    body,
    content_zh,
    content_en,
    original_language,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(title_en) as title_en_length,
    LENGTH(content_zh) as content_zh_length,
    LENGTH(content_en) as content_en_length
FROM posts
WHERE id = 'e51f67dc-a46c-4ac5-ac75-f248035c9203';

-- 或者查看所有有翻译的帖子
-- SELECT 
--     id,
--     LEFT(title, 50) as title_preview,
--     LEFT(title_zh, 50) as title_zh_preview,
--     LEFT(title_en, 50) as title_en_preview,
--     original_language,
--     CASE 
--         WHEN title_zh IS NULL OR title_zh = '' THEN '❌ 无中文标题'
--         ELSE '✅ 有中文标题'
--     END as title_zh_status,
--     CASE 
--         WHEN title_en IS NULL OR title_en = '' THEN '❌ 无英文标题'
--         ELSE '✅ 有英文标题'
--     END as title_en_status
-- FROM posts
-- WHERE original_language = 'th'
-- ORDER BY created_at DESC
-- LIMIT 20;

