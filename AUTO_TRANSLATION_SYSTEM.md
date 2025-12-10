# 自动翻译系统说明

## 概述

系统已实现完全自动化的双语翻译功能。当用户发送帖子或系统爬取新闻时，会自动检测语言并翻译成中文和英文，保存到数据库中。用户在前端选择语言时，系统会自动返回对应语言的翻译内容。

## 工作流程

### 1. 用户发帖时的自动翻译

**触发时机：** 用户通过前端发布新帖子时

**处理流程：**
1. 用户提交帖子（可以是中文、英文或泰文）
2. `PostController.createPost()` 或 `CommunityController.createPost()` 接收请求
3. 调用 `LanguageDetectionService.detectLanguage()` 检测内容语言
4. 调用 `TranslationService.translateContent()` 翻译成中文和英文
5. 将原文、中文翻译、英文翻译和原始语言保存到数据库
6. 返回创建成功的响应

**相关代码：**
- `PostController.createPost()`
- `CommunityController.createPost()`
- `LanguageDetectionService.detectLanguage()`
- `TranslationService.translateContent()`

### 2. 爬取新闻时的自动翻译

**触发时机：** 系统定时任务爬取新闻并转换为帖子时

**处理流程：**
1. `NewsScheduler` 定时触发新闻爬取
2. `NewsCrawlerService` 爬取新闻并保存到 `news` 表
3. `NewsToPostService.convertNewsToPosts()` 将新闻转换为帖子
4. 在 `createPostFromNews()` 方法中：
   - 检测新闻内容的语言
   - 调用翻译服务翻译成中文和英文
   - 保存原文、中文翻译、英文翻译和原始语言到数据库

**相关代码：**
- `NewsScheduler.scheduledCrawlAndSummarize()`
- `NewsToPostService.convertNewsToPosts()`
- `NewsToPostService.createPostFromNews()`

### 3. 前端显示时的语言选择

**触发时机：** 用户在前端选择语言（中文/英文）时

**处理流程：**
1. 用户在登录页面或 Header 组件中选择语言
2. 语言选择保存到 `localStorage`
3. 前端调用 API 时自动传递 `lang` 参数（`/api/posts?lang=zh` 或 `/api/posts?lang=en`）
4. `PostController.listPosts()` 或 `PostController.getPost()` 接收 `lang` 参数
5. `toPostResponse()` 方法根据 `lang` 参数返回对应语言的翻译内容：
   - `lang=zh` → 返回 `contentZh`
   - `lang=en` → 返回 `contentEn`
   - 如果没有对应翻译，返回原文

**相关代码：**
- `PostController.listPosts()`
- `PostController.getPost()`
- `PostController.toPostResponse()`
- `frontend/src/utils/language.js`
- `frontend/src/api.js`
- `frontend/src/components/PostList.jsx`

## 数据库字段

`CommunityPost` 实体包含以下翻译相关字段：

- `contentZh` (TEXT): 中文翻译内容
- `contentEn` (TEXT): 英文翻译内容
- `originalLanguage` (VARCHAR(10)): 检测到的原始语言（zh/en/th）
- `title` (VARCHAR): 原始标题（保持原样）
- `body` (TEXT): 原始正文内容

## 翻译服务

### LanguageDetectionService

- **功能：** 检测文本的原始语言
- **支持语言：** 中文（zh）、英文（en）、泰文（th）
- **实现方式：** 使用 DashScope Qwen-Max API

### TranslationService

- **功能：** 将文本翻译成中文和英文
- **模型：** Qwen-Max
- **返回结果：** `TranslationResult` 包含：
  - `bodyZh`: 中文翻译
  - `bodyEn`: 英文翻译
  - `titleZh`: 标题中文翻译（可选）
  - `titleEn`: 标题英文翻译（可选）

## 错误处理

1. **语言检测失败：** 默认使用 "en" 作为原始语言
2. **翻译失败：** 使用原文作为回退方案
3. **API 调用失败：** 记录错误日志，使用原文保存

## 配置要求

确保在 `application.properties` 中配置了 DashScope API Key：

```properties
dashscope.api.key=your-api-key-here
```

## 注意事项

1. **自动翻译：** 所有翻译都是自动完成的，无需手动操作
2. **已删除功能：** 手动批量翻译功能已完全删除，包括：
   - `PostTranslationService` 服务
   - `/api/news/translate-posts` API 端点
   - AdminPanel 中的批量翻译 UI
3. **性能考虑：** 翻译需要调用 DashScope API，可能需要一些时间
4. **API 配额：** 确保 DashScope API Key 有足够的配额

## 测试建议

1. **测试用户发帖：**
   - 用中文发帖，检查是否自动翻译成英文
   - 用英文发帖，检查是否自动翻译成中文
   - 用泰文发帖，检查是否自动翻译成中文和英文

2. **测试新闻爬取：**
   - 等待定时任务执行或手动触发新闻爬取
   - 检查转换的帖子是否包含翻译

3. **测试语言切换：**
   - 在前端选择中文，检查帖子是否显示中文内容
   - 在前端选择英文，检查帖子是否显示英文内容

