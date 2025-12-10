-- 创建帖子详情功能所需的数据表
-- 包括：评论表、点赞表、关注关系表

USE bridgeu; -- 根据实际数据库名称修改

-- 1. 创建评论表 (comments)
CREATE TABLE IF NOT EXISTS comments (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    content_zh TEXT,
    content_en TEXT,
    original_language VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id),
    INDEX idx_author_id (author_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 创建点赞表 (post_likes)
CREATE TABLE IF NOT EXISTS post_likes (
    id VARCHAR(36) PRIMARY KEY,
    post_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_user (post_id, user_id),
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 创建关注关系表 (user_follows)
CREATE TABLE IF NOT EXISTS user_follows (
    id VARCHAR(36) PRIMARY KEY,
    follower_id VARCHAR(36) NOT NULL COMMENT '关注者ID',
    following_id VARCHAR(36) NOT NULL COMMENT '被关注者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    INDEX idx_follower_id (follower_id),
    INDEX idx_following_id (following_id),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    CHECK (follower_id != following_id) -- 不能关注自己
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 验证表是否创建成功
SELECT 
    'comments' as table_name,
    COUNT(*) as row_count
FROM comments
UNION ALL
SELECT 
    'post_likes' as table_name,
    COUNT(*) as row_count
FROM post_likes
UNION ALL
SELECT 
    'user_follows' as table_name,
    COUNT(*) as row_count
FROM user_follows;

-- 显示表结构
SHOW CREATE TABLE comments;
SHOW CREATE TABLE post_likes;
SHOW CREATE TABLE user_follows;

