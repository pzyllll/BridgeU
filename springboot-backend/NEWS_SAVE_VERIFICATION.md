# 新闻保存到 MySQL 数据库验证

## ✅ 数据库配置确认

### 1. 数据库连接
- **数据库名**: `bridgeu`
- **表名**: `news`
- **配置位置**: `application.properties`

### 2. 表结构
```sql
CREATE TABLE news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255),
    original_content TEXT,
    summary TEXT,
    original_url VARCHAR(1000),
    source VARCHAR(255),
    publish_date DATETIME(6),
    create_time DATETIME(6)
);
```

## 🔄 保存流程

### 1. 新闻获取
- **RSS 订阅源**: 通过 `RssFeedService.fetchNewsFromRss()` 获取
- **HTML 爬取**: 通过 `NewsCrawlerService.crawlXXX()` 获取
- **所有新闻源**: 通过 `crawlAllThaiNews()` 统一获取

### 2. 保存逻辑（NewsScheduler.scheduledCrawlAndSummarize()）

```java
// 步骤 1: 获取新闻列表
List<News> newsList = new ArrayList<>();
List<News> thaiNews = newsCrawlerService.crawlAllThaiNews();
newsList.addAll(thaiNews);
List<News> cmuNews = newsCrawlerService.crawlChiangMaiUniversity();
newsList.addAll(cmuNews);

// 步骤 2: 遍历每条新闻
for (News news : newsList) {
    // 2.1 去重检查（根据 originalUrl）
    if (newsRepository.findByOriginalUrl(news.getOriginalUrl()).isPresent()) {
        continue; // 跳过已存在的新闻
    }
    
    // 2.2 获取完整内容（可选）
    if (news.getOriginalContent() == null) {
        String content = newsCrawlerService.crawlNewsContent(news.getOriginalUrl());
        news.setOriginalContent(content);
    }
    
    // 2.3 生成 AI 摘要（可选，失败不影响保存）
    String summary = aiSummaryService.generateSummary(content);
    news.setSummary(summary);
    
    // 2.4 设置时间戳
    news.setCreateTime(new Date());
    news.setPublishDate(new Date());
    
    // 2.5 保存到数据库 ⭐ 关键步骤
    News savedNews = newsRepository.save(news);
}
```

## ✅ 保存保证

### 1. 自动保存
- ✅ 所有从 RSS 获取的新闻都会保存
- ✅ 所有从 HTML 爬取的新闻都会保存
- ✅ 即使 AI 摘要生成失败，新闻也会保存

### 2. 去重机制
- ✅ 根据 `originalUrl` 自动去重
- ✅ 已存在的新闻不会重复保存

### 3. 数据完整性
- ✅ 标题（title）- 必填
- ✅ 原文链接（originalUrl）- 必填（用于去重）
- ✅ 来源（source）- 必填
- ✅ 创建时间（createTime）- 自动设置
- ✅ 发布时间（publishDate）- 自动设置
- ⚠️ 摘要（summary）- 可选，AI 生成失败时使用标题
- ⚠️ 原始内容（originalContent）- 可选，需要额外抓取

## 🧪 验证方法

### 1. 检查数据库中的新闻数量

```sql
-- 查看总新闻数
SELECT COUNT(*) FROM news;

-- 按来源统计
SELECT source, COUNT(*) as count 
FROM news 
GROUP BY source 
ORDER BY count DESC;

-- 查看最新新闻
SELECT id, title, source, create_time 
FROM news 
ORDER BY create_time DESC 
LIMIT 10;
```

### 2. 手动触发抓取并验证

```powershell
# 触发抓取
$body = @{ limit = 20 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body

# 检查数据库
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p123456 bridgeu -e "SELECT COUNT(*) as total FROM news;"
```

### 3. 查看日志

保存成功时会输出：
```
✅ 新闻保存成功 - ID: 123, 标题: xxx, 来源: xxx, 摘要: 有/无
```

## 📊 预期数据

### 每次定时任务执行
- **泰国新闻网站**: 最多 105 条（7个网站 × 15条/网站）
- **清迈大学新闻**: 最多 30-40 条
- **总计**: 最多 135-145 条新闻/次

### 实际保存数量
- 取决于 RSS 订阅源的更新频率
- 已存在的新闻会被自动跳过（去重）

## ⚙️ 配置检查

### 1. JPA 配置
```properties
# application.properties
spring.jpa.hibernate.ddl-auto=update  # 自动更新表结构
spring.jpa.show-sql=true              # 显示 SQL 语句（调试用）
```

### 2. 数据库连接
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bridgeu?...
spring.datasource.username=root
spring.datasource.password=123456
```

## 🔍 常见问题

### Q1: 新闻没有保存到数据库？
**检查**:
1. 查看日志是否有错误信息
2. 检查数据库连接是否正常
3. 确认 `news` 表是否存在
4. 检查是否有去重导致跳过

### Q2: 部分新闻没有保存？
**可能原因**:
1. 新闻已存在（根据 URL 去重）
2. 保存时发生异常（查看日志）
3. RSS 订阅源返回空数据

### Q3: 如何强制重新保存已存在的新闻？
**方法**: 删除数据库中的记录，然后重新抓取

```sql
-- 删除特定来源的新闻
DELETE FROM news WHERE source = 'Bangkok Post';

-- 删除所有新闻（谨慎使用）
DELETE FROM news;
```

## 📝 总结

✅ **所有获取的新闻都会自动保存到 MySQL 的 `news` 表中**

保存流程：
1. 获取新闻（RSS 或 HTML）
2. 去重检查
3. 生成 AI 摘要（可选）
4. **保存到数据库** ⭐
5. 转换为社区帖子（可选）

