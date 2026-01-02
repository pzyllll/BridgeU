# 如何获取新闻信息 - 使用指南

## 📋 概述

系统已实现**混合数据源策略**：
- **Google Trends RSS**（定方向）：获取热门关键词，确定新闻方向
- **媒体 RSS**（取内容）：从多个泰国新闻媒体获取具体新闻内容
- **ROME 库**：使用专业的 RSS 解析库，更稳定可靠

## 🚀 获取新闻的三种方式

### 方式 1：自动定时爬取（推荐）

系统每天**上午 8:00** 自动执行新闻爬取任务，包括：
1. 从 Google Trends RSS 获取热门关键词
2. 从配置的媒体 RSS 源获取新闻
3. 根据热门关键词对新闻进行优先级排序
4. 生成 AI 摘要
5. 自动翻译（中文/英文）
6. 保存到数据库

**无需任何操作**，系统会自动完成！

---

### 方式 2：手动触发爬取

如果你想立即获取最新新闻，可以手动触发：

#### 使用 Postman 或 curl

```bash
# 手动触发新闻爬取
POST http://localhost:8080/api/news/refresh
```

**响应示例：**
```json
{
  "success": true,
  "message": "News crawling and AI summarization task triggered",
  "costMs": 45000
}
```

**说明：**
- 这会执行完整的新闻爬取流程（包括 Google Trends + 媒体 RSS）
- 处理时间通常需要 30-60 秒
- 新闻会自动保存到数据库

---

### 方式 3：查看已获取的新闻

#### 3.1 获取新闻列表（前端接口）

```bash
# 获取每日新闻简报（支持分页、筛选）
GET http://localhost:8080/api/news/daily-briefing?page=0&size=10&lang=zh
```

**参数说明：**
- `page`: 页码（从 0 开始）
- `size`: 每页数量
- `lang`: 语言（zh/en）
- `source`: 新闻来源筛选（可选）
- `startDate`: 开始日期（格式：yyyy-MM-dd，可选）
- `endDate`: 结束日期（格式：yyyy-MM-dd，可选）

**响应示例：**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "新闻标题",
        "titleZh": "新闻标题（中文）",
        "titleEn": "News Title (English)",
        "summary": "新闻摘要",
        "summaryZh": "新闻摘要（中文）",
        "summaryEn": "News Summary (English)",
        "source": "Bangkok Post",
        "originalUrl": "https://...",
        "publishDate": "2026-01-01T00:00:00"
      }
    ],
    "totalElements": 100,
    "totalPages": 10,
    "currentPage": 0
  }
}
```

---

## 🧪 测试接口

### 测试 Google Trends RSS

```bash
# 测试泰国热门关键词
POST http://localhost:8080/api/news/test-google-trends
Content-Type: application/json

{
  "geoCode": "TH",
  "maxItems": 20
}
```

**响应示例：**
```json
{
  "success": true,
  "message": "Google Trends RSS test completed",
  "geoCode": "TH",
  "count": 20,
  "costMs": 2500,
  "keywords": [
    "关键词1",
    "关键词2",
    ...
  ]
}
```

**参数说明：**
- `geoCode`: 地区代码
  - `"TH"`: 泰国
  - `"US"`: 美国
  - `null` 或不提供: 全球
- `maxItems`: 最多获取的关键词数量（默认 20）

---

### 测试单个 RSS 源

```bash
# 测试单个 RSS 源
POST http://localhost:8080/api/news/test-rss
Content-Type: application/json

{
  "url": "https://www.khaosod.co.th/feed",
  "source": "Khaosod"
}
```

---

### 测试所有配置的 RSS 源

```bash
# 测试所有 RSS 源
POST http://localhost:8080/api/news/test-all-rss
```

这会测试所有配置的媒体 RSS 源，并返回每个源的测试结果。

---

## 📊 工作流程

```
┌─────────────────────────────────────────────────────────┐
│  1. Google Trends RSS (定方向)                           │
│     ↓ 获取热门关键词（如：20个）                         │
│                                                          │
│  2. 媒体 RSS (取内容)                                    │
│     ↓ 从多个新闻媒体获取新闻（每个源最多5条）            │
│                                                          │
│  3. 混合策略（优先级排序）                               │
│     ↓ 根据热门关键词对新闻进行排序                       │
│     ↓ 包含热门关键词的新闻排在前面                       │
│                                                          │
│  4. AI 摘要生成                                          │
│     ↓ 为每条新闻生成摘要                                 │
│                                                          │
│  5. 自动翻译                                             │
│     ↓ 翻译标题和摘要（中文/英文）                        │
│                                                          │
│  6. 保存到数据库                                         │
│     ↓ 新闻保存到 MySQL 数据库                           │
└─────────────────────────────────────────────────────────┘
```

---

## 🔍 查看日志

系统会输出详细的日志信息，你可以在控制台或日志文件中查看：

```
INFO  - Step 1: Fetching trending keywords from Google Trends RSS (Thailand)...
INFO  - Fetched 20 trending keywords from Google Trends: [...]
INFO  - Step 2: Fetching news from 12 media RSS feeds (max 5 items per feed)...
INFO  - Successfully fetched 45 news items from all Thai news website RSS feeds
INFO  - Step 3: Prioritizing news based on 20 trending keywords...
INFO  - News prioritized: 45 items total (trending items appear first)
```

---

## ⚙️ 配置说明

### Google Trends RSS URL

在 `GoogleTrendsRssService.java` 中已配置：
- 泰国：`https://trends.google.com/trends/trendingsearches/daily/rss?geo=TH`
- 美国：`https://trends.google.com/trends/trendingsearches/daily/rss?geo=US`
- 全球：`https://trends.google.com/trends/trendingsearches/daily/rss`

### 媒体 RSS 源

在 `NewsCrawlerService.java` 中配置了多个泰国新闻媒体 RSS 源，包括：
- Matichon (มติชน)
- Khaosod (ข่าวสด)
- Post Today
- Bangkok Post
- The Nation Thailand
- 等等...

---

## ❓ 常见问题

### Q: 为什么有些 RSS 源获取失败？

A: 可能的原因：
1. RSS 源 URL 已失效（404）
2. 网络连接问题
3. XML 格式问题（系统已使用 ROME 库和预处理来尽量解决）

### Q: Google Trends RSS 获取失败怎么办？

A: Google Trends RSS 可能偶尔不可用。系统会记录错误日志，但不会影响媒体 RSS 的获取。

### Q: 如何查看获取到的新闻？

A: 使用 `/api/news/daily-briefing` 接口，或在前端页面的"每日简报"中查看。

### Q: 新闻多久更新一次？

A: 默认每天上午 8:00 自动更新。你也可以随时手动触发 `/api/news/refresh`。

---

## 📝 总结

1. **自动方式**：系统每天 8:00 自动爬取（无需操作）
2. **手动方式**：调用 `POST /api/news/refresh` 立即获取
3. **查看新闻**：使用 `/api/news/daily-briefing` 接口
4. **测试功能**：使用测试接口验证各个功能是否正常

系统已完全自动化，你只需要等待或手动触发即可！

