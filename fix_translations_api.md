# 批量修复错误翻译 API 使用指南

## 问题描述

某些记录的 `title_zh` 字段包含泰语字符而不是中文，这通常是由于：
1. 语言检测错误（将泰语误判为中文）
2. 翻译服务返回了错误的翻译结果
3. 直接保存了原始标题到 `title_zh` 字段

## 修复方案

### 1. 使用 SQL 查询找出所有错误记录

执行 `find_incorrect_translations.sql` 来查找所有 `title_zh` 包含泰语字符的记录。

### 2. 使用 HTML 工具批量修复

打开 `fix_incorrect_translations.html`，按照以下步骤操作：

1. **登录认证**
   - 输入管理员 JWT Token
   - 或点击"快速登录"按钮

2. **检查错误翻译**
   - 点击"检查所有错误翻译"按钮
   - 查看统计信息和错误列表

3. **批量修复**
   - 点击"修复所有帖子 (Posts)"按钮修复帖子
   - 点击"修复所有新闻 (News)"按钮修复新闻
   - 等待修复完成

### 3. 使用 API 端点手动修复

#### 修复单个帖子
```bash
POST /api/admin/posts/{postId}/translate
Authorization: Bearer {token}
```

#### 批量修复所有新闻
```bash
POST /api/news/translate-news?force=true&limit=0
Authorization: Bearer {token}
```

## 修复后的验证

修复完成后，可以再次执行 SQL 查询来验证：
- 所有 `title_zh` 字段应该包含中文字符
- 不应该包含泰语字符
- 如果原始语言是中文，`title_zh` 应该等于原始标题

## 注意事项

1. 修复过程可能需要较长时间，特别是当错误记录较多时
2. 建议在修复前备份数据库
3. 修复后需要重新编译并重启后端，确保新的验证逻辑生效

