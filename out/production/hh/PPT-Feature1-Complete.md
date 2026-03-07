# BridgeU – Daily Briefing System 中期答辩 PPT 大纲
## Feature 1 完整版（精确到每页内容）

---

## 第 1 页：封面
- **标题**：BridgeU – Daily Briefing System 中期答辩
- **副标题**：从需求到系统方法的完整链路（Feature 1）
- **信息**：
  - 学生：Pan Zhiyi（652115558）
  - 专业：Software Engineering Program
  - 指导老师：Asst. Prof. Pattama Longani, Ph.D.
  - 学院、学校、日期

---

## 第 2 页：软件功能概览
- **BridgeU 平台定位**
  - 面向在泰国际学生的双语（中/英）信息与社区平台
  - 目标：消除语言障碍，提供本地新闻与社区支持
- **三大核心功能**（一句话带过）
  - Feature 1：Daily Briefing System – 自动抓取并双语展示泰国新闻
  - Feature 2：Community Interaction Platform – 双语社区互动
  - Feature 3：Authentication & Profile – 注册认证与资料管理
- **本次汇报聚焦**
  - 只聚焦 Feature 1，从需求 → 用例 → SRS → 系统方法 → 测试与实现

---

## 第 3 页：Feature 1 功能介绍（软件功能 → 选取典型需求）
- **Feature 1：Daily Briefing System 概述**
  - 每天早上定时从 Google News RSS 与 NewsAPI 抓取泰国新闻
  - 调用 Qwen AI 自动生成**中/英双语标题和摘要**
  - 提供分页列表、详情页、关键词搜索、日期与来源过滤、查看原文等功能
- **典型用户价值**
  - 国际学生在 1-3 分钟内浏览当天重要新闻，不需要懂泰文
- **为后续链条选取的"典型需求"**
  - **需求 R1**：用户能在指定日期范围内浏览当天的 Daily Briefing 新闻列表，并按关键词搜索

---

## 第 4 页：典型需求 R1 – 自然语言表述
- **典型需求 R1（中文自然语言版）**
  - "系统应支持用户在指定的日期范围内浏览每日新闻简报列表，并可以输入关键词对新闻标题和摘要进行搜索，结果按发布时间从新到旧排序展示。"
- **R1 涉及的功能点**
  - 时间维度：开始日期、结束日期过滤
  - 内容维度：关键词搜索，搜索范围包括中/英标题与摘要
  - 展示维度：分页、按发布时间倒序、显示来源、封面与摘要
- **R1 在整个平台中的地位**
  - 是 Daily Briefing 页面最常用、也是测试频率最高的核心场景之一

---

## 第 5 页：用例描述 – UC-01 Find Daily Briefing
- **用例名称**：UC-01 Find Daily Briefing
- **主要参与者**：User（已登录用户）
- **触发条件**：用户从导航栏/首页进入"Daily Briefing"页面
- **基本流程（与 R1 对应）**
  1. 用户打开 Daily Briefing 页面
  2. 系统按默认日期范围（例如最近 7 天）加载新闻列表，按发布时间倒序显示
  3. 用户可调整日期范围（startDate、endDate）
  4. 系统重新查询并刷新列表
- **扩展点**
  - 扩展用例 UC-05：按日期过滤
  - 扩展用例 UC-06：在日期范围内根据关键词搜索

---

## 第 6 页：用例描述 – UC-05 & UC-06（展示需求流程）
- **用例 UC-05：Filter Daily Briefing News by Date**
  - 前置条件：用户已进入 Daily Briefing 页面（UC-01）
  - 基本流程：
    1. 用户选择开始日期和结束日期
    2. 系统校验日期合法性（开始 ≤ 结束）
    3. 系统调用后端接口，返回该时间段内的新闻列表
- **用例 UC-06：Search for Daily Briefing News**（extend UC-05）
  - 前置条件：已选择日期范围，或使用默认范围
  - 基本流程：
    1. 用户在搜索框输入关键词
    2. 系统在中/英标题和摘要字段中执行搜索
    3. 按匹配度 + 发布时间倒序返回结果
- **图示建议**：
  - 本页放用例流程图/简单时序图（User → Frontend → Backend → DB）

---

