# 修复 posts.status 字段错误

## 问题描述

当报告处理完成后，尝试更新帖子状态为 `REPORTED_REMOVED` 时，出现以下错误：

```
Data truncated for column 'status' at row 1
```

## 原因分析

数据库中的 `posts.status` 字段可能是：
1. **ENUM 类型**：但枚举值中没有包含 `REPORTED_REMOVED`
2. **VARCHAR 类型**：但长度不够存储 `REPORTED_REMOVED`（15个字符）

## 解决方案

### 方案1：使用 SQL 脚本修复（推荐）

执行以下 SQL 脚本修复数据库表结构：

```sql
USE global_buddy;

-- 将 status 字段改为 VARCHAR(20)，支持所有状态值
ALTER TABLE posts 
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW';
```

或者直接运行提供的 SQL 文件：

```bash
mysql -u root -p123456 global_buddy < src/main/resources/db/migration/fix_posts_status_enum.sql
```

### 方案2：让 Hibernate 自动更新（如果 ddl-auto=update）

如果 `application.yml` 中配置了 `spring.jpa.hibernate.ddl-auto=update`，重启应用后 Hibernate 会自动更新表结构。

**注意**：实体类已经更新，明确指定了 `status` 字段长度为 20：
```java
@Column(nullable = false, length = 20)
private Status status = Status.PENDING_REVIEW;
```

## 验证修复

修复后，可以运行以下 SQL 验证：

```sql
USE bridgeu;
SHOW COLUMNS FROM posts LIKE 'status';
```

应该看到 `status` 字段类型为 `varchar(20)`。

✅ **验证结果** - 已确认修复成功：
- Field: `status`
- Type: `varchar(20)` ✅
- Null: `NO`
- Default: `PENDING_REVIEW`

## 状态值说明

`CommunityPost.Status` 枚举包含以下值：
- `PENDING_REVIEW` (14个字符)
- `APPROVED` (8个字符)
- `REJECTED` (8个字符)
- `REPORTED_REMOVED` (15个字符) ← 最长的值

因此，VARCHAR(20) 足够存储所有可能的状态值。

## 后续操作

1. ✅ **执行 SQL 脚本修复数据库** - **已完成**
   - 已在 `bridgeu` 数据库中成功执行修复脚本
   - `status` 字段已从 ENUM 类型改为 VARCHAR(20)
2. ✅ **验证修复结果** - **已完成**
   - 已通过 `SHOW COLUMNS` 查询确认字段类型为 `varchar(20)`
3. 🔄 **重启 Spring Boot 应用** - **待执行**
   - 重启应用以确保 JPA 实体类与数据库结构同步
4. 🧪 **重新测试报告处理功能** - **待执行**
   - 测试报告处理功能，验证 `REPORTED_REMOVED` 状态可以正常保存

## 修复状态

✅ **数据库修复已完成** - 2024年执行
- 数据库：`bridgeu`
- 表：`posts`
- 字段：`status` 已从 `enum('PENDING_REVIEW','APPROVED','REJECTED')` 改为 `varchar(20)`

修复后，报告处理应该可以正常工作，不会再出现 "Data truncated" 错误。

