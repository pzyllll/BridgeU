# Spring Boot Backend - 留学生互助平台

## 项目说明

这是使用 Spring Boot 构建的后端服务，集成了以下功能：

- **Jsoup**: HTML 解析库，用于网页爬虫
- **Apache HttpClient**: HTTP 客户端，用于网络请求
- **阿里云 DashScope SDK**: 调用 Qwen 模型进行文本总结和智能问答

## 依赖说明

### 核心依赖

1. **Jsoup (1.17.2)**
   - 用于解析和提取 HTML 内容
   - 示例：`CrawlerService.crawlWithJsoup()`

2. **Apache HttpClient 5 (5.2.1)**
   - 用于发送 HTTP 请求
   - 示例：`CrawlerService.crawlWithHttpClient()`

3. **阿里云 DashScope SDK (2.8.0)**
   - 用于调用 Qwen 大语言模型
   - 示例：`QwenService.summarizeText()` 和 `QwenService.answerQuestion()`

## 配置说明

### 1. 环境变量配置

在 `application.yml` 中配置阿里云 DashScope API Key：

```yaml
dashscope:
  api:
    key: your-api-key-here
```

或者通过环境变量设置：

```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

### 2. 获取 API Key

1. 访问 [阿里云 DashScope 控制台](https://dashscope.console.aliyun.com/)
2. 注册/登录账号
3. 创建 API Key
4. 将 API Key 配置到项目中

## 使用方法

### 安装依赖

```bash
mvn clean install
```

### 运行项目

```bash
mvn spring-boot:run
```

### 使用示例

#### 1. 爬虫服务

```java
@Autowired
private CrawlerService crawlerService;

// 使用 Jsoup 爬取网页
String result = crawlerService.crawlWithJsoup("https://example.com");

// 使用 HttpClient + Jsoup 爬取网页
String result = crawlerService.crawlWithHttpClient("https://example.com");
```

#### 2. Qwen 模型服务

```java
@Autowired
private QwenService qwenService;

// 总结文本
String summary = qwenService.summarizeText("长文本内容...");

// 回答问题
String answer = qwenService.answerQuestion("问题", "上下文信息");
```

## 注意事项

1. **API Key 安全**: 不要将 API Key 提交到版本控制系统，使用环境变量或配置文件（已加入 .gitignore）
2. **爬虫合规**: 使用爬虫时请遵守目标网站的 robots.txt 和使用条款
3. **请求频率**: 注意控制 API 调用频率，避免超出限制
4. **错误处理**: 实际使用时请添加完善的异常处理和重试机制

## 项目结构

```
springboot-backend/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── globalbuddy/
│       │           └── service/
│       │               ├── CrawlerService.java
│       │               └── QwenService.java
│       └── resources/
│           └── application.yml
└── README.md
```

