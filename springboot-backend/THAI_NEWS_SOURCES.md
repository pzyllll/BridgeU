# 泰国新闻源配置

## 📰 已配置的新闻网站

### 1. Thai Rath (ไทยรัฐ)
- **主页**: https://www.thairath.co.th
- **RSS**: https://www.thairath.co.th/rss
- **语言**: 泰语
- **备注**: 泰国流量最大新闻站之一，RSS 更新频繁
- **来源标识**: "Thai Rath (ไทยรัฐ)"

### 2. Matichon (มติชน)
- **主页**: https://www.matichon.co.th
- **RSS**: https://www.matichon.co.th/feed
- **语言**: 泰语
- **备注**: 老牌权威媒体，支持全文 RSS
- **来源标识**: "Matichon (มติชน)"

### 3. Khaosod (ข่าวสด)
- **主页**: https://www.khaosod.co.th
- **RSS**: https://www.khaosod.co.th/feed
- **语言**: 泰语
- **备注**: 内容通俗，覆盖社会/娱乐/政治
- **来源标识**: "Khaosod (ข่าวสด)"

### 4. Post Today
- **主页**: https://www.posttoday.com
- **RSS**: https://www.posttoday.com/rss
- **语言**: 泰语
- **备注**: 聚焦财经、商业、市场新闻
- **来源标识**: "Post Today"

### 5. Bangkok Post
- **主页**: https://www.bangkokpost.com
- **RSS**: https://www.bangkokpost.com/rss.xml
- **语言**: 英文
- **备注**: 泰国主流英文报纸，国际读者多
- **来源标识**: "Bangkok Post"

### 6. The Nation Thailand
- **主页**: https://www.nationthailand.com
- **RSS**: https://www.nationthailand.com/rss
- **语言**: 英文/泰语
- **备注**: 原 Nation Multimedia，转型后仍提供 RSS
- **来源标识**: "The Nation Thailand"

### 7. Prachachat (ประชาชาติธุรกิจ)
- **主页**: https://www.prachachat.net
- **RSS**: https://www.prachachat.net/feed
- **语言**: 泰语
- **备注**: 专注经济、金融、商业新闻
- **来源标识**: "Prachachat (ประชาชาติธุรกิจ)"

## 🔄 工作流程

1. **使用 RSS 获取** - 所有新闻源都通过 RSS 订阅源获取
2. **自动保存** - 获取的新闻自动保存到数据库
3. **生成 AI 摘要** - 使用 Qwen 模型生成中文摘要
4. **转换为帖子** - 自动转换为社区帖子并推送到首页

## 📊 预期数据量

每次执行 `crawlAllThaiNews()` 应该能获取：
- 每个网站最多 15 条新闻
- 总计最多 105 条新闻（7个网站 × 15条）

## 🧪 测试

### 测试所有泰国新闻网站

```powershell
# 测试抓取所有新闻
$body = @{ limit = 20 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
```

### 测试单个 RSS 订阅源

```powershell
# 测试 Thai Rath RSS
$body = @{ url = "https://www.thairath.co.th/rss"; source = "Thai Rath" } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/test-rss -ContentType "application/json" -Body $body
```

## ⚙️ 配置位置

RSS 订阅源配置在 `NewsCrawlerService.crawlAllThaiNews()` 方法中。

如需添加新的新闻源，只需在该方法中添加：

```java
rssFeeds.add(new RssFeedService.RssFeedConfig(
    "RSS_URL",      // RSS 订阅源 URL
    "来源标识"      // 新闻来源名称
));
```

## 📝 注意事项

1. **RSS 更新频率** - 不同网站的 RSS 更新频率可能不同
2. **语言支持** - 泰语新闻会通过 AI 生成中文摘要
3. **去重机制** - 系统会根据 URL 自动去重，避免重复保存
4. **数据保存** - 即使 AI 摘要生成失败，新闻数据也会保存到数据库

