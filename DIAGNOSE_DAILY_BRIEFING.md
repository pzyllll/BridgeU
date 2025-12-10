# 每日简报语言问题诊断指南

## 问题描述
每日简报没有根据用户语言偏好显示对应语言的内容。

## 诊断步骤

### 1. 检查数据库翻译数据

运行 SQL 脚本检查数据库：
```bash
mysql -u your_username -p global_buddy < check_news_translations.sql
```

或者直接在 MySQL 客户端运行：
```sql
USE global_buddy;

-- 检查翻译字段的填充情况
SELECT 
    COUNT(*) as total_news,
    COUNT(title_zh) as has_title_zh,
    COUNT(title_en) as has_title_en,
    COUNT(summary_zh) as has_summary_zh,
    COUNT(summary_en) as has_summary_en
FROM news;
```

**如果所有翻译字段都是 0，说明数据库中没有翻译数据，需要运行翻译功能。**

### 2. 检查前端语言设置

在浏览器控制台运行：
```javascript
// 检查 localStorage
console.log('userLanguage:', localStorage.getItem('userLanguage'));

// 检查当前语言
const { getCurrentLanguage } = require('./src/i18n');
console.log('Current language:', getCurrentLanguage());

// 手动设置语言并刷新
localStorage.setItem('userLanguage', 'zh');
location.reload();
```

### 3. 检查网络请求

在浏览器开发者工具的 Network 标签页中：
1. 刷新页面
2. 查找 `/api/news/daily-briefing` 请求
3. 检查请求参数中是否包含 `lang=zh` 或 `lang=en`
4. 检查响应数据中的 `title` 和 `summary` 字段

### 4. 检查后端日志

查看后端控制台输出，查找：
- `Fetching today's news briefing, page: ..., size: ..., lang: zh`
- `Using Chinese title translation` 或 `Chinese title translation not available`
- `News items with Chinese translation: X/Y`

### 5. 如果数据库没有翻译数据

调用新闻翻译 API：
```bash
# 使用 curl
curl -X POST "http://localhost:8080/api/news/translate-news?limit=0&force=false"

# 或使用 Postman/浏览器
POST http://localhost:8080/api/news/translate-news?limit=0&force=false
```

这会翻译所有没有翻译的新闻。

### 6. 强制重新翻译（如果需要）

如果翻译数据有问题，可以强制重新翻译：
```bash
POST http://localhost:8080/api/news/translate-news?limit=0&force=true
```

## 常见问题

### 问题 1: 数据库中没有翻译数据
**症状**: 后端日志显示 `Chinese title translation not available`
**解决**: 运行新闻翻译 API

### 问题 2: 前端没有传递语言参数
**症状**: 后端日志显示 `lang: en`（即使设置了中文）
**解决**: 检查 `localStorage.getItem('userLanguage')` 的值

### 问题 3: 后端返回了翻译但前端显示原文
**症状**: 网络请求响应中有 `titleZh`，但页面显示的是 `title`
**解决**: 检查后端 `convertToDTO` 方法是否正确设置了 `title` 字段

## 调试命令

### 前端调试
```javascript
// 在浏览器控制台运行
localStorage.setItem('userLanguage', 'zh');
console.log('Set language to zh, reloading...');
location.reload();
```

### 后端调试
查看日志级别，确保 INFO 和 WARN 级别的日志都显示。