## 第 7 页：从用例提炼 SRS（功能需求条目示例）
- **从 UC-01/05/06 提炼出的 SRS 功能需求（示例）**
  - **FR-1**：系统应提供 `GET /api/news/daily-briefing` 接口，支持分页获取 Daily Briefing 列表。
  - **FR-2**：接口参数应支持 `startDate` 与 `endDate`（格式 yyyy-MM-dd），用于按发布日期过滤新闻。
  - **FR-3**：接口参数应支持 `keyword`，用于在中/英标题和摘要字段中执行包含匹配搜索。
  - **FR-4**：返回结果应按 `publishTime` 字段从新到旧排序。
  - **FR-5**：列表项最少包含：标题、摘要、来源、发布日期、封面图片 URL、原文链接。
- **说明**
  - 这一步体现"从用例行为 → 转换为可实现/可测试的 SRS 条目"的过程

---

## 第 8 页：从 SRS 提取系统方法（接口与内部方法）
- **对外接口方法（Service API 层）**
  - `GET /api/news/daily-briefing`
    - 入参：page, size, startDate, endDate, keyword, source, lang
    - 出参：分页结果 `Page<DailyBriefingDto>`
- **后端内部核心方法（根据 SRS 提炼）**
  - `DailyBriefingService.findDailyBriefings(filter)`（如果存在 Service 层）
  - `NewsController.getDailyBriefing(...)`（实际实现）
  - `NewsRepository.findByKeywordAndPublishDateBetween(keyword, startDate, endDate, pageable)`
  - `NewsScheduler.scheduledCrawlAndSummarize()`（定时任务）
  - `QwenAiClient.generateSummaryAndTranslation(rawContent)`
- **说明**
  - 这一页强调：SRS 不是停在文档，而是被系统方法"实现落地"的过程

---

## 第 9 页：需求-方法对应关系（Traceability）
- **表格形式（建议直接画成表）**

| 需求/SRS 条目 | 实现方法 | 数据操作 |
|--------------|---------|---------|
| **R1 / FR-2 – 按日期过滤 Daily Briefing** | `NewsController.getDailyBriefing()` 参数包含 startDate/endDate | `NewsRepository.findByPublishDateBetweenOrdered()` 基于 `publish_time` 的查询 |
| **FR-3 – 关键词搜索中/英标题与摘要** | `NewsController.getDailyBriefing()` 参数包含 keyword | `NewsRepository.findByKeywordAndPublishDateBetween()` 搜索 `titleZh`, `titleEn`, `summaryZh`, `summaryEn` |
| **FR-4 – 按发布时间倒序排序** | `NewsRepository` 查询方法使用 `Pageable` 排序 | `ORDER BY publish_time DESC`（JPA Pageable Sort） |

- **结论**
  - 每一个 SRS 功能条目，都能在代码中的"系统方法 + 数据字段"找到对应实现

---

## 第 10 页：系统架构（聚焦 Feature 1）
- **总体架构（局部视图，仅 Feature 1）**
  - **前端（Vue 3 + Element Plus）**
    - Daily Briefing 列表页与详情页
    - 通过 Axios 调用 `/api/news/daily-briefing`、`/daily-briefing/{id}`
  - **后端（Spring Boot）**
    - 控制层：`NewsController`
    - 业务层：`DailyBriefingService`（如果存在）
    - 数据层：`NewsRepository`（JPA）
    - 定时任务：`NewsScheduler`
    - 外部 AI 客户端：`QwenAiClient`
  - **数据库（MySQL 8）**
    - 表：`news` + 相关配置表
- **图示建议**
  - 一张分层架构图：Browser → Frontend → Backend → DB & External APIs

---

## 第 11 页：数据结构设计（Daily Briefing 表）
- **数据库表：`news`（示例字段）**
  - `id`：主键（Long）
  - `original_title`：原始标题（String）
  - `source`：新闻来源名称（String，如 "Bangkok Post"）
  - `original_url`：原文链接（String）
  - `cover_image_url`：封面图片链接（String）
  - `publish_date`：新闻发布时间（Date）
  - `title_zh`：中文标题（String）
  - `title_en`：英文标题（String）
  - `summary_zh`：中文摘要（String）
  - `summary_en`：英文摘要（String）
  - `create_time` / `update_time`：系统记录时间（Date）
