import axios from 'axios';
import { getLanguagePreference } from './utils/language';

// 如果设置了 VITE_API_BASE 环境变量，使用它；否则使用空字符串（通过 Vite 代理）
const baseURL = import.meta.env.VITE_API_BASE || '';
console.log('API baseURL:', baseURL || '(使用 Vite 代理)');

const client = axios.create({
  baseURL: baseURL,
  timeout: 30000, // 增加到 30 秒，给后端更多响应时间
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
    
    // Skip logging for expected errors (e.g., "Already following" in toggleFollow)
    if (error.isExpectedError) {
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
    
    // 如果是超时错误，提供更友好的错误信息
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      console.error('⏱️ Timeout Error Details:', {
        '请求URL': error.config?.url,
        '超时时间': error.config?.timeout + 'ms',
        '建议': [
          '1. 检查后端服务器是否运行正常',
          '2. 检查后端服务器响应时间',
          '3. 检查网络连接',
          '4. 如果后端正在处理大量数据，可能需要优化查询'
        ]
      });
      
      // 为超时错误添加更友好的消息
      error.userMessage = error.config?.url?.includes('/api/posts') 
        ? '加载帖子列表超时，请检查网络连接或稍后重试'
        : '请求超时，请稍后重试';
    }
    
    // 处理 401 未授权错误
    if (error.response?.status === 401) {
      console.warn('🔒 401 Unauthorized - Token may be invalid or expired');
      
      // 清除无效的 token
      const token = localStorage.getItem('token');
      if (token) {
        console.log('🗑️ Clearing invalid token from localStorage');
        localStorage.removeItem('token');
        
        // 触发自定义事件，通知应用需要重新登录
        window.dispatchEvent(new CustomEvent('auth:unauthorized', {
          detail: { url: error.config?.url }
        }));
      }
      
      // 为 401 错误添加用户友好的消息
      error.userMessage = error.config?.url?.includes('/api/posts/my/rejected')
        ? '需要登录才能查看被拒绝的帖子。请先登录。'
        : '登录已过期，请重新登录';
      error.isAuthError = true;
    }
    
    return Promise.reject(error);
  }
);

// API functions with automatic language parameter
export const fetchPosts = (params = {}) => {
  // Use the lang from params if provided, otherwise get from localStorage
  const lang = params.lang !== undefined ? params.lang : getLanguagePreference();
  console.log('fetchPosts called with params:', params, 'final lang:', lang);
  // 为帖子列表请求设置更长的超时时间（60秒），因为可能需要加载大量数据
  return client.get('/api/posts', { 
    params: { ...params, lang },
    timeout: 60000 // 60秒超时
  }).then((res) => res.data);
};
export const searchAll = (params = {}) => client.get('/api/search', { params }).then((res) => res.data);
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

