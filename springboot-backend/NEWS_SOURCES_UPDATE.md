# 新闻源更新说明

## 📰 更新的新闻源

由于原来的 `https://www.cmu.ac.th/news` 网站已不存在，现已更新为以下三个新的新闻源：

### 1. 清迈大学校友会 (CMUAC)
- **网站**: https://cmuac.com/
- **来源标识**: "清迈大学校友会 (CMUAC)"
- **抓取数量**: 最多 15 条/次

### 2. 中国驻清迈总领馆
- **网站**: https://chiangmai.china-consulate.gov.cn/
- **来源标识**: "中国驻清迈总领馆"
- **抓取页面**: 
  - 首页
  - 新闻动态页面 (xwdt/)
- **抓取数量**: 最多 10 条/页面

### 3. 泰国电子签证
- **网站**: https://www.thaievisa.go.th/
- **来源标识**: "泰国电子签证"
- **抓取数量**: 最多 10 条/次

## 🔄 工作流程

`crawlChiangMaiUniversity()` 方法现在会：
1. 从清迈大学校友会网站抓取新闻
2. 从中国驻清迈总领馆网站抓取新闻
3. 从泰国电子签证网站抓取新闻
4. 合并所有新闻并返回

## 📝 新闻格式

每条新闻包含：
- **标题**: 从链接文本或页面元素提取
- **原文链接**: 完整的绝对 URL
- **来源**: 对应的网站标识
- **摘要**: 如果页面有摘要信息，会自动提取（最多 200 字）
- **创建时间**: 当前时间
- **发布时间**: 当前时间（如果页面有日期信息，可以后续改进提取）

## 🧪 测试

### 测试清迈大学新闻爬取

```powershell
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/test-cmu-crawl -ContentType "application/json"
```

### 完整抓取并转换流程

```powershell
$body = @{ limit = 20 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
```

## ⚠️ 注意事项

1. **网站结构变化**: 如果网站结构发生变化，可能需要调整选择器
2. **网络访问**: 确保服务器可以访问这些网站
3. **反爬虫**: 某些网站可能有反爬虫机制，如果遇到 403 错误，可能需要：
   - 调整 User-Agent
   - 增加请求间隔
   - 使用代理

## 🔍 调试

如果某个网站无法抓取到新闻，可以：

1. **查看日志**: 检查后端日志中的错误信息
2. **测试单个网站**: 可以修改代码单独测试某个网站的爬取
3. **检查网站结构**: 使用浏览器开发者工具检查网站的实际 HTML 结构

## 📊 预期结果

每次执行 `crawlChiangMaiUniversity()` 应该能抓取到：
- 清迈大学校友会: 0-15 条
- 中国驻清迈总领馆: 0-20 条（2个页面 × 10条）
- 泰国电子签证: 0-10 条

总计: 0-45 条新闻

