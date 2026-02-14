# AI 审核流程说明

## 概述

当用户提交举报后，系统会立即触发 AI 自动审核流程。以下是详细的处理步骤：

## 流程步骤

### 1. 用户提交举报
- 用户在前端提交举报，包含：
  - 举报原因（reasons）：如 "Illegal Service Promotion"、"Fraud or Scam" 等
  - 可选描述（description）
  - 目标类型（targetType）：POST 或 COMMENT
  - 目标ID（targetId）

### 2. 创建报告记录
- 后端 `ReportController.submitReport()` 创建报告
- 初始状态：`Status.PENDING`（待审核）
- 保存到数据库

### 3. 触发 AI 审核（异步）
- 立即调用 `ReportModerationService.processReport(reportId)`
- 使用 `@Async` 注解，异步执行，不阻塞用户请求
- 返回消息："Report submitted successfully. AI review is in progress."

### 4. AI 审核处理流程

#### 4.1 获取被举报内容
```java
if (targetType == POST) {
    // 获取帖子内容和标题
    content = post.getBody();
    title = post.getTitle();
} else {
    // 获取评论内容
    content = comment.getContent();
}
```

#### 4.2 构建 AI Prompt
AI Prompt 包含：
- 举报原因列表
- 举报者的描述（如果有）
- 被举报的内容（标题+正文或评论内容）
- 要求 AI 返回 JSON 格式的判断结果

Prompt 示例：
```
You are a content moderation AI. A user has reported content for the following reasons: [举报原因]
Reporter's description: [举报描述]

Please review the reported content and determine if it violates community guidelines.

Respond ONLY in JSON format with the following fields:
- is_safe: boolean
- confidence_score: number 0-100
- reason: string
- violation_snippet: string
- guideline_violated: string

Title: [帖子标题]
Content: [内容]
```

#### 4.3 调用 AI 服务
- 使用 `QwenService.answerQuestion(prompt, "")` 调用通义千问模型
- 获取 AI 的 JSON 响应

#### 4.4 解析 AI 响应
解析 JSON 结果：
```json
{
  "is_safe": false,
  "confidence_score": 85,
  "reason": "Contains spam content",
  "violation_snippet": "Buy cheap products now!",
  "guideline_violated": "Spam"
}
```

#### 4.5 更新报告状态
根据 AI 判断结果更新报告：
- `aiResult`: 保存完整的 AI 响应
- `aiConfidence`: 置信度分数（0-100）
- `isViolation`: 是否违规（!result.isSafe）
- `violationSnippet`: 违规片段
- `reviewedAt`: 审核时间
- `status`: 
  - 如果 `isSafe = true` → `Status.DISMISSED`（驳回举报）
  - 如果 `isSafe = false` → `Status.REVIEWED`（已审核，确认违规）

### 5. 处理违规内容

#### 5.1 如果内容合规（isSafe = true）
- 状态设为 `DISMISSED`
- 通知举报者：举报失败，内容合规
- 不采取任何行动

#### 5.2 如果内容违规（isSafe = false）
- 状态设为 `REVIEWED`
- **处理帖子违规**：
  - 将帖子状态设为 `REPORTED_REMOVED`（已下架/折叠）
  - 通知举报者：举报成功
  - 通知帖子作者：内容违规，已被处理
  
- **处理评论违规**：
  - 将评论状态设为 `REPORTED_REMOVED`（已删除）
  - 通知举报者：举报成功
  - 通知评论作者：内容违规，已被处理

### 6. 错误处理
如果 AI 审核过程中出现异常：
- 记录错误日志
- 将报告状态设为 `REVIEWED`
- 在 `reviewNotes` 中记录错误信息

## 关键代码位置

1. **提交举报**：`ReportController.submitReport()`
2. **AI 审核服务**：`ReportModerationService.processReport()`
3. **AI Prompt 构建**：`ReportModerationService.buildModerationPrompt()`
4. **AI 响应解析**：`ReportModerationService.parseAiResponse()`
5. **违规处理**：`ReportModerationService.handleViolation()`

## 状态流转

```
PENDING (待审核)
    ↓
[AI 审核中...]
    ↓
┌─────────────────┬─────────────────┐
│                 │                 │
DISMISSED      REVIEWED         REVIEWED
(内容合规)      (确认违规)        (处理错误)
```

## 注意事项

1. **异步处理**：AI 审核是异步的，不会阻塞用户请求
2. **自动触发**：报告创建后立即触发，无需手动操作
3. **错误恢复**：如果 AI 审核失败，报告会标记为已审核并记录错误
4. **内容恢复**：系统提供了 `restoreContent()` 方法，可以恢复误判的内容

## 问题排查和解决方案

### 如果报告一直处于 PENDING 状态

如果报告提交后长时间（超过5分钟）仍处于 PENDING 状态，可能的原因：

1. **异步任务未执行**：检查应用日志，查看是否有 "Processing report" 的日志
2. **AI 服务调用失败**：检查 DashScope API Key 是否配置正确，网络是否正常
3. **数据库事务问题**：检查数据库连接是否正常

### 解决方案

#### 方案1：自动处理（推荐）
系统已配置定时任务，每10分钟自动处理超过5分钟的 PENDING 报告：
- 定时任务类：`ReportScheduler`
- 执行频率：每10分钟
- 处理条件：创建时间超过5分钟且状态为 PENDING

#### 方案2：手动重试单个报告（管理员）
```bash
POST /api/reports/{reportId}/retry
```
需要管理员权限，可以手动触发单个报告的重新审核。

#### 方案3：批量重试卡住的报告（管理员）
```bash
POST /api/reports/retry-stuck
```
需要管理员权限，可以批量处理所有卡住的报告。

### 已实现的改进

1. ✅ **异步配置**：已创建 `AsyncConfig` 类，配置了专用的线程池
2. ✅ **定时任务**：已添加 `ReportScheduler`，自动处理卡住的报告
3. ✅ **错误处理**：改进了错误处理和日志记录
4. ✅ **批量处理**：提供了批量重试 API

### 日志检查

如果遇到问题，请检查以下日志：
- `Processing report: id={}` - 开始处理报告
- `Calling AI service for report moderation` - 调用 AI 服务
- `AI service response received` - AI 响应接收成功
- `Failed to process report` - 处理失败（查看详细错误信息）

