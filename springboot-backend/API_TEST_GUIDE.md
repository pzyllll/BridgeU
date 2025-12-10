# API 测试指南

## PowerShell 命令（正确写法）

### 方法1: 分两行执行

```powershell
$body = @{ limit = 10 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
```

### 方法2: 使用分号分隔

```powershell
$body = @{ limit = 10 } | ConvertTo-Json; Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
```

### 方法3: 使用脚本文件（推荐）

直接运行 `test_api.ps1`:

```powershell
cd springboot-backend
.\test_api.ps1
```

## 常用 API 测试命令

### 1. 抓取并转换为帖子

```powershell
$body = @{ limit = 10 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
```

### 2. 仅转换已抓取的新闻

```powershell
$body = @{ limit = 10 } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/convert-to-posts -ContentType "application/json" -Body $body
```

### 3. 手动刷新新闻（不转换）

```powershell
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/refresh
```

### 4. 测试 AI 摘要

```powershell
$body = @{ text = "这是一条测试新闻内容" } | ConvertTo-Json
Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/test-ai -ContentType "application/json" -Body $body
```

### 5. 查看所有帖子

```powershell
Invoke-WebRequest -Method GET -Uri http://localhost:8080/api/posts
```

### 6. 查看所有社区

```powershell
Invoke-WebRequest -Method GET -Uri http://localhost:8080/api/communities
```

## 查看响应内容（格式化）

```powershell
$response = Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
```

## 错误处理

如果遇到错误，可以查看详细错误信息：

```powershell
try {
    $body = @{ limit = 10 } | ConvertTo-Json
    $response = Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
    Write-Host "成功: $($response.StatusCode)" -ForegroundColor Green
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "错误: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "响应: $($_.Exception.Response)" -ForegroundColor Yellow
}
```

## 注意事项

1. **确保后端正在运行**: 先启动 `mvn spring-boot:run`
2. **端口**: 默认端口是 8080，如果修改了请相应调整 URL
3. **JSON 格式**: PowerShell 的 `ConvertTo-Json` 会自动处理格式
4. **命令分隔**: PowerShell 中多个命令需要用分号 `;` 或换行分隔

