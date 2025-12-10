# 检查泰语帖子翻译状态的 PowerShell 脚本
# 需要 MySQL 客户端

$dbName = "bridgeu"
$dbUser = "root"
$dbPass = "root"
$dbHost = "localhost"
$dbPort = "3306"

Write-Host "`n=== 检查泰语帖子翻译状态 ===" -ForegroundColor Cyan
Write-Host "`n正在查询数据库..." -ForegroundColor Yellow

# 构建 MySQL 命令
$mysqlCmd = "mysql -h$dbHost -P$dbPort -u$dbUser -p$dbPass $dbName"

# 查询1: 查看所有泰语帖子及其翻译状态
Write-Host "`n1. 查看所有泰语帖子及其翻译状态:" -ForegroundColor Green
$query1 = @"
SELECT 
    id,
    LEFT(title, 50) as title_preview,
    LENGTH(title_zh) as title_zh_length,
    LENGTH(title_en) as title_en_length,
    LENGTH(content_zh) as content_zh_length,
    LENGTH(content_en) as content_en_length,
    original_language,
    CASE 
        WHEN title_zh IS NULL OR title_zh = '' THEN '❌ 无中文标题'
        ELSE '✅ 有中文标题'
    END as title_zh_status,
    CASE 
        WHEN title_en IS NULL OR title_en = '' THEN '❌ 无英文标题'
        ELSE '✅ 有英文标题'
    END as title_en_status,
    CASE 
        WHEN content_zh IS NULL OR content_zh = '' THEN '❌ 无中文内容'
        ELSE '✅ 有中文内容'
    END as content_zh_status,
    CASE 
        WHEN content_en IS NULL OR content_en = '' THEN '❌ 无英文内容'
        ELSE '✅ 有英文内容'
    END as content_en_status
FROM community_posts
WHERE title REGEXP '[ก-๙]' OR original_language = 'th'
ORDER BY created_at DESC
LIMIT 10;
"@

# 查询2: 统计需要翻译的帖子数量
Write-Host "`n2. 统计需要翻译的帖子数量:" -ForegroundColor Green
$query2 = @"
SELECT 
    COUNT(*) as total_thai_posts,
    SUM(CASE WHEN title_zh IS NULL OR title_zh = '' THEN 1 ELSE 0 END) as missing_title_zh,
    SUM(CASE WHEN title_en IS NULL OR title_en = '' THEN 1 ELSE 0 END) as missing_title_en,
    SUM(CASE WHEN content_zh IS NULL OR content_zh = '' THEN 1 ELSE 0 END) as missing_content_zh,
    SUM(CASE WHEN content_en IS NULL OR content_en = '' THEN 1 ELSE 0 END) as missing_content_en,
    SUM(CASE WHEN (title_zh IS NULL OR title_zh = '') AND (title_en IS NULL OR title_en = '') THEN 1 ELSE 0 END) as missing_both_title,
    SUM(CASE WHEN (content_zh IS NULL OR content_zh = '') AND (content_en IS NULL OR content_en = '') THEN 1 ELSE 0 END) as missing_both_content
FROM community_posts
WHERE title REGEXP '[ก-๙]' OR original_language = 'th';
"@

# 查询3: 查找具体几个帖子的详细内容
Write-Host "`n3. 查找具体帖子的详细内容:" -ForegroundColor Green
$query3 = @"
SELECT 
    id,
    LEFT(title, 80) as title,
    LEFT(body, 100) as body_preview,
    LEFT(content_zh, 100) as content_zh_preview,
    LEFT(content_en, 100) as content_en_preview,
    LEFT(title_zh, 50) as title_zh_preview,
    LEFT(title_en, 50) as title_en_preview,
    original_language,
    created_at
FROM community_posts
WHERE title LIKE '%เถ้าแก่น้อย%'
   OR title LIKE '%สสส%'
   OR title LIKE '%โอ-ออ%'
   OR title LIKE '%ไชยยงค์%'
ORDER BY created_at DESC
LIMIT 5;
"@

# 执行查询
try {
    # 查询1
    Write-Host "`n执行查询1..." -ForegroundColor Yellow
    $query1 | & $mysqlCmd 2>&1
    
    # 查询2
    Write-Host "`n执行查询2..." -ForegroundColor Yellow
    $query2 | & $mysqlCmd 2>&1
    
    # 查询3
    Write-Host "`n执行查询3..." -ForegroundColor Yellow
    $query3 | & $mysqlCmd 2>&1
    
    Write-Host "`n✅ 查询完成！" -ForegroundColor Green
} catch {
    Write-Host "`n❌ 查询失败: $_" -ForegroundColor Red
    Write-Host "`n提示: 请确保 MySQL 已安装并在 PATH 中，或者手动运行 SQL 文件" -ForegroundColor Yellow
}

