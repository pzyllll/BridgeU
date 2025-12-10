-- 验证翻译结果
-- 检查刚才翻译的4个帖子

SELECT 
    id,
    LEFT(title, 60) as original_title,
    LEFT(title_zh, 60) as chinese_title,
    LEFT(title_en, 60) as english_title,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(title_en) as title_en_length,
    LENGTH(content_zh) as content_zh_length,
    LENGTH(content_en) as content_en_length,
    original_language,
    CASE 
        WHEN title_zh IS NOT NULL AND title_zh != '' THEN '✅'
        ELSE '❌'
    END as has_title_zh,
    CASE 
        WHEN title_en IS NOT NULL AND title_en != '' THEN '✅'
        ELSE '❌'
    END as has_title_en,
    CASE 
        WHEN content_zh IS NOT NULL AND content_zh != '' THEN '✅'
        ELSE '❌'
    END as has_content_zh,
    CASE 
        WHEN content_en IS NOT NULL AND content_en != '' THEN '✅'
        ELSE '❌'
    END as has_content_en
FROM community_posts
WHERE id IN (
    'e376e0d4-f1f7-43f4-970f-d9620659fe63',
    '327b611f-a1ee-4ae9-a4dc-f99a228e3205',
    '08a5f95f-1603-4c0f-bb2a-e4698f390fdb',
    'b99775b4-58a6-4d93-a319-6ce740e0c7ad'
)
ORDER BY created_at DESC;