- **数据结构与需求/方法的关系**
  - 日期过滤 → 依赖 `publish_date` 字段
  - 关键词搜索 → 依赖 `title_zh/en` 和 `summary_zh/en` 字段
  - "查看原文" → 依赖 `original_url`
- **图示建议**
  - 简单 ER 图：`news` 为核心实体，关联到 `news_source` 等辅助表（可选）

---

## 第 12 页：类图概览
- **标题**：Feature 1 - Class Diagram Overview
- **内容**：
  - Feature 1 的类图按**分层架构**拆分为 4 个子图展示
  - 每个子图聚焦一个层次，保持简洁清晰（参考图一图二风格）
- **子图列表**：
  1. Views - 前端视图层（DailyBriefing + DailyBriefingDetail）
  2. Controller - 控制器层（NewsController + NewsBriefDTO）
  3. Repository - 数据访问层（NewsRepository + News）
  4. Services - 服务层（LanguageDetectionService + NewsScheduler）

---

## 第 13 页：类图子图 1 - 前端视图层
- **标题**：Figure X: Views - Daily Briefing Frontend Components
- **类图内容**：
  ```
  DailyBriefing (前端列表页组件)
    - newsList: Array
    - loading: Boolean
    - currentPage: Number
    - searchKeyword: String
    - filterStartDate: Date
    - filterEndDate: Date
    + fetchDailyBriefing(): Promise<void>
    + handleSearch(): void
    + applyFilters(): void
    + viewDetail(newsId: Number): void

  DailyBriefingDetail (前端详情页组件)
    - news: Object
    - loading: Boolean
    - currentLang: String
    + fetchNewsDetail(): Promise<void>
    + goBack(): void
    + openOriginalUrl(url: String): void

  DailyBriefing --> DailyBriefingDetail : "navigates to"
  ```
- **说明**：
  - 前端组件负责用户交互和数据展示
  - 列表页通过路由导航到详情页

---

## 第 14 页：类图子图 2 - 控制器层
- **标题**：Figure X+1: Controller - NewsController
- **类图内容**：
  ```
  NewsController (后端控制器)
    - newsRepository: NewsRepository
    - languageDetectionService: LanguageDetectionService
    + getDailyBriefing(...): ResponseEntity
    + getNewsDetail(...): ResponseEntity
    + getNewsSources(): ResponseEntity
    - convertToDTO(news: News, lang: String): NewsBriefDTO

  NewsBriefDTO (数据传输对象)
    + id: Long
    + titleZh: String
    + titleEn: String
    + summaryZh: String
    + summaryEn: String
    + source: String
    + publishDate: Date

  NewsController --> NewsBriefDTO : "creates"
  ```
- **说明**：
  - Controller 处理 HTTP 请求，协调数据获取和转换
  - DTO 用于前后端数据交换，避免直接暴露实体类

---

## 第 15 页：类图子图 3 - 数据访问层
- **标题**：Figure X+2: Repository - NewsRepository Interface
- **类图内容**：
  ```
  NewsRepository <<interface>> (数据访问接口)
    + findByPublishDateBetweenOrdered(...): Page<News>
    + findByKeywordAndPublishDateBetween(...): Page<News>
    + findByKeywordAndSourceAndPublishDateBetween(...): Page<News>
    + findDistinctSources(): List<String>

  News (新闻实体类)
    + id: Long
    + titleZh: String
    + titleEn: String
    + summaryZh: String
    + summaryEn: String
    + originalUrl: String
    + source: String
    + publishDate: Date

  NewsRepository --> News : "queries"
  ```
- **说明**：
  - Repository 定义数据访问契约，通过 JPA 查询 News 实体
  - News 对应数据库表，包含双语字段

---

## 第 16 页：类图子图 4 - 服务层
- **标题**：Figure X+3: Services - Language Detection & Scheduling
- **类图内容**：
  ```
  LanguageDetectionService (语言检测服务)
    + hasAnyThai(text: String): boolean
    + containsChinese(text: String): boolean
    + containsThai(text: String): boolean

  NewsScheduler (定时任务调度器)
    + scheduledCrawlAndSummarize(): void

  NewsScheduler --> LanguageDetectionService : "uses"
  ```
