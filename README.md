# Global Buddy 留学生互助平台

一个示例性的全栈应用，包含：

- **后端**：Node.js + Express + SQLite，提供用户、社区、帖子、语义搜索与 NLP 问答接口。
- **前端**：React + Vite，展示社区内容、语义搜索面板以及问答机器人 Demo。
- **NLP 占位实现**：内置轻量级语义匹配与答案摘要逻辑，可替换为真实向量数据库与大模型。

## 快速开始

1. 安装依赖
   ```bash
   cd backend && npm install
   cd ../frontend && npm install
   ```
2. 初始化数据库（可选）
   ```bash
   cd backend
   npm run seed
   ```
3. 启动服务
   ```bash
   # 后端
   cd backend
   npm run dev
   
   # 新开终端运行前端
   cd frontend
   npm run dev
   ```
4. 打开浏览器访问 `http://localhost:5173`。

## 目录结构

```
hh/
├─ backend/        # Express API + SQLite
│  ├─ src/
│  │  ├─ routes/   # 用户/社区/帖子/搜索/NLP 路由
│  │  ├─ services/ # 语义匹配与问答逻辑
│  │  ├─ db/       # 数据库初始化
│  │  └─ scripts/  # 种子数据脚本
│  └─ data/        # SQLite 数据文件
└─ frontend/       # React + Vite 单页应用
   └─ src/
      ├─ components
      ├─ api.js
      ├─ App.jsx
      └─ styles.css
```

## 后端 API 摘要

- `GET /health`：健康检查
- `GET/POST /api/users`
- `GET/POST /api/communities`，`GET/POST /api/communities/:id/posts`
- `GET/POST /api/posts`
- `GET /api/search?q=...`：语义搜索帖子与社区
- `POST /api/nlp/qa`：根据帖子知识库生成回答

## 进一步扩展

- 将 `services/nlpService.js` 替换为真实的向量检索（如 Pinecone/Milvus）+ 大语言模型（OpenAI/Qwen 等）。
- 为用户体系增加鉴权、社交关系与消息推送。
- 对帖子与社区加入多语言字段、机器翻译与审核流程。
- 在前端增加路由、实时聊天、更多可视化模块。

