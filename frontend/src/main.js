import { createApp } from 'vue';
import App from './App.vue';
import './styles.css';

// Element Plus
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import en from 'element-plus/dist/locale/en.mjs';

const app = createApp(App);

// 注册 Element Plus（支持中英文）
app.use(ElementPlus, {
  locale: zhCn, // 默认中文，可以根据需要动态切换
});

app.mount('#root');

