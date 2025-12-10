-- 删除未翻译好的泰语帖子及其关联数据
-- 根据标题查找这些帖子

-- 第一步：查找这些帖子
SELECT 
    id,
    title,
    LEFT(title_zh, 80) as title_zh_preview,
    LEFT(content_zh, 80) as content_zh_preview,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(content_zh) as content_zh_length,
    created_at
FROM community_posts
WHERE title LIKE '%ไชยยงค์%'
   OR title LIKE '%เถ้าแก่น้อย%'
   OR title LIKE '%สสส.%'
   OR title LIKE '%ICONIC Run Fest%'
   OR title LIKE '%โอ-ออ%'
   OR title LIKE '%นัทปง%'
ORDER BY created_at DESC;

-- 第二步：删除这些帖子的关联数据（评论、点赞）
-- 注意：先执行上面的查询，确认要删除的帖子ID，然后取消注释下面的DELETE语句

-- 删除评论（先删除，因为有外键约束）
-- DELETE FROM comments WHERE post_id IN (
--     SELECT id FROM community_posts
--     WHERE title LIKE '%ไชยยงค์%'
--        OR title LIKE '%เถ้าแก่น้อย%'
--        OR title LIKE '%สสส.%'
--        OR title LIKE '%ICONIC Run Fest%'
--        OR title LIKE '%โอ-ออ%'
--        OR title LIKE '%นัทปง%'
-- );

-- 删除点赞（先删除，因为有外键约束）
-- DELETE FROM post_likes WHERE post_id IN (
--     SELECT id FROM community_posts
--     WHERE title LIKE '%ไชยยงค์%'
--        OR title LIKE '%เถ้าแก่น้อย%'
--        OR title LIKE '%สสส.%'
--        OR title LIKE '%ICONIC Run Fest%'
--        OR title LIKE '%โอ-ออ%'
--        OR title LIKE '%นัทปง%'
-- );

-- 第三步：删除帖子本身
-- DELETE FROM community_posts
-- WHERE title LIKE '%ไชยยงค์%'
--    OR title LIKE '%เถ้าแก่น้อย%'
--    OR title LIKE '%สสส.%'
--    OR title LIKE '%ICONIC Run Fest%'
--    OR title LIKE '%โอ-ออ%'
--    OR title LIKE '%นัทปง%';

-- ============================================
-- 或者，如果您想一次性删除（使用事务）：
-- ============================================

START TRANSACTION;

-- 查找并显示要删除的帖子
SELECT 
    id,
    title,
    LEFT(title_zh, 80) as title_zh_preview,
    created_at
FROM community_posts
WHERE title LIKE '%ไชยยงค์%'
   OR title LIKE '%เถ้าแก่น้อย%'
   OR title LIKE '%สสส.%'
   OR title LIKE '%ICONIC Run Fest%'
   OR title LIKE '%โอ-ออ%'
   OR title LIKE '%นัทปง%'
ORDER BY created_at DESC;

-- 删除评论
DELETE FROM comments WHERE post_id IN (
    SELECT id FROM community_posts
    WHERE title LIKE '%ไชยยงค์%'
       OR title LIKE '%เถ้าแก่น้อย%'
       OR title LIKE '%สสส.%'
       OR title LIKE '%ICONIC Run Fest%'
       OR title LIKE '%โอ-ออ%'
       OR title LIKE '%นัทปง%'
);

-- 删除点赞
DELETE FROM post_likes WHERE post_id IN (
    SELECT id FROM community_posts
    WHERE title LIKE '%ไชยยงค์%'
       OR title LIKE '%เถ้าแก่น้อย%'
       OR title LIKE '%สสส.%'
       OR title LIKE '%ICONIC Run Fest%'
       OR title LIKE '%โอ-ออ%'
       OR title LIKE '%นัทปง%'
);

-- 删除帖子
DELETE FROM community_posts
WHERE title LIKE '%ไชยยงค์%'
   OR title LIKE '%เถ้าแก่น้อย%'
   OR title LIKE '%สสส.%'
   OR title LIKE '%ICONIC Run Fest%'
   OR title LIKE '%โอ-ออ%'
   OR title LIKE '%นัทปง%';

-- 查看删除结果
SELECT ROW_COUNT() as deleted_posts_count;

-- 如果确认无误，执行 COMMIT; 否则执行 ROLLBACK;
-- COMMIT;
-- ROLLBACK;

