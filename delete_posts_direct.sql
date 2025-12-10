-- 直接删除未翻译好的泰语帖子
-- 注意：此操作不可逆，请先备份数据库！

USE global_buddy;

-- 开始事务（可以回滚）
START TRANSACTION;

-- 第一步：查看要删除的帖子（确认一下）
SELECT 
    id,
    title,
    LEFT(title_zh, 50) as title_zh_preview,
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

-- 第二步：删除评论
DELETE FROM comments WHERE post_id IN (
    SELECT id FROM community_posts
    WHERE title LIKE '%ไชยยงค์%'
       OR title LIKE '%เถ้าแก่น้อย%'
       OR title LIKE '%สสส.%'
       OR title LIKE '%ICONIC Run Fest%'
       OR title LIKE '%โอ-ออ%'
       OR title LIKE '%นัทปง%'
);

-- 第三步：删除点赞
DELETE FROM post_likes WHERE post_id IN (
    SELECT id FROM community_posts
    WHERE title LIKE '%ไชยยงค์%'
       OR title LIKE '%เถ้าแก่น้อย%'
       OR title LIKE '%สสส.%'
       OR title LIKE '%ICONIC Run Fest%'
       OR title LIKE '%โอ-ออ%'
       OR title LIKE '%นัทปง%'
);

-- 第四步：删除帖子
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