- **说明**：
  - LanguageDetectionService 用于识别文本语言，确保不显示泰文原文
  - NewsScheduler 每天 8:00 执行爬虫和摘要任务，使用语言检测服务进行内容过滤

---

## 第 17 页：测试设计（用例到测试）
- **测试设计思路**
  - 基于 UC-01/05/06 和对应的 SRS 条目，设计系统测试用例（STC）
  - 覆盖：正常路径 + 边界情况 + 异常情况
- **示例系统测试用例**
  - **STC-01：按默认日期范围加载列表**
    - 前置条件：数据库中存在最近 7 天的 Daily Briefing 记录
    - 步骤：用户首次打开 Daily Briefing 页面
    - 预期：系统返回最近 7 天记录，按 `publish_time` 降序，分页显示
  - **STC-02：按日期范围过滤 + 关键词搜索**
    - 步骤：设置 startDate、endDate，输入关键词，点击搜索
    - 预期：返回记录全部在日期范围内，标题或摘要包含关键词，排序正确
  - **STC-03：非法日期输入**
    - 步骤：startDate > endDate
    - 预期：前端提示错误或后端返回参数错误，列表不更新/使用上一次正确条件

---

## 第 18 页：测试设计（方法级/组件级）
- **单元测试（Unit Test）示例**
  - `NewsController.getDailyBriefing()` 测试
    - 输入：不同组合的 startDate/endDate/keyword/source
    - 断言：生成的查询条件与预期一致，排序和分页参数正确
  - `NewsRepository.findByKeywordAndPublishDateBetween()` 测试
    - 输入：keyword、日期范围、pageable
    - 断言：返回的 Page<News> 包含匹配的记录，排序正确
  - `LanguageDetectionService.hasAnyThai()` 测试
    - 输入：包含/不包含泰文的文本
    - 断言：返回 boolean 值与预期一致
- **集成测试（Integration Test）示例**
  - 通过 Mock 外部 RSS 和 Qwen AI 接口，验证 `NewsScheduler` 能够成功抓取、处理并写入 `news` 表
- **目标**
  - 确保从"需求 → 方法"的每个关键节点，都有对应的测试覆盖

---

## 第 19 页：总结 – 从需求到系统方法的闭环（Feature 1）
- **链条回顾（只针对 Feature 1）**
  1. **软件功能**：为在泰国际学生提供双语每日新闻简报
  2. **典型需求**：按日期和关键词浏览 Daily Briefing 列表（R1）
  3. **用例**：UC-01 / UC-05 / UC-06 描述完整用户流程
  4. **SRS**：提炼成可实现、可测试的功能条目（FR-1 ~ FR-5 …）
  5. **系统方法**：API + Controller + Repository + 定时任务 + AI 客户端
  6. **架构 & 数据结构**：分层架构 + `news` 表设计
  7. **类图设计**：按层次拆分为 4 个子图（Views / Controller / Repository / Services）
  8. **测试设计**：系统测试 + 单元/集成测试构成闭环验证
- **当前状态**
  - Feature 1 的需求、设计和核心实现路径已经清晰；部分模块已进入实现与测试阶段
- **Q&A**
  - 欢迎老师和同学就需求链路、架构设计或 AI 使用提出建议

---

## PPT 制作建议

### 类图制作
- 使用 **PlantUML** 或 **draw.io** 生成类图
- 每个子图保持 **2-4 个类**，参考图一图二的简洁风格
- 类图可以放在同一页 PPT 的左右两侧，或分页展示

### 图表建议
- **第 6 页**：用例流程图/时序图
- **第 10 页**：系统架构图（分层）
- **第 11 页**：ER 图（简化版，只展示 `news` 表）
- **第 13-16 页**：4 个类图子图（按上述内容）

### 时间分配建议（假设 15 分钟答辩）
- 第 1-3 页（功能介绍）：2 分钟
- 第 4-6 页（需求与用例）：3 分钟
- 第 7-9 页（SRS 与方法）：3 分钟
- 第 10-16 页（架构与类图）：4 分钟
- 第 17-18 页（测试设计）：2 分钟
- 第 19 页（总结）：1 分钟

