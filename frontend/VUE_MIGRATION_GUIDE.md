# React to Vue Migration Guide

## 已完成的转换

1. ✅ `package.json` - 已更新为 Vue 依赖
2. ✅ `vite.config.js` - 已配置 Vue 插件
3. ✅ `main.js` - Vue 入口文件
4. ✅ `App.vue` - 主应用组件
5. ✅ `LoginPage.vue` - 登录页面
6. ✅ `Sidebar.vue` - 侧边栏
7. ✅ `DailyBriefing.vue` - 已存在（在 vue 文件夹中）

## 需要转换的组件

### 转换模式

#### React 组件结构：
```jsx
import { useState, useEffect } from 'react';

const Component = ({ prop1, prop2 }) => {
  const [state, setState] = useState(initial);
  
  useEffect(() => {
    // side effects
  }, [deps]);
  
  return <div>...</div>;
};
```

#### Vue 组件结构：
```vue
<template>
  <div>...</div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';

const props = defineProps({
  prop1: String,
  prop2: Number
});

const emit = defineEmits(['event']);

const state = ref(initial);

onMounted(() => {
  // side effects
});

watch(() => props.prop1, (newVal) => {
  // watch changes
});
</script>
```

### 关键转换点

1. **useState** → `ref()` 或 `reactive()`
2. **useEffect** → `onMounted()`, `watch()`, `onUpdated()`
3. **props** → `defineProps()`
4. **事件** → `defineEmits()` 和 `$emit()`
5. **className** → `class` 或 `:class`
6. **onClick** → `@click`
7. **onChange** → `@input` 或 `v-model`
8. **条件渲染** → `v-if`, `v-else-if`, `v-else`
9. **列表渲染** → `v-for`
10. **JSX** → `<template>` 标签

### 需要转换的组件列表

1. `PostList.jsx` → `PostList.vue`
2. `PostDetail.jsx` → `PostDetail.vue`
3. `NewPostForm.jsx` → `NewPostForm.vue`
4. `SearchPanel.jsx` → `SearchPanel.vue`
5. `NlpAssistant.jsx` → `NlpAssistant.vue`
6. `AdminPanel.jsx` → `AdminPanel.vue`

## 安装依赖

运行以下命令安装 Vue 依赖：

```bash
cd frontend
npm install
```

## 测试

启动开发服务器：

```bash
npm run dev
```

确保所有功能正常工作，没有控制台错误。

