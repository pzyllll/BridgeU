# 项目设置指南

## 📋 完成清单

除了配置阿里云 DashScope API Key 之外，您还需要完成以下步骤：

---

## 1️⃣ 后端设置（Spring Boot）

### 1.1 环境要求
- ✅ **Java 17** 或更高版本
- ✅ **Maven 3.6+**
- ✅ **IDE**（推荐 IntelliJ IDEA 或 Eclipse）

### 1.2 安装依赖
```bash
cd springboot-backend
mvn clean install
```

### 1.3 配置阿里云 API Key

**方式一：在配置文件中设置**
编辑 `src/main/resources/application.properties`：
```properties
dashscope.api.key=your-api-key-here
```

**方式二：使用环境变量（推荐）**
```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY="your-api-key-here"

# Windows CMD
set DASHSCOPE_API_KEY=your-api-key-here

# Linux/Mac
export DASHSCOPE_API_KEY=your-api-key-here
```

### 1.4 启动后端服务
```bash
cd springboot-backend
mvn spring-boot:run
```

或者使用 IDE 直接运行 `GlobalBuddyApplication.java`

**验证启动成功：**
- 控制台显示：`Started GlobalBuddyApplication`
- 访问：`http://localhost:8080/health` 应返回 `{"status":"ok"}`

---

## 2️⃣ 数据库配置

### 2.1 当前配置（H2 内存数据库）
项目已配置 H2 内存数据库，**数据在应用重启后会丢失**。

**访问 H2 控制台：**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:newsdb`
- 用户名: `sa`
- 密码: （留空）

### 2.2 如需持久化存储（可选）

**选项 A：使用 H2 文件数据库**
修改 `application.properties`：
```properties
spring.datasource.url=jdbc:h2:file:./data/newsdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

**选项 B：使用 MySQL**
1. 在 `pom.xml` 中添加 MySQL 依赖：
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. 修改 `application.properties`：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/newsdb?useUnicode=true&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=your-password
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

---

## 3️⃣ 爬虫配置调整

### 3.1 检查目标网站结构
`NewsCrawlerService.crawlBangkokPost()` 方法中的选择器可能需要根据实际网站结构调整：

**文件位置：** `src/main/java/com/globalbuddy/service/NewsCrawlerService.java`

**需要调整的部分：**
```java
// 第 60-65 行：根据实际网站 HTML 结构调整选择器
Elements newsElements = doc.select("article, .news-item, .article-item, .story-list-item");
```

**建议：**
1. 访问目标网站：`https://www.bangkokpost.com/thailand/general`
2. 使用浏览器开发者工具检查 HTML 结构
3. 调整 Jsoup 选择器以匹配实际结构

### 3.2 测试爬虫功能
可以手动调用爬虫服务进行测试（创建测试类或使用 Postman 调用）

---

## 4️⃣ 定时任务配置

### 4.1 当前配置
- **执行时间：** 每天早上 8:00:00
- **Cron 表达式：** `0 0 8 * * ?`

### 4.2 修改执行时间（可选）
编辑 `NewsScheduler.java`：
```java
@Scheduled(cron = "0 0 8 * * ?")  // 修改这里的 cron 表达式
```

**常用 Cron 表达式示例：**
- `0 0 8 * * ?` - 每天 8:00
- `0 0 */6 * * ?` - 每 6 小时
- `0 0 8 * * MON-FRI` - 工作日上午 8:00
- `0 0 12 * * ?` - 每天中午 12:00

### 4.3 手动触发测试（可选）
在 `NewsScheduler` 中已有 `manualTrigger()` 方法，可以创建测试接口调用

---

## 5️⃣ 前端设置（Vue2 + Element UI）

### 5.1 创建 Vue 项目（如果还没有）
```bash
# 使用 Vue CLI
npm install -g @vue/cli
vue create vue-frontend

# 或使用现有目录
cd vue-frontend
npm init -y
```

### 5.2 安装依赖
```bash
cd vue-frontend
npm install vue@2 element-ui axios
```

### 5.3 配置 Element UI
在 `src/main.js` 中：
```javascript
import Vue from 'vue';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';

Vue.use(ElementUI);
```

### 5.4 配置 API 代理（开发环境）
创建 `vue.config.js`：
```javascript
module.exports = {
  devServer: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
};
```

### 5.5 使用组件
将 `DailyBriefing.vue` 放入 `src/components/` 目录，在需要的页面中引入使用。

### 5.6 启动前端
```bash
npm run serve
# 或
npm run dev
```

---

## 6️⃣ 测试验证

### 6.1 测试后端 API
```bash
# 健康检查
curl http://localhost:8080/health

# 获取新闻简报（需要先有数据）
curl http://localhost:8080/api/news/daily-briefing?page=0&size=10
```

### 6.2 测试完整流程
1. **手动触发定时任务**（或等待定时执行）
2. **检查数据库**：访问 H2 控制台查看是否有数据
3. **调用 API**：验证 `/api/news/daily-briefing` 返回数据
4. **前端展示**：在 Vue 组件中查看效果

---

## 7️⃣ 常见问题排查

### 7.1 API Key 相关
- ❌ **问题：** `请配置 dashscope.api.key`
- ✅ **解决：** 检查环境变量或配置文件中的 API Key

### 7.2 爬虫失败
- ❌ **问题：** 爬取不到数据或选择器不匹配
- ✅ **解决：** 
  1. 检查目标网站是否可访问
  2. 调整 `NewsCrawlerService` 中的选择器
  3. 检查 User-Agent 和超时设置

### 7.3 数据库连接问题
- ❌ **问题：** H2 数据库连接失败
- ✅ **解决：** 检查 `application.properties` 中的数据库配置

### 7.4 跨域问题（前端调用后端）
- ❌ **问题：** CORS 错误
- ✅ **解决：** 
  1. 后端已配置 `@CrossOrigin`（如需要）
  2. 前端配置代理（见 5.4）

---

## 8️⃣ 生产环境部署建议

### 8.1 数据库
- 使用 MySQL 或 PostgreSQL 替代 H2
- 配置数据库连接池

### 8.2 安全性
- API Key 使用环境变量或密钥管理服务
- 添加 API 认证和授权
- 配置 HTTPS

### 8.3 性能优化
- 添加 Redis 缓存
- 配置爬虫请求频率限制
- 优化数据库查询

### 8.4 监控和日志
- 配置日志文件输出
- 添加应用监控（如 Spring Boot Actuator）
- 设置错误告警

---

## 📝 快速启动检查清单

- [ ] Java 17 已安装
- [ ] Maven 已安装
- [ ] 阿里云 DashScope API Key 已配置
- [ ] 后端依赖已安装（`mvn clean install`）
- [ ] 后端服务已启动（端口 8080）
- [ ] 数据库配置正确（H2 或 MySQL）
- [ ] 爬虫选择器已调整（如需要）
- [ ] 定时任务已启用
- [ ] 前端依赖已安装
- [ ] 前端代理已配置
- [ ] 前端服务已启动
- [ ] API 测试通过

---

## 🎯 下一步建议

1. **测试爬虫功能**：手动触发一次爬取，检查是否能获取数据
2. **测试 AI 摘要**：验证 Qwen API 调用是否正常
3. **调整爬虫选择器**：根据实际网站结构调整
4. **优化 UI**：根据实际需求调整前端组件样式
5. **添加更多功能**：如搜索、筛选、详情页等

---

**祝您开发顺利！** 🚀

