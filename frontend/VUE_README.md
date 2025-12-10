# Vue2 + Element UI 新闻简报组件

## DailyBriefing.vue 组件说明

这是一个用于展示当天新闻简报的 Vue2 组件，使用 Element UI 进行样式设计。

### 功能特性

1. ✅ 在 `mounted` 钩子中自动调用后端接口 `/api/news/daily-briefing`
2. ✅ 使用 `el-card` 列表展示新闻
3. ✅ 展示字段：标题、AI 中文总结、来源、发布时间
4. ✅ 提供 "Read Original" 链接，点击跳转到原文
5. ✅ 顶部 "Intelligence Robot" 标题栏
6. ✅ 支持分页查询
7. ✅ 加载状态、错误处理、空状态展示

### 使用方法

#### 1. 安装依赖

```bash
npm install vue@2 element-ui axios
```

#### 2. 在 main.js 中引入 Element UI

```javascript
import Vue from 'vue';
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';

Vue.use(ElementUI);
```

#### 3. 配置 API 基础 URL

在组件中，axios 会使用以下配置：
- 环境变量 `VUE_APP_API_BASE_URL`
- 默认值：`http://localhost:8080`

或者直接在组件中修改 axios 的 baseURL。

#### 4. 在父组件中使用

```vue
<template>
  <div>
    <DailyBriefing />
  </div>
</template>

<script>
import DailyBriefing from './components/DailyBriefing.vue';

export default {
  name: 'App',
  components: {
    DailyBriefing
  }
};
</script>
```

### API 接口说明

组件调用的后端接口：

```
GET /api/news/daily-briefing?page=0&size=10
```

**请求参数：**
- `page`：页码（从 0 开始），默认 0
- `size`：每页大小，默认 10

**响应格式：**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "新闻标题",
      "summary": "AI 生成的中文总结",
      "originalUrl": "https://...",
      "source": "Bangkok Post",
      "publishDate": "2024-12-03T10:00:00",
      "createTime": "2024-12-03T08:00:00"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 组件 Props

无

### 组件 Events

无

### 样式定制

组件使用了 scoped 样式，可以通过以下方式定制：

1. 修改组件内的 `<style scoped>` 部分
2. 使用深度选择器在父组件中覆盖样式：

```vue
<style>
.daily-briefing >>> .news-card {
  /* 自定义样式 */
}
</style>
```

### 注意事项

1. **跨域问题**：如果前端和后端不在同一域名，需要配置 CORS 或使用代理
2. **API 地址**：确保后端服务运行在正确的端口（默认 8080）
3. **Element UI 版本**：确保使用兼容 Vue2 的 Element UI 版本（2.x）

### 完整示例项目结构

```
vue-frontend/
├── src/
│   ├── components/
│   │   └── DailyBriefing.vue
│   ├── api/
│   │   └── news.js
│   └── main.js
├── package.json
└── README.md
```

### 开发建议

1. 使用 Vue CLI 创建项目：
   ```bash
   vue create vue-frontend
   cd vue-frontend
   ```

2. 安装 Element UI：
   ```bash
   vue add element
   ```
   或手动安装：
   ```bash
   npm install element-ui
   ```

3. 配置代理（开发环境）：
   在 `vue.config.js` 中：
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

