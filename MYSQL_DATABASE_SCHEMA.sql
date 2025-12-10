-- Global Buddy MySQL Database Schema
-- Version: 1.0
-- Date: 2025-12-05

-- 创建数据库
CREATE DATABASE IF NOT EXISTS global_buddy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE global_buddy;

-- 删除外键约束（如果存在）
SET FOREIGN_KEY_CHECKS = 0;

-- 删除现有表（如果存在）
DROP TABLE IF EXISTS posts;
DROP TABLE IF EXISTS communities;
DROP TABLE IF EXISTS users;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 创建用户表
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY COMMENT '用户唯一标识符',
    username VARCHAR(255) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱地址',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    password VARCHAR(255) COMMENT '兼容旧字段',
    display_name VARCHAR(255) NOT NULL COMMENT '显示名称',
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER' COMMENT '用户角色',
    nationality VARCHAR(255) NOT NULL DEFAULT '' COMMENT '国籍',
    studying_in_country VARCHAR(255) NOT NULL DEFAULT '' COMMENT '留学国家',
    institution VARCHAR(255) COMMENT '所在学校',
    languages TEXT COMMENT '语言列表 (JSON 格式)',
    interests TEXT COMMENT '兴趣爱好 (JSON 格式)',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '账户是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 创建社区表
CREATE TABLE communities (
    id VARCHAR(36) PRIMARY KEY COMMENT '社区唯一标识符',
    title VARCHAR(255) NOT NULL COMMENT '社区标题',
    description TEXT COMMENT '社区描述',
    country VARCHAR(255) NOT NULL COMMENT '国家',
    language VARCHAR(255) COMMENT '语言',
    tags TEXT COMMENT '标签列表 (JSON 格式)',
    created_by VARCHAR(36) COMMENT '创建者',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    INDEX idx_country (country),
    INDEX idx_language (language),
    INDEX idx_created_by (created_by),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区表';

-- 创建帖子表
CREATE TABLE posts (
    id VARCHAR(36) PRIMARY KEY COMMENT '帖子唯一标识符',
    community_id VARCHAR(36) COMMENT '所属社区',
    author_id VARCHAR(36) NOT NULL COMMENT '作者',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    body TEXT NOT NULL COMMENT '内容',
    tags TEXT COMMENT '标签列表 (JSON 格式)',
    category VARCHAR(255) COMMENT '分类',
    image_url VARCHAR(255) COMMENT '图片 URL',
    ai_result TEXT COMMENT 'AI 审核结果',
    ai_confidence DOUBLE COMMENT 'AI 置信度 (0.0 - 1.0)',
    status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING_REVIEW' COMMENT '审核状态',
    review_note TEXT COMMENT '审核备注',
    reviewed_by VARCHAR(36) COMMENT '审核人',
    reviewed_at TIMESTAMP NULL DEFAULT NULL COMMENT '审核时间',
    embedding TEXT COMMENT '向量嵌入',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    INDEX idx_community (community_id),
    INDEX idx_author (author_id),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE SET NULL,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 插入测试数据

-- 插入管理员用户
INSERT INTO users (id, username, email, password_hash, display_name, role, nationality, studying_in_country, institution, languages, interests, enabled) VALUES
('1', 'admin', 'admin@globalbuddy.com', '$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO', '管理员', 'ADMIN', 'China', 'China', 'GlobalBuddy', '["zh", "en"]', '["管理", "审核"]', TRUE);

-- 插入普通用户
INSERT INTO users (id, username, email, password_hash, display_name, role, nationality, studying_in_country, institution, languages, interests, enabled) VALUES
('2', 'lihua', 'lihua@example.com', '$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO', '李华', 'USER', 'China', 'Thailand', 'Chulalongkorn University', '["zh", "en"]', '["美食", "旅游"]', TRUE),
('3', 'emily', 'emily@example.com', '$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO', 'Emily', 'USER', 'UK', 'South Korea', 'Yonsei University', '["en", "ko"]', '["语言交换", "咖啡厅"]', TRUE),
('4', 'somchai', 'somchai@example.com', '$2a$10$8K1p/a0dhrxiowP.dnkgNORTWgdEDHn5L2/xjpEWuC.QQv4rKO9jO', 'Somchai', 'USER', 'Thailand', 'China', 'Fudan University', '["th", "zh"]', '["科技", "创业"]', TRUE);

-- 插入社区数据
INSERT INTO communities (id, title, description, country, language, tags, created_by) VALUES
('1', '中国人在泰国留学', '分享签证、租房、美食攻略等信息', 'Thailand', 'zh', '["签证", "美食", "租房"]', '2'),
('2', '英国人在韩国留学', '学校申请、课程选择、生活分享', 'South Korea', 'en', '["课程", "生活", "语言"]', '3'),
('3', '泰国人在中国留学', '适应中国生活、二手交易、语言互助', 'China', 'zh', '["二手", "语言", "互助"]', '4');

-- 插入已审核帖子
INSERT INTO posts (id, community_id, author_id, title, body, tags, category, status) VALUES
('1', '1', '2', '曼谷租房攻略', '推荐在 BTS 线附近找公寓，注意提前准备押金，和房东确认水电费用。', '["租房", "曼谷"]', '生活', 'APPROVED'),
('2', '2', '3', '延世大学选课技巧', '热门课程要提前卡位，建议关注选课开放时间，并提前准备好备选计划。', '["课程", "选课"]', '学习', 'APPROVED'),
('3', '3', '4', '上海哪里吃泰餐', '静安寺附近有很多泰国餐厅，想家时可以去吃，味道很接近家乡。', '["美食", "生活"]', '社交', 'APPROVED');

-- 插入待审核帖子
INSERT INTO posts (id, community_id, author_id, title, body, tags, category, status, ai_result, ai_confidence) VALUES
('4', '1', '2', '泰国留学签证申请教程', '详细介绍如何申请泰国学生签证，包括所需材料和流程。', '["签证", "留学"]', '生活', 'PENDING_REVIEW', '待审核', 0.4),
('5', '2', '3', '韩国二手物品交易', '有人需要二手家具吗？低价转让，联系方式: xxx', '["二手", "交易"]', '生活', 'PENDING_REVIEW', 'AI 无法确定内容是否合规', 0.35);

COMMIT;