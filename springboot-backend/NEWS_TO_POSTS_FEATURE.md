# 新闻自动转帖子功能说明

## 功能概述

该功能可以将抓取的公开网站新闻自动转换为社区帖子，推送到前端显示。

## 工作流程

1. **抓取新闻** - 从目标网站（如 Bangkok Post）抓取新闻
2. **生成摘要** - 使用 AI（Qwen）生成中文摘要
3. **保存新闻** - 将新闻保存到 `news` 表
4. **转换为帖子** - 自动将新闻转换为社区帖子
5. **推送到前端** - 帖子自动出现在"新闻资讯"社区中

## API 接口

### 1. 抓取并转换为帖子（推荐）

**接口**: `POST /api/news/crawl-and-convert`

**功能**: 一次性完成抓取新闻和转换为帖子的流程

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/news/crawl-and-convert \
  -H "Content-Type: application/json" \
  -d '{"limit": 10}'
```

**响应示例**:
```json
{
  "success": true,
  "message": "抓取并转换完成",
  "crawlCostMs": 15234,
  "conversionResult": {
    "totalProcessed": 8,
    "successCount": 8,
    "skipCount": 0,
    "errorCount": 0
  }
}
```

### 2. 仅转换已抓取的新闻

**接口**: `POST /api/news/convert-to-posts`

**功能**: 只将数据库中已存在的新闻转换为帖子（不重新抓取）

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/news/convert-to-posts \
  -H "Content-Type: application/json" \
  -d '{"limit": 10}'
```

**响应示例**:
```json
{
  "success": true,
  "message": "转换完成",
  "result": {
    "totalProcessed": 5,
    "successCount": 5,
    "skipCount": 0,
    "errorCount": 0
  }
}
```

### 3. 手动刷新新闻（不转换）

**接口**: `POST /api/news/refresh`

**功能**: 只抓取新闻并生成摘要，不转换为帖子

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/news/refresh
```

## 前端集成

### 1. 调用 API

在 `frontend/src/api.js` 中添加：

```javascript
// 抓取并转换为帖子
export const crawlAndConvertToPosts = async (limit = 10) => {
  try {
    const response = await axios.post('http://localhost:8080/api/news/crawl-and-convert', {
      limit
    });
    return response.data;
  } catch (error) {
    console.error('Error crawling and converting:', error);
    throw error;
  }
};

// 仅转换已抓取的新闻
export const convertNewsToPosts = async (limit = 10) => {
  try {
    const response = await axios.post('http://localhost:8080/api/news/convert-to-posts', {
      limit
    });
    return response.data;
  } catch (error) {
    console.error('Error converting news:', error);
    throw error;
  }
};
```

### 2. 创建管理组件

创建 `frontend/src/components/NewsCrawlerAdmin.jsx`:

```jsx
import { useState } from 'react';
import { crawlAndConvertToPosts, convertNewsToPosts } from '../api';

const NewsCrawlerAdmin = () => {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);

  const handleCrawlAndConvert = async () => {
    setLoading(true);
    try {
      const data = await crawlAndConvertToPosts(10);
      setResult(data);
    } catch (error) {
      alert('操作失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleConvertOnly = async () => {
    setLoading(true);
    try {
      const data = await convertNewsToPosts(10);
      setResult(data);
    } catch (error) {
      alert('操作失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="news-crawler-admin">
      <h2>新闻抓取管理</h2>
      <div className="actions">
        <button onClick={handleCrawlAndConvert} disabled={loading}>
          {loading ? '处理中...' : '抓取并转换为帖子'}
        </button>
        <button onClick={handleConvertOnly} disabled={loading}>
          {loading ? '处理中...' : '仅转换已抓取的新闻'}
        </button>
      </div>
      {result && (
        <div className="result">
          <h3>处理结果</h3>
          <pre>{JSON.stringify(result, null, 2)}</pre>
        </div>
      )}
    </div>
  );
};

export default NewsCrawlerAdmin;
```

## 自动创建的资源

### 1. 系统用户

- **用户名**: `system_bot`
- **邮箱**: `system@globalbuddy.com`
- **显示名**: `系统机器人`
- **用途**: 作为自动生成帖子的作者

### 2. 新闻社区

- **标题**: `新闻资讯`
- **描述**: `自动抓取的新闻资讯，帮助留学生了解最新动态`
- **国家**: `Global`
- **语言**: `zh`
- **标签**: `["新闻", "资讯", "自动更新"]`

## 帖子格式

转换后的帖子包含：

- **标题**: 新闻标题
- **内容**: 
  - AI 生成的摘要
  - 新闻详细内容（前 500 字）
  - 原文链接
- **标签**: 自动提取的关键词（如"泰国"、"留学"、"签证"等）
- **分类**: `新闻资讯`
- **状态**: `APPROVED`（自动审核通过）

## 去重机制

系统会自动检查是否已存在相同标题的帖子，避免重复创建。

## 注意事项

1. **API Key**: 确保 `application.properties` 中配置了有效的 DashScope API Key
2. **数据库**: 确保 MySQL 数据库 `bridgeu` 已创建
3. **网络**: 确保可以访问目标网站（如 Bangkok Post）
4. **限制**: 默认每次最多转换 10 条新闻，可通过 `limit` 参数调整

## 定时任务

可以配置定时任务自动执行抓取和转换：

在 `NewsScheduler` 中添加：

```java
@Scheduled(cron = "0 0 */6 * * ?") // 每 6 小时执行一次
public void scheduledCrawlAndConvert() {
    newsScheduler.manualTrigger();
    newsToPostService.convertNewsToPosts(10);
}
```

## 故障排查

1. **转换失败**: 检查日志中的错误信息
2. **帖子未出现**: 检查是否已创建"新闻资讯"社区
3. **AI 摘要为空**: 检查 DashScope API Key 是否有效
4. **重复帖子**: 系统会自动跳过已存在的帖子

