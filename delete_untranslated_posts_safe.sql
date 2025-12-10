-- 安全删除脚本：先查看，再删除
-- 使用方法：
-- 1. 先执行第一部分（SELECT）查看要删除的帖子
-- 2. 确认无误后，取消注释 DELETE 语句并执行

-- ============================================
-- 第一部分：查看要删除的帖子
-- ============================================
SELECT 
    id,
    title,
    LEFT(title_zh, 80) as title_zh_preview,
    LEFT(content_zh, 80) as content_zh_preview,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(content_zh) as content_zh_length,
    created_at,
    (SELECT COUNT(*) FROM comments WHERE post_id = community_posts.id) as comment_count,
    (SELECT COUNT(*) FROM post_likes WHERE post_id = community_posts.id) as like_count
FROM community_posts
WHERE title LIKE '%ไชยยงค์%'
   OR title LIKE '%เถ้าแก่น้อย%'
   OR title LIKE '%สสส.%'
   OR title LIKE '%ICONIC Run Fest%'
   OR title LIKE '%โอ-ออ%'
   OR title LIKE '%นัทปง%'
ORDER BY created_at DESC;

-- ============================================
-- 第二部分：删除操作（需要手动取消注释）
-- ============================================

-- 开始事务
-- START TRANSACTION;

-- 删除评论
-- DELETE FROM comments WHERE post_id IN (
--     SELECT id FROM community_posts
--     WHERE title LIKE '%ไชยยงค์%'
--        OR title LIKE '%เถ้าแก่น้อย%'
--        OR title LIKE '%สสส.%'
--        OR title LIKE '%ICONIC Run Fest%'
--        OR title LIKE '%โอ-ออ%'
--        OR title LIKE '%นัทปง%'
-- );

-- 删除点赞
-- DELETE FROM post_likes WHERE post_id IN (
--     SELECT id FROM community_posts
--     WHERE title LIKE '%ไชยยงค์%'
--        OR title LIKE '%เถ้าแก่น้อย%'
--        OR title LIKE '%สสส.%'
--        OR title LIKE '%ICONIC Run Fest%'
--        OR title LIKE '%โอ-ออ%'
--        OR title LIKE '%นัทปง%'
-- );

-- 删除帖子
-- DELETE FROM community_posts
-- WHERE title LIKE '%ไชยยงค์%'
--    OR title LIKE '%เถ้าแก่น้อย%'
--    OR title LIKE '%สสส.%'
--    OR title LIKE '%ICONIC Run Fest%'
--    OR title LIKE '%โอ-ออ%'
--    OR title LIKE '%นัทปง%';

-- 提交事务（确认无误后执行）
-- COMMIT;

-- 回滚事务（如果有问题，执行这个）
-- ROLLBACK;

