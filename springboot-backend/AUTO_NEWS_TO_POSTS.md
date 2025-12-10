# 新闻自动转帖子功能说明（已更新）

## ✨ 新功能

1. **自动抓取清迈大学新闻** - 新增清迈大学新闻源
2. **自动转换为帖子** - 定时任务自动将新闻转换为帖子
3. **推送到首页** - 帖子自动出现在首页的帖子列表中
4. **AI 智能总结** - 每条新闻都有 AI 生成的中文摘要
5. **原文链接** - 每条帖子都包含原文链接

## 🔄 工作流程

### 定时任务（每天上午 8 点）

1. **抓取新闻**
   - Bangkok Post 新闻
   - 清迈大学 (CMU) 新闻
   
2. **生成 AI 摘要**
   - 使用 Qwen 模型生成中文摘要
   - 保存到 `news` 表

3. **自动转换为帖子**
   - 将新闻转换为社区帖子
   - 自动创建"新闻资讯"社区（如果不存在）
   - 帖子状态：自动审核通过

4. **推送到首页**
   - 帖子自动出现在首页的 `PostList` 组件中
   - 用户可以直接在首页看到最新新闻

## 📝 帖子格式

每条帖子包含：

```
📝 **AI 智能总结**

[AI 生成的中文摘要]

---

📄 **详细内容**

[新闻详细内容，前 500 字]

---

🔗 **阅读原文**: [原文链接]
```

## 🎯 手动触发

### 1. 抓取并转换为帖子（推荐）

```powershell
$body = @{ limit = 10 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
```

这会：
- 抓取 Bangkok Post 新闻
- 抓取清迈大学新闻
- 生成 AI 摘要
- 自动转换为帖子
- 推送到首页

### 2. 仅转换已抓取的新闻

```powershell
$body = @{ limit = 10 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/convert-to-posts -ContentType "application/json" -Body $body
```

## 📊 新闻来源

1. **Bangkok Post** - 泰国综合新闻
   - URL: `https://www.bangkokpost.com/thailand/general`
   - 来源标识: "Bangkok Post"

2. **清迈大学 (CMU)** - 大学新闻
   - URL: `https://www.cmu.ac.th/en/news` 和 `https://www.cmu.ac.th/th/news`
   - 来源标识: "清迈大学 (CMU)"

## 🏠 首页显示

帖子会自动显示在首页的 `PostList` 组件中：

- 访问 `http://localhost:5173`
- 首页会显示所有帖子（包括新闻帖子）
- 帖子按创建时间倒序排列
- 最新新闻帖子会出现在最前面

## 🔍 查看帖子

### 通过 API

```powershell
# 查看所有帖子
Invoke-WebRequest -Method GET -Uri http://localhost:8080/api/posts

# 查看新闻资讯社区的帖子
Invoke-WebRequest -Method GET -Uri http://localhost:8080/api/communities/[社区ID]/posts
```

### 通过前端

1. 访问 `http://localhost:5173`
2. 首页会显示所有帖子
3. 新闻帖子会有"新闻资讯"分类和标签

## ⚙️ 配置

### 定时任务配置

在 `NewsScheduler.java` 中：

```java
@Scheduled(cron = "0 0 8 * * ?")  // 每天上午 8 点执行
public void scheduledCrawlAndSummarize() {
    // 自动抓取并转换为帖子
}
```

### 修改执行时间

修改 cron 表达式：
- `0 0 8 * * ?` - 每天 8:00
- `0 0 */6 * * ?` - 每 6 小时
- `0 0 0 * * ?` - 每天 0:00

## 📈 统计信息

每次执行后会输出统计信息：

```
========== 定时任务执行完成 ==========
统计信息 - 成功: 15, 跳过: 3, 失败: 0
新闻转帖子完成: 转换结果 - 总计: 15, 成功: 15, 跳过: 0, 失败: 0
```

## 🛠️ 故障排查

1. **没有清迈大学新闻**
   - 检查网络连接
   - 检查清迈大学网站是否可访问
   - 查看日志中的错误信息

2. **帖子没有出现在首页**
   - 检查帖子状态是否为 `APPROVED`
   - 检查前端是否正确调用 `/api/posts`
   - 查看浏览器控制台错误

3. **AI 摘要为空**
   - 检查 DashScope API Key 是否有效
   - 查看日志中的 API 调用错误

4. **原文链接不显示**
   - 检查新闻的 `originalUrl` 字段是否为空
   - 查看帖子内容是否正确生成

## 📝 注意事项

1. **去重机制** - 系统会自动跳过已存在的新闻（根据 URL）
2. **API 限制** - 注意 DashScope API 的调用频率限制
3. **网络超时** - 爬虫有 15 秒超时设置
4. **内容长度** - 帖子内容会截取前 500 字，完整内容请查看原文链接

