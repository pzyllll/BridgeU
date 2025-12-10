# RSS 订阅源配置说明

## 📡 RSS 简介

RSS (Really Simple Syndication) 是一种标准化的内容分发格式，许多新闻网站都提供 RSS 订阅源。使用 RSS 获取新闻有以下优势：

1. **合法合规** - RSS 是网站官方提供的内容分发方式
2. **稳定可靠** - 不依赖网页结构变化
3. **效率更高** - 直接获取结构化数据，无需解析 HTML
4. **避免反爬虫** - 不会触发网站的反爬虫机制

## 🔧 已实现的 RSS 功能

### 1. RssFeedService
- 位置：`com.globalbuddy.service.RssFeedService`
- 功能：解析 RSS/Atom 订阅源，转换为 News 对象
- 支持：RSS 2.0、Atom 1.0 等标准格式

### 2. 自动回退机制
- 优先使用 RSS 订阅源
- 如果 RSS 不可用，自动回退到 HTML 爬取
- 确保即使 RSS 失败也能获取新闻

## 📰 RSS 订阅源配置

### Bangkok Post
- **RSS URL**: `https://www.bangkokpost.com/rss/data/thailand.xml`
- **来源标识**: "Bangkok Post"
- **状态**: ✅ 已配置

### 清迈大学相关新闻源

#### 1. 中国驻清迈总领馆
- **查找 RSS**: 访问网站查找 RSS 链接（通常在页脚或 `/rss`、`/feed` 路径）
- **可能的 URL**:
  - `https://chiangmai.china-consulate.gov.cn/rss.xml`
  - `https://chiangmai.china-consulate.gov.cn/feed`
  - `https://chiangmai.china-consulate.gov.cn/xwdt/rss.xml`
- **状态**: ⚠️ 需要确认实际 RSS URL

#### 2. 清迈大学校友会
- **查找 RSS**: 访问 `https://cmuac.com/` 查找 RSS 链接
- **可能的 URL**:
  - `https://cmuac.com/feed`
  - `https://cmuac.com/rss`
  - `https://cmuac.com/rss.xml`
- **状态**: ⚠️ 需要确认实际 RSS URL

#### 3. 泰国电子签证
- **查找 RSS**: 访问 `https://www.thaievisa.go.th/` 查找 RSS 链接
- **可能的 URL**:
  - `https://www.thaievisa.go.th/feed`
  - `https://www.thaievisa.go.th/rss`
- **状态**: ⚠️ 需要确认实际 RSS URL

## 🔍 如何查找 RSS 订阅源

### 方法 1: 网站页面查找
1. 访问目标网站
2. 查看页脚、侧边栏或"订阅"页面
3. 查找 RSS、Feed、订阅等链接

### 方法 2: 常见路径尝试
在浏览器中尝试以下路径：
- `/rss`
- `/feed`
- `/rss.xml`
- `/feed.xml`
- `/atom.xml`

### 方法 3: 查看网页源代码
1. 在浏览器中按 F12 打开开发者工具
2. 查看 `<head>` 部分，查找：
   ```html
   <link rel="alternate" type="application/rss+xml" href="...">
   <link rel="alternate" type="application/atom+xml" href="...">
   ```

## 📝 配置 RSS 订阅源

在 `NewsCrawlerService.crawlChiangMaiUniversityFromRss()` 方法中添加 RSS URL：

```java
rssFeeds.add(new RssFeedService.RssFeedConfig(
    "https://example.com/rss.xml",  // RSS URL
    "来源标识"                        // 新闻来源名称
));
```

## 🧪 测试 RSS 订阅源

### 测试单个 RSS 订阅源

```powershell
# 创建测试接口（需要在 NewsAdminController 中添加）
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/test-rss -ContentType "application/json" -Body '{"url":"https://www.bangkokpost.com/rss/data/thailand.xml","source":"Bangkok Post"}'
```

### 查看 RSS 内容

在浏览器中直接访问 RSS URL，查看是否返回 XML 格式的内容。

## ⚙️ 工作流程

1. **尝试 RSS** - 首先尝试从 RSS 订阅源获取新闻
2. **检查结果** - 如果 RSS 返回了新闻，直接使用
3. **回退爬取** - 如果 RSS 失败或返回空，回退到 HTML 爬取
4. **保存数据** - 将获取的新闻保存到数据库

## 📊 优势对比

| 方式 | RSS | HTML 爬取 |
|------|-----|-----------|
| 合法性 | ✅ 官方支持 | ⚠️ 可能违反 ToS |
| 稳定性 | ✅ 高 | ⚠️ 依赖网页结构 |
| 效率 | ✅ 高 | ⚠️ 需要解析 HTML |
| 反爬虫 | ✅ 无风险 | ⚠️ 可能被限制 |

## 🔄 当前实现

- ✅ Bangkok Post 已配置 RSS
- ⚠️ 清迈大学相关新闻源需要查找并配置 RSS URL
- ✅ 自动回退机制已实现

## 📚 参考资源

- [RSS 2.0 规范](https://www.rssboard.org/rss-specification)
- [Atom 1.0 规范](https://tools.ietf.org/html/rfc4287)
- [Rome RSS 库文档](https://rometools.github.io/rome/)