// Get available news sources for filtering
export const fetchNewsSources = () => {
  return client.get('/api/news/sources').then((res) => res.data);
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

// 当前用户被拒绝的帖子列表
export const fetchMyRejectedPosts = () =>
  client.get('/api/posts/my/rejected').then((res) => res.data);

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

// Get comment summary API
export const getCommentSummary = (postId, lang) => {
  const langToUse = lang || getLanguagePreference();
  return client.get(`/api/posts/${postId}/comments/summary`, {
    params: { lang: langToUse }
  }).then((res) => res.data);
};

// Delete comment API
export const deleteComment = (postId, commentId, token) => {
  return client.delete(
    `/api/posts/${postId}/comments/${commentId}`,
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Submit report API
export const submitReport = (targetType, targetId, reasons, description, token) => {
  return client.post(
    '/api/reports',
    {
      targetType,
      targetId,
      reasons,
      description
    },
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Get my reports API
export const getMyReports = (token) => {
  return client.get(
    '/api/reports/my',
    {
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

// Search users API
export const searchUsers = (query, limit = 20, token) => {
  return client.get(
    `/api/users/search`,
    {
      params: { q: query, limit },
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Follow user API
export const followUser = (userId, token) => {
  return client.post(
    `/api/users/${userId}/follow`,
    {},
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Unfollow user API
export const unfollowUser = (userId, token) => {
  return client.delete(
    `/api/users/${userId}/follow`,
    {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    }
  ).then((res) => res.data);
};

// Follow API (legacy, kept for compatibility)
// This function toggles follow status. If isCurrentlyFollowing is provided, it uses that to determine action.
// Otherwise, it tries to follow first, and if already following, it unfollows.
export const toggleFollow = async (userId, token, isCurrentlyFollowing = null) => {
  // If we know the current state, use it directly
  if (isCurrentlyFollowing !== null) {
    if (isCurrentlyFollowing) {
      // Unfollow
      const unfollowResponse = await client.delete(
        `/api/users/${userId}/follow`,
        {
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        }
      );
      return unfollowResponse.data;
    } else {
      // Follow
      const followResponse = await client.post(
        `/api/users/${userId}/follow`,
        {},
        {
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        }
      );
      return followResponse.data;
    }
  }
  
  // Otherwise, try to follow first, and if already following, unfollow
  try {
    // Try to follow first
    const followResponse = await client.post(
      `/api/users/${userId}/follow`,
      {},
      {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      }
    );
    
    if (followResponse.data.success) {
      return { following: true, success: true };
    }
    
    // If follow failed, check if it's because already following
    if (followResponse.data.message && followResponse.data.message.includes('Already following')) {
      // Try to unfollow
      const unfollowResponse = await client.delete(
        `/api/users/${userId}/follow`,
        {
          headers: token ? { Authorization: `Bearer ${token}` } : {}
        }
      );
      
      if (unfollowResponse.data.success) {
        return { following: false, success: true };
      }
    }
    
    return followResponse.data;
  } catch (err) {
    // If follow returns 400 with "Already following", try to unfollow
    // Mark this as an expected error to avoid logging
    if (err.response?.status === 400 && 
        (err.response?.data?.message?.includes('Already following') || 
         err.response?.data?.message?.includes('already following'))) {
      // Mark as expected error to suppress logging
      err.isExpectedError = true;
      
      try {
        const unfollowResponse = await client.delete(
          `/api/users/${userId}/follow`,
          {
            headers: token ? { Authorization: `Bearer ${token}` } : {}
          }
        );
        
        if (unfollowResponse.data.success) {
          return { following: false, success: true };
        }
      } catch (unfollowErr) {
        throw unfollowErr;
      }
    }
    throw err;
  }
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

// Auth API functions
// Send verification code (email) for registration
// （手机短信验证码改由 Firebase 负责，这里只给邮箱用）
export const sendVerificationCode = (identifier, type) => {
  // type: 'email' or 'phone'
  // 发送邮件可能需要更长时间，设置 60 秒超时
  return client.post('/api/auth/send-verification-code', {
    identifier,
    type
  }, {
    timeout: 60000 // 60 秒超时
  }).then((res) => res.data);
};

// Verify verification code（邮箱或电话，取决于后端实现）
export const verifyCode = (identifier, code, type, purpose = 'REGISTER') => {
  return client.post('/api/auth/verify-code', {
    identifier,
    code,
    type,
    purpose
  }).then((res) => res.data);
};

// Login
export const login = (username, password) => {
  return client.post('/api/auth/login', {
    username,
    password,
  }).then((res) => res.data);
};

// Register with verification（主要用于邮箱注册）
export const registerWithVerification = (payload) => {
  return client.post('/api/auth/register', payload).then((res) => res.data);
};

// Register with phone (Firebase 已验证手机号后调用)
export const registerWithPhone = (payload) => {
  return client.post('/api/auth/register/phone', payload).then((res) => res.data);
};

// Forgot password - send code（目前仅支持邮箱）
export const sendPasswordResetCode = (identifier, type) => {
  // 发送邮件可能需要更长时间，设置 60 秒超时
  return client.post('/api/auth/forgot-password/send-code', {
    identifier,
    type
  }, {
    timeout: 60000 // 60 秒超时
  }).then((res) => res.data);
};

// Reset password with verification code (for email)
export const resetPassword = (identifier, code, newPassword, type) => {
  return client.post('/api/auth/forgot-password/reset', {
    identifier,
    code,
    newPassword,
    type
  }).then((res) => res.data);
};

// Reset password with phone (Firebase verified)
export const resetPasswordWithPhone = (phone, newPassword) => {
  return client.post('/api/auth/forgot-password/reset/phone', {
    phone,
    newPassword
  }).then((res) => res.data);
};

// ========== User Follow APIs ==========

// Get followers list for a user
export const getFollowers = (userId, token) => {
  return client.get(`/api/users/${userId}/followers`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data);
};

// Get mutual follows list for a user
export const getUserMutualFollows = (userId, token) => {
  return client.get(`/api/users/${userId}/mutual-follows`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data);
};

// ========== Private Messaging APIs ==========

// Get all conversations for current user
export const getConversations = (token) => {
  return client.get('/api/messages/conversations', {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data);
};

// Create or get conversation with a user
export const createOrGetConversation = (userId, token) => {
  return client.post('/api/messages/conversations', {
    userId
  }, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data)
    .catch((err) => {
      // Re-throw the error so it can be handled by the caller
      throw err;
    });
};

// Get messages in a conversation
export const getConversationMessages = (conversationId, token) => {
  return client.get(`/api/messages/conversations/${conversationId}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => {
    // Debug: Log the full response to see what backend is actually returning
    console.log('🔍 getConversationMessages - Full response:', {
      status: res.status,
      statusText: res.statusText,
      data: res.data,
      dataKeys: res.data ? Object.keys(res.data) : [],
      hasIsMutualFollow: res.data && 'isMutualFollow' in res.data,
      hasCanSendMore: res.data && 'canSendMore' in res.data,
      isMutualFollowValue: res.data?.isMutualFollow,
      canSendMoreValue: res.data?.canSendMore
    });
    return res.data;
  });
};

// Send a message in a conversation
export const sendMessage = (conversationId, content, token) => {
  return client.post(`/api/messages/conversations/${conversationId}/messages`, {
    content
  }, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data);
};

// Mark a message as read
export const markMessageAsRead = (messageId, token) => {
  return client.put(`/api/messages/${messageId}/read`, {}, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data);
};

// Mark all messages in a conversation as read
export const markConversationAsRead = (conversationId, token) => {
  return client.put(`/api/messages/conversations/${conversationId}/read`, {}, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data);
};

// Delete a conversation (soft delete)
export const deleteConversation = (conversationId, token) => {
  return client.delete(`/api/messages/conversations/${conversationId}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then((res) => res.data);
};

