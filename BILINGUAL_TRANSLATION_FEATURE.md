# 双语自动翻译引擎功能文档

## 功能概述

实现了 UGC（用户生成内容）双语自动翻译引擎，当用户提交帖子时，系统会自动检测内容语言（中文/英文/泰文），并使用 Qwen-Max 模型调用 DashScope API 将其翻译成中文和英文，同时保存原文、中文版和英文版到数据库。前端根据用户语言偏好（zh/en）从 API 获取对应译文展示。

## 实现的功能

### 后端功能

1. **语言检测服务 (LanguageDetectionService)**
   - 自动检测文本语言（中文/英文/泰文）
   - 基于字符频率和模式匹配进行语言识别
   - 支持中文字符、泰文字符和英文字符的识别

2. **翻译服务 (TranslationService)**
   - 使用 Qwen-Max 模型进行高质量翻译
   - 支持中文、英文、泰文之间的翻译
   - 分别翻译标题和正文，提高翻译准确性
   - 包含翻译失败降级处理机制

3. **数据库字段扩展**
   - `CommunityPost` 实体新增字段：
     - `contentZh`: 中文翻译内容
     - `contentEn`: 英文翻译内容
     - `originalLanguage`: 检测到的原始语言

4. **API 接口更新**
   - `POST /api/posts`: 创建帖子时自动检测语言并翻译
   - `GET /api/posts?lang=xx`: 支持语言参数，返回对应语言的翻译内容
   - `GET /api/posts/{id}?lang=xx`: 获取单个帖子时支持语言参数
   - `POST /api/communities/{id}/posts`: 社区帖子创建时也支持自动翻译

5. **翻译失败降级处理**
   - 如果翻译失败，使用原文作为降级方案
   - 如果检测到的语言已经是目标语言，直接使用原文
   - 确保系统在 API 调用失败时仍能正常工作

### 前端功能

1. **语言偏好管理**
   - 使用 `localStorage` 保存用户语言选择
   - 支持中文（zh）和英文（en）两种语言
   - 语言选择器位于 Header 组件中

2. **自动语言切换**
   - 前端 API 调用自动携带语言参数
   - 帖子列表根据用户语言偏好显示对应翻译
   - 语言切换时自动刷新内容

3. **发布表单优化**
   - 发布表单无需手动选择语言
   - 系统自动检测用户输入的语言
   - 显示提示信息，告知用户系统会自动翻译

## 技术实现细节

### 后端实现

#### 1. LanguageDetectionService.java
```java
- detectLanguage(String text): 检测文本语言
- containsChinese(String text): 检查是否包含中文字符
- containsThai(String text): 检查是否包含泰文字符
- isPrimarilyEnglish(String text): 检查是否主要为英文
```

#### 2. TranslationService.java
```java
- translateToChinese(String text, String sourceLang): 翻译为中文
- translateToEnglish(String text, String sourceLang): 翻译为英文
- translateContent(String title, String body, String sourceLang): 翻译标题和正文
```

#### 3. PostController.java
- `createPost()`: 创建帖子时自动检测语言并翻译
- `listPosts()`: 支持 `lang` 参数，返回对应语言的翻译
- `getPost()`: 支持 `lang` 参数，返回对应语言的翻译

### 前端实现

#### 1. utils/language.js
- `getLanguagePreference()`: 获取用户语言偏好
- `setLanguagePreference(lang)`: 设置用户语言偏好
- `getLanguageName(lang)`: 获取语言名称

#### 2. api.js
- `fetchPosts()`: 自动携带语言参数

#### 3. Header.jsx
- 语言选择器组件
- 支持中英文切换

#### 4. PostList.jsx
- 监听语言变化事件
- 根据语言偏好加载对应翻译

#### 5. NewPostForm.jsx
- 显示自动翻译提示信息

## 配置要求

### 后端配置

确保 `application.properties` 中配置了 DashScope API Key：
```properties
dashscope.api.key=sk-your-api-key-here
```

### 数据库迁移

系统使用 `spring.jpa.hibernate.ddl-auto=update`，会自动创建新字段：
- `posts.content_zh` (TEXT)
- `posts.content_en` (TEXT)
- `posts.original_language` (VARCHAR(10))

## 使用流程

1. **用户发布帖子**
   - 用户在发布表单中输入内容（可以是中文、英文或泰文）
   - 系统自动检测语言
   - 调用 DashScope API 翻译为中文和英文
   - 保存原文、中文翻译和英文翻译到数据库

2. **用户浏览帖子**
   - 用户在 Header 中选择语言偏好（中文/英文）
   - 系统从 localStorage 读取语言偏好
   - API 调用时自动携带语言参数
   - 返回对应语言的翻译内容

3. **语言切换**
   - 用户点击 Header 中的语言选择器
   - 触发 `languageChanged` 事件
   - 所有相关组件自动刷新，显示新语言的翻译

## 错误处理

1. **API 调用失败**
   - 使用原文作为降级方案
   - 记录错误日志
   - 不影响帖子创建流程

2. **语言检测失败**
   - 默认使用英文
   - 记录警告日志

3. **翻译返回空值**
   - 使用原文作为降级方案
   - 确保系统正常运行

## 性能考虑

1. **翻译 API 调用**
   - 翻译是异步进行的，不会阻塞帖子创建
   - 如果翻译失败，使用原文作为降级方案

2. **数据库存储**
   - 原文、中文翻译和英文翻译都存储在数据库中
   - 前端根据语言偏好选择显示哪个版本

3. **缓存策略**
   - 语言偏好存储在 localStorage 中
   - 减少不必要的 API 调用

## 未来改进

1. **支持更多语言**
   - 可以扩展支持更多语言（如日语、韩语等）

2. **翻译质量优化**
   - 可以添加翻译质量评估
   - 支持用户反馈翻译质量

3. **批量翻译**
   - 对于历史帖子，可以批量翻译

4. **翻译缓存**
   - 对于相同内容的翻译，可以使用缓存

## 测试建议

1. **测试语言检测**
   - 输入中文、英文、泰文内容，验证检测准确性

2. **测试翻译功能**
   - 验证翻译质量
   - 测试翻译失败降级机制

3. **测试前端语言切换**
   - 验证语言偏好保存
   - 验证内容自动刷新

4. **测试 API 接口**
   - 测试不同语言参数下的 API 响应
   - 验证翻译内容的正确性

