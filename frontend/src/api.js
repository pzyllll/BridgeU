import axios from 'axios';
import { getLanguagePreference } from './utils/language';

// 如果设置了 VITE_API_BASE 环境变量，使用它；否则使用空字符串（通过 Vite 代理）
const baseURL = import.meta.env.VITE_API_BASE || '';
console.log('API baseURL:', baseURL || '(使用 Vite 代理)');

const client = axios.create({
  baseURL: baseURL,
  timeout: 10000,
});

// 添加请求拦截器用于调试
client.interceptors.request.use(
  (config) => {
    // 自动附加本地 token（若未显式传入）
    if (!config.headers?.Authorization) {
      const token = localStorage.getItem('token');
      if (token) {
        config.headers = { ...(config.headers || {}), Authorization: `Bearer ${token}` };
      }
    }
    const fullUrl = config.baseURL ? `${config.baseURL}${config.url}` : config.url;
    console.log('🌐 API Request:', config.method?.toUpperCase(), fullUrl, {
      params: config.params,
      baseURL: config.baseURL || '(relative)',
      url: config.url,
      hasToken: !!config.headers?.Authorization,
    });
    return config;
  },
  (error) => {
    console.error('❌ API Request Error:', error);
    return Promise.reject(error);
  }
);

// 添加响应拦截器用于错误处理
client.interceptors.response.use(
  (response) => {
    console.log('✅ API Response:', response.config.method?.toUpperCase(), response.config.url, response.status);
    return response;
  },
  (error) => {
    // 过滤掉浏览器扩展相关的错误
    const isExtensionError = error.message && (
      error.message.includes('content-all.js') ||
      error.message.includes('chrome-extension') ||
      error.message.includes('moz-extension') ||
      error.stack?.includes('content-all.js')
    );
    
    if (isExtensionError) {
      console.warn('⚠️ 检测到浏览器扩展相关错误，已忽略:', error.message);
      // 不阻止请求，让原始错误继续传播，但标记为扩展错误
      error.isExtensionError = true;
      return Promise.reject(error);
    }
    
    const errorDetails = {
      message: error.message,
      code: error.code,
      url: error.config?.url,
      baseURL: error.config?.baseURL,
      fullUrl: error.config?.baseURL ? `${error.config.baseURL}${error.config.url}` : error.config?.url,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      request: error.request
    };
    
    console.error('❌ API Response Error:', errorDetails);
    
    // 如果是网络错误，提供更详细的提示
    if (error.code === 'ERR_NETWORK' || error.message === 'Network Error' || error.message.includes('Failed to fetch')) {
      console.error('🔴 Network Error Details:', {
        '后端服务器': 'http://localhost:8080',
        'Vite 代理': '/api -> http://localhost:8080',
        '建议检查': [
          '1. 后端服务器是否运行？',
          '2. Vite 开发服务器是否运行？',
          '3. 浏览器 Network 标签页查看实际请求',
          '4. 检查 CORS 配置'
        ]
      });
    }
    
    return Promise.reject(error);
  }
);

// API functions with automatic language parameter
export const fetchPosts = (params = {}) => {
  // Use the lang from params if provided, otherwise get from localStorage
  const lang = params.lang !== undefined ? params.lang : getLanguagePreference();
  console.log('fetchPosts called with params:', params, 'final lang:', lang);
  return client.get('/api/posts', { params: { ...params, lang } }).then((res) => res.data);
};
export const searchAll = (params = {}) => client.get('/api/search', { params }).then((res) => res.data);
export const askQuestion = (payload) => client.post('/api/nlp/qa', payload).then((res) => res.data);
export const createPost = (payload, token) =>
  client
    .post('/api/posts', payload, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    .then((res) => res.data);

export const uploadPostImage = (file, token) => {
  const formData = new FormData();
  formData.append('file', file);
  return client
    .post('/api/posts/upload-image', formData, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    .then((res) => res.data);
};
export const fetchDailyBriefing = (params = {}) => {
  // Use the lang from params if provided, otherwise get from localStorage
  const lang = params.lang !== undefined ? params.lang : getLanguagePreference();
  console.log('fetchDailyBriefing called with params:', params, 'final lang:', lang);
  return client.get('/api/news/daily-briefing', { params: { ...params, lang } }).then((res) => res.data);
};

// Post Detail API
export const fetchPostDetail = (postId, lang) => {
  const langToUse = lang || getLanguagePreference();
  console.log('🌐 fetchPostDetail: postId=', postId, 'lang=', lang, 'langToUse=', langToUse, 'getLanguagePreference()=', getLanguagePreference());
  return client.get(`/api/posts/${postId}`, { params: { lang: langToUse } }).then((res) => {
    console.log('✅ fetchPostDetail response: title=', res.data?.post?.title?.substring(0, 50), 'lang=', langToUse);
    return res.data;
  });
};

// Comment API
export const addComment = (postId, content, lang, token) => {
  const langToUse = lang || getLanguagePreference();
  return client.post(
    `/api/posts/${postId}/comments`,
    { content },
    {
      params: { lang: langToUse },
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Like API
export const toggleLike = (postId, token) => {
  return client.post(
    `/api/posts/${postId}/like`,
    {},
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Follow API
export const toggleFollow = (userId, token) => {
  return client.post(
    `/api/posts/users/${userId}/follow`,
    {},
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Get post author name (helper function)
export const getPostAuthorName = async (authorId) => {
  try {
    const response = await client.get(`/api/users/${authorId}`);
    return response.data?.displayName || response.data?.username || 'Unknown';
  } catch (error) {
    console.error('Failed to get author name:', error);
    return 'Unknown';
  }
};

