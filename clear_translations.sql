-- 清除 posts 表中所有翻译字段的 SQL 脚本
-- 执行此脚本后，需要重新运行翻译迁移

-- 根据实际数据库名称修改 USE 语句
-- USE bridgeu;  -- 如果数据库名是 bridgeu，取消注释此行
-- USE global_buddy;  -- 如果数据库名是 global_buddy，取消注释此行

-- 清除所有帖子的中文翻译字段
UPDATE posts SET content_zh = NULL;

-- 清除所有帖子的英文翻译字段
UPDATE posts SET content_en = NULL;

-- 可选：同时清除标题翻译字段（如果存在）
-- UPDATE posts SET title_zh = NULL;
-- UPDATE posts SET title_en = NULL;

-- 查看清除结果
SELECT 
    COUNT(*) as total_posts,
    COUNT(content_zh) as posts_with_zh,
    COUNT(content_en) as posts_with_en
FROM posts;

