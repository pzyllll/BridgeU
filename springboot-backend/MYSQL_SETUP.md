# MySQL 数据库设置指南

## 1. 安装 MySQL（如果尚未安装）

### Windows
1. 下载 MySQL Installer：https://dev.mysql.com/downloads/installer/
2. 运行安装程序，选择 "Developer Default" 或 "Server only"
3. 设置 root 用户密码为：`123456`（或你想要的密码，记得同步修改 `application.properties`）

### macOS
```bash
brew install mysql
brew services start mysql
mysql_secure_installation
```

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install mysql-server
sudo mysql_secure_installation
```

## 2. 创建数据库

### 方法一：使用 MySQL 命令行
```bash
mysql -u root -p
```
输入密码后执行：
```sql
CREATE DATABASE IF NOT EXISTS bridgeu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 方法二：使用 SQL 脚本
```bash
mysql -u root -p < src/main/resources/db/migration/create_database.sql
```

### 方法三：使用 MySQL Workbench
1. 打开 MySQL Workbench
2. 连接到本地 MySQL 服务器
3. 执行以下 SQL：
```sql
CREATE DATABASE IF NOT EXISTS bridgeu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 3. 验证数据库创建成功

```bash
mysql -u root -p -e "SHOW DATABASES LIKE 'bridgeu';"
```

应该能看到 `bridgeu` 数据库。

## 4. 启动 Spring Boot 应用

启动后，Spring Boot 会自动：
- 根据 JPA 实体创建表结构（`spring.jpa.hibernate.ddl-auto=update`）
- 执行 `DataSeeder` 初始化示例数据

## 5. 验证数据

```bash
mysql -u root -p bridgeu -e "SHOW TABLES;"
```

应该能看到以下表：
- `users`
- `communities`
- `posts`
- `news`
- 等等...

## 注意事项

- 如果 MySQL root 密码不是 `123456`，请修改 `application.properties` 中的 `spring.datasource.password`
- 如果 MySQL 端口不是 3306，请修改 `spring.datasource.url` 中的端口号
- 确保 MySQL 服务正在运行：`net start MySQL` (Windows) 或 `systemctl status mysql` (Linux)

