-- 检查泰语帖子的翻译情况
-- 查找原始语言为泰语或标题包含泰语字符的帖子

-- 1. 查看所有泰语帖子及其翻译状态
SELECT 
    id,
    title,
    LEFT(title, 50) as title_preview,
    LENGTH(body) as body_length,
    LENGTH(content_zh) as content_zh_length,
    LENGTH(content_en) as content_en_length,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(title_en) as title_en_length,
    original_language,
    CASE 
        WHEN content_zh IS NULL OR content_zh = '' THEN '❌ 无中文翻译'
        ELSE '✅ 有中文翻译'
    END as zh_status,
    CASE 
        WHEN content_en IS NULL OR content_en = '' THEN '❌ 无英文翻译'
        ELSE '✅ 有英文翻译'
    END as en_status,
    CASE 
        WHEN title_zh IS NULL OR title_zh = '' THEN '❌ 无中文标题'
        ELSE '✅ 有中文标题'
    END as title_zh_status,
    CASE 
        WHEN title_en IS NULL OR title_en = '' THEN '❌ 无英文标题'
        ELSE '✅ 有英文标题'
    END as title_en_status,
    created_at
FROM community_posts
WHERE original_language = 'th' 
   OR title REGEXP '[ก-๙]'  -- 包含泰语字符
ORDER BY created_at DESC
LIMIT 20;

-- 2. 统计泰语帖子的翻译情况
SELECT 
    COUNT(*) as total_thai_posts,
    SUM(CASE WHEN content_zh IS NULL OR content_zh = '' THEN 1 ELSE 0 END) as missing_content_zh,
    SUM(CASE WHEN content_en IS NULL OR content_en = '' THEN 1 ELSE 0 END) as missing_content_en,
    SUM(CASE WHEN title_zh IS NULL OR title_zh = '' THEN 1 ELSE 0 END) as missing_title_zh,
    SUM(CASE WHEN title_en IS NULL OR title_en = '' THEN 1 ELSE 0 END) as missing_title_en,
    SUM(CASE WHEN (content_zh IS NULL OR content_zh = '') AND (content_en IS NULL OR content_en = '') THEN 1 ELSE 0 END) as missing_both_content,
    SUM(CASE WHEN (title_zh IS NULL OR title_zh = '') AND (title_en IS NULL OR title_en = '') THEN 1 ELSE 0 END) as missing_both_title
FROM community_posts
WHERE original_language = 'th' 
   OR title REGEXP '[ก-๙]';

-- 3. 查看具体几个帖子的详细内容（根据图片中的标题）
-- 查找包含 "เถ้าแก่น้อย" 的帖子
SELECT 
    id,
    title,
    LEFT(body, 100) as body_preview,
    LEFT(content_zh, 100) as content_zh_preview,
    LEFT(content_en, 100) as content_en_preview,
    LEFT(title_zh, 50) as title_zh_preview,
    LEFT(title_en, 50) as title_en_preview,
    original_language,
    created_at
FROM community_posts
WHERE title LIKE '%เถ้าแก่น้อย%'
   OR title LIKE '%สสส%'
   OR title LIKE '%โอ-ออ%'
   OR title LIKE '%ไชยยงค์%'
ORDER BY created_at DESC;

