-- 修复泰语帖子的翻译
-- 这个脚本会查找需要翻译的泰语帖子并标记它们

-- 1. 查找所有需要翻译的泰语帖子（标题包含泰语字符但翻译为空）
SELECT 
    id,
    title,
    original_language,
    CASE 
        WHEN title_zh IS NULL OR title_zh = '' THEN '需要中文标题翻译'
        ELSE '已有中文标题'
    END as title_zh_status,
    CASE 
        WHEN title_en IS NULL OR title_en = '' THEN '需要英文标题翻译'
        ELSE '已有英文标题'
    END as title_en_status,
    CASE 
        WHEN content_zh IS NULL OR content_zh = '' THEN '需要中文内容翻译'
        ELSE '已有中文内容'
    END as content_zh_status,
    CASE 
        WHEN content_en IS NULL OR content_en = '' THEN '需要英文内容翻译'
        ELSE '已有英文内容'
    END as content_en_status
FROM community_posts
WHERE (title REGEXP '[ก-๙]' OR original_language = 'th')
  AND (
    (title_zh IS NULL OR title_zh = '') 
    OR (title_en IS NULL OR title_en = '')
    OR (content_zh IS NULL OR content_zh = '')
    OR (content_en IS NULL OR content_en = '')
  )
ORDER BY created_at DESC;

-- 2. 统计需要翻译的帖子数量
SELECT 
    COUNT(*) as total_needs_translation,
    SUM(CASE WHEN title_zh IS NULL OR title_zh = '' THEN 1 ELSE 0 END) as need_title_zh,
    SUM(CASE WHEN title_en IS NULL OR title_en = '' THEN 1 ELSE 0 END) as need_title_en,
    SUM(CASE WHEN content_zh IS NULL OR content_zh = '' THEN 1 ELSE 0 END) as need_content_zh,
    SUM(CASE WHEN content_en IS NULL OR content_en = '' THEN 1 ELSE 0 END) as need_content_en
FROM community_posts
WHERE (title REGEXP '[ก-๙]' OR original_language = 'th')
  AND (
    (title_zh IS NULL OR title_zh = '') 
    OR (title_en IS NULL OR title_en = '')
    OR (content_zh IS NULL OR content_zh = '')
    OR (content_en IS NULL OR content_en = '')
  );

