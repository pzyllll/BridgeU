# 测试新闻抓取并转换为帖子的 API

# 方法1: 抓取并转换为帖子（推荐）
Write-Host "正在调用: POST /api/news/crawl-and-convert" -ForegroundColor Green
$body = @{ limit = 10 } | ConvertTo-Json
$response = Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/crawl-and-convert -ContentType "application/json" -Body $body
Write-Host "响应状态: $($response.StatusCode)" -ForegroundColor Cyan
Write-Host "响应内容:" -ForegroundColor Yellow
$response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

Write-Host "`n" -NoNewline

# 方法2: 仅转换已抓取的新闻
Write-Host "正在调用: POST /api/news/convert-to-posts" -ForegroundColor Green
$body2 = @{ limit = 10 } | ConvertTo-Json
$response2 = Invoke-WebRequest -Method POST -Uri http://localhost:8080/api/news/convert-to-posts -ContentType "application/json" -Body $body2
Write-Host "响应状态: $($response2.StatusCode)" -ForegroundColor Cyan
Write-Host "响应内容:" -ForegroundColor Yellow
$response2.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10

