-- 查找所有 title_zh 包含泰语字符的帖子（posts 表）
-- 泰语字符范围: \u0e00-\u0e7f

SELECT 
    id,
    title,
    title_zh,
    title_en,
    original_language,
    LEFT(title, 50) as title_preview,
    LEFT(title_zh, 50) as title_zh_preview,
    CASE 
        WHEN title_zh REGEXP '[\\u0e00-\\u0e7f]' THEN '❌ 包含泰语'
        WHEN title_zh REGEXP '[\\u4e00-\\u9fa5]' THEN '✅ 包含中文'
        ELSE '⚠️ 无中文无泰语'
    END as title_zh_status,
    created_at
FROM posts
WHERE title_zh IS NOT NULL 
  AND title_zh != ''
  AND title_zh REGEXP '[\\u0e00-\\u0e7f]'  -- 包含泰语字符
ORDER BY created_at DESC;

-- 查找所有 title_zh 包含泰语字符的新闻（news 表）
SELECT 
    id,
    title,
    title_zh,
    title_en,
    LEFT(title, 50) as title_preview,
    LEFT(title_zh, 50) as title_zh_preview,
    CASE 
        WHEN title_zh REGEXP '[\\u0e00-\\u0e7f]' THEN '❌ 包含泰语'
        WHEN title_zh REGEXP '[\\u4e00-\\u9fa5]' THEN '✅ 包含中文'
        ELSE '⚠️ 无中文无泰语'
    END as title_zh_status,
    create_time
FROM news
WHERE title_zh IS NOT NULL 
  AND title_zh != ''
  AND title_zh REGEXP '[\\u0e00-\\u0e7f]'  -- 包含泰语字符
ORDER BY create_time DESC;

-- 统计信息
SELECT 
    'posts' as table_name,
    COUNT(*) as total_with_title_zh,
    SUM(CASE WHEN title_zh REGEXP '[\\u0e00-\\u0e7f]' THEN 1 ELSE 0 END) as incorrect_count,
    SUM(CASE WHEN title_zh REGEXP '[\\u4e00-\\u9fa5]' AND NOT title_zh REGEXP '[\\u0e00-\\u0e7f]' THEN 1 ELSE 0 END) as correct_count
FROM posts
WHERE title_zh IS NOT NULL AND title_zh != ''

UNION ALL

SELECT 
    'news' as table_name,
    COUNT(*) as total_with_title_zh,
    SUM(CASE WHEN title_zh REGEXP '[\\u0e00-\\u0e7f]' THEN 1 ELSE 0 END) as incorrect_count,
    SUM(CASE WHEN title_zh REGEXP '[\\u4e00-\\u9fa5]' AND NOT title_zh REGEXP '[\\u0e00-\\u0e7f]' THEN 1 ELSE 0 END) as correct_count
FROM news
WHERE title_zh IS NOT NULL AND title_zh != '';

