@echo off
echo 正在创建 MySQL 数据库 bridgeu...
echo.

REM 尝试使用 MySQL 8.0 的默认路径
set MYSQL_PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe

if not exist "%MYSQL_PATH%" (
    echo MySQL 未找到在默认路径，请手动执行 SQL 脚本
    echo 或修改此脚本中的 MYSQL_PATH 变量
    pause
    exit /b 1
)

echo 使用 MySQL 路径: %MYSQL_PATH%
echo.

"%MYSQL_PATH%" -u root -p123456 -e "CREATE DATABASE IF NOT EXISTS bridgeu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if %errorlevel% equ 0 (
    echo.
    echo 数据库 bridgeu 创建成功！
    echo.
    echo 验证数据库：
    "%MYSQL_PATH%" -u root -p123456 -e "SHOW DATABASES LIKE 'bridgeu';"
    echo.
    echo 下一步：启动 Spring Boot 应用以自动创建表结构
    echo 运行: mvn spring-boot:run
) else (
    echo.
    echo 创建数据库失败，请检查：
    echo 1. MySQL 服务是否正在运行
    echo 2. root 密码是否正确（如果不是 123456，请手动执行 SQL）
    echo.
    echo 手动创建数据库的 SQL：
    echo CREATE DATABASE IF NOT EXISTS bridgeu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    pause
)

