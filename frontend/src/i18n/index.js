/**
 * Global i18n (Internationalization) System
 * Manages all UI text translations for the application
 */

import { getLanguagePreference, setLanguagePreference } from '../utils/language';

// All UI text translations
const translations = {
  en: {
    // Login Page
    login: {
      subtitle: 'Connecting Students & Local Life',
      username: 'USERNAME',
      email: 'EMAIL',
      password: 'PASSWORD',
      login: 'LOGIN',
      register: 'REGISTER',
      create: 'Create an account',
      hasAccount: 'Have an account? Login',
      student: 'Student',
      merchant: 'Merchant',
      businessName: 'Business Name',
      contact: 'Email / Phone',
      registerMerchant: 'REGISTER & VERIFY',
      upload: 'Upload ID / Business License',
      verification: '(Verification Required)',
      back: 'Back to Student Login',
      testAdmin: 'Test: admin/admin123',
      testUser: 'Test: lihua/password123',
      pleaseWait: 'Please wait...',
      preferredLanguage: 'Preferred Language',
    },
    // Header
    header: {
      subtitle: 'International Student Support',
      title: 'Global Buddy',
      description: 'Cross-border Support · Second-hand Trading · NLP Assistant · Semantic Search',
    },
    // Sidebar
    sidebar: {
      platform: 'PLATFORM',
      communityFeed: 'Community Feed',
      newPost: 'New Post',
      semanticSearch: 'Semantic Search',
      aiAssistant: 'AI Assistant',
      admin: 'ADMIN',
      adminPanel: 'Admin Panel',
      profile: 'Profile',
      logout: 'Logout',
    },
    // Post List
    postList: {
      searchPlaceholder: 'Semantic search, e.g. food, rent, visa',
      search: 'Search',
      loading: 'Loading...',
      anonymous: 'Anonymous',
      student: 'Student',
      semanticScore: 'Semantic Match Score',
    },
    // Post Detail
    postDetail: {
      loading: 'Loading post...',
      notFound: 'Post not found',
      back: 'Back',
      follow: 'Follow',
      following: 'Following',
      addComment: 'Add a comment',
      commentPlaceholder: 'Write your comment...',
      submitComment: 'Submit',
      submitting: 'Submitting...',
      comments: 'Comments',
      noComments: 'No comments yet. Be the first to comment!',
      commentFailed: 'Failed to add comment',
      loginRequired: 'Please login to perform this action',
      justNow: 'Just now',
      minutesAgo: 'm ago',
      hoursAgo: 'h ago',
      daysAgo: 'd ago',
      noTitle: 'No title',
      noContent: 'No content available for this post.',
    },
    // New Post Form
    newPost: {
      title: '➕ New Post',
      autoTranslation: '🌐 Auto Translation:',
      autoTranslationDesc: 'Your post will be automatically translated to Chinese and English. You can write in any language (Chinese/English/Thai) - the system will detect and translate it automatically.',
      selectTag: 'Select Tag (Mandatory)',
      community: 'Community',
      authorId: 'Author ID',
      authorIdPlaceholder: 'Available from /api/users',
      postTitle: 'Title',
      postTitlePlaceholder: 'Enter post title',
      content: 'Content',
      contentPlaceholder: 'Share survival info, sell items, or ask for help...',
      uploadImage: 'Upload Image (Optional)',
      publish: 'Publish Now',
      publishing: 'Publishing...',
      success: '✅ Post published successfully, check it in Community Feed.',
      failed: '❌ Failed to publish, please check your input.',
    },
    // Search Panel
    search: {
      title: '🔍 Semantic Linked Search',
      placeholder: 'Enter keywords, e.g. cooking, accommodation, courses',
      search: 'Search',
      searching: 'Searching...',
      postMatches: '📝 Post Matches',
      communityMatches: '🌐 Community Matches',
      noMatches: 'No matches',
      score: 'Score',
    },
    // AI Assistant
    assistant: {
      title: '🤖 Smart Q&A Assistant',
      placeholder: 'Ask a question, e.g. Where to eat cheaply in Bangkok? How to find Thai friends in Shanghai?',
      ask: 'Ask',
      generating: 'Generating...',
      referencePosts: '📚 Reference Posts:',
    },
    // Admin Panel
    admin: {
      title: '🔧 Admin Backend',
      dashboard: '📊 Dashboard',
      postReview: '📋 Post Review',
      userManagement: '👥 User Management',
      userStats: '👥 User Statistics',
      totalPosts: '📝 Total Posts',
      pendingReview: '⏳ Pending Review',
      approved: '✅ Approved',
      rejected: '❌ Rejected',
      postReviewQueue: '📋 Post Review Queue',
      noPendingPosts: '✨ No posts pending review',
      approve: '✓ Approve',
      reject: '✗ Reject',
      rejectionReason: 'Please enter rejection reason:',
      username: 'Username',
      email: 'Email',
      role: 'Role',
      status: 'Status',
      actions: 'Actions',
      enabled: '✓ Enabled',
      disabled: '✗ Disabled',
      promoteToAdmin: 'Promote to Admin',
      demoteToUser: 'Demote to User',
      disable: 'Disable',
      enable: 'Enable',
    },
    // Daily Briefing
    briefing: {
      title: 'Daily Briefing',
      source: 'Source: News API',
      totalItems: 'Total {count} items',
      loading: 'Loading...',
      error: 'Failed to fetch news, please try again later',
      noNews: 'No news today, please try again later.',
      readOriginal: 'Read Original →',
      previous: 'Previous',
      next: 'Next',
    },
    // Common
    common: {
      loading: 'Loading...',
      error: 'Error',
      success: 'Success',
      cancel: 'Cancel',
      confirm: 'Confirm',
      save: 'Save',
      delete: 'Delete',
      edit: 'Edit',
      close: 'Close',
    },
  },
  zh: {
    // Login Page
    login: {
      subtitle: '连接留学生与本地生活',
      username: '用户名',
      email: '电子邮箱',
      password: '密码',
      login: '登录',
      register: '注册',
      create: '创建新账户',
      hasAccount: '已有账户？登录',
      student: '学生',
      merchant: '商户',
      businessName: '商户名称',
      contact: '联系方式',
      registerMerchant: '注册并验证',
      upload: '上传证件/营业执照',
      verification: '（需要审核）',
      back: '返回学生登录',
      testAdmin: '测试: admin/admin123',
      testUser: '测试: lihua/password123',
      pleaseWait: '请稍候...',
      preferredLanguage: '语言偏好',
    },
    // Header
    header: {
      subtitle: '留学生互助平台',
      title: 'Global Buddy',
      description: '跨境互助 · 二手交易 · NLP智能助手 · 语义搜索',
    },
    // Sidebar
    sidebar: {
      platform: '平台',
      communityFeed: '社区动态',
      newPost: '发布帖子',
      semanticSearch: '语义搜索',
      aiAssistant: 'AI助手',
      admin: '管理',
      adminPanel: '管理面板',
      profile: '个人资料',
      logout: '退出登录',
    },
    // Post List
    postList: {
      searchPlaceholder: '语义搜索，例如：吃饭、租房、签证',
      search: '搜索',
      loading: '加载中...',
      anonymous: '匿名',
      student: '学生',
      semanticScore: '语义匹配分数',
    },
    // Post Detail
    postDetail: {
      loading: '加载帖子中...',
      notFound: '帖子未找到',
      back: '返回',
      follow: '关注',
      following: '已关注',
      addComment: '添加评论',
      commentPlaceholder: '写下你的评论...',
      submitComment: '提交',
      submitting: '提交中...',
      comments: '评论',
      noComments: '还没有评论。成为第一个评论的人！',
      commentFailed: '添加评论失败',
      loginRequired: '请先登录以执行此操作',
      justNow: '刚刚',
      minutesAgo: '分钟前',
      hoursAgo: '小时前',
      daysAgo: '天前',
      noTitle: '无标题',
      noContent: '此帖子暂无内容。',
    },
    // New Post Form
    newPost: {
      title: '➕ 发布新帖',
      autoTranslation: '🌐 自动翻译：',
      autoTranslationDesc: '您的帖子将自动翻译成中文和英文。您可以用任何语言（中文/英文/泰文）编写 - 系统会自动检测并翻译。',
      selectTag: '选择标签（必填）',
      community: '社区',
      authorId: '作者ID',
      authorIdPlaceholder: '从 /api/users 获取',
      postTitle: '标题',
      postTitlePlaceholder: '输入帖子标题',
      content: '内容',
      contentPlaceholder: '分享生活信息、出售物品或寻求帮助...',
      uploadImage: '上传图片（可选）',
      publish: '立即发布',
      publishing: '发布中...',
      success: '✅ 帖子发布成功，请在社区动态中查看。',
      failed: '❌ 发布失败，请检查您的输入。',
    },
    // Search Panel
    search: {
      title: '🔍 语义关联搜索',
      placeholder: '输入关键词，例如：做饭、住宿、课程',
      search: '搜索',
      searching: '搜索中...',
      postMatches: '📝 帖子匹配',
      communityMatches: '🌐 社区匹配',
      noMatches: '无匹配结果',
      score: '分数',
    },
    // AI Assistant
    assistant: {
      title: '🤖 智能问答助手',
      placeholder: '提问，例如：在曼谷哪里可以便宜地吃饭？如何在上海找到泰国朋友？',
      ask: '提问',
      generating: '生成中...',
      referencePosts: '📚 参考帖子：',
    },
    // Admin Panel
    admin: {
      title: '🔧 管理后台',
      dashboard: '📊 仪表板',
      postReview: '📋 帖子审核',
      userManagement: '👥 用户管理',
      userStats: '👥 用户统计',
      totalPosts: '📝 总帖子数',
      pendingReview: '⏳ 待审核',
      approved: '✅ 已通过',
      rejected: '❌ 已拒绝',
      postReviewQueue: '📋 帖子审核队列',
      noPendingPosts: '✨ 没有待审核的帖子',
      approve: '✓ 通过',
      reject: '✗ 拒绝',
      rejectionReason: '请输入拒绝原因：',
      username: '用户名',
      email: '邮箱',
      role: '角色',
      status: '状态',
      actions: '操作',
      enabled: '✓ 已启用',
      disabled: '✗ 已禁用',
      promoteToAdmin: '提升为管理员',
      demoteToUser: '降级为普通用户',
      disable: '禁用',
      enable: '启用',
    },
    // Daily Briefing
    briefing: {
      title: '每日简报',
      source: '来源：新闻API',
      totalItems: '共 {count} 条',
      loading: '加载中...',
      error: '获取新闻失败，请稍后重试',
      noNews: '今天没有新闻，请稍后重试。',
      readOriginal: '阅读原文 →',
      previous: '上一页',
      next: '下一页',
    },
    // Common
    common: {
      loading: '加载中...',
      error: '错误',
      success: '成功',
      cancel: '取消',
      confirm: '确认',
      save: '保存',
      delete: '删除',
      edit: '编辑',
      close: '关闭',
    },
  },
};

// Current language state
let currentLang = getLanguagePreference();

// Get translation function
export const t = (key, params = {}) => {
  const keys = key.split('.');
  let value = translations[currentLang];
  
  for (const k of keys) {
    if (value && typeof value === 'object') {
      value = value[k];
    } else {
      console.warn(`Translation key not found: ${key}`);
      return key;
    }
  }
  
  if (typeof value === 'string') {
    // Replace placeholders like {count}
    return value.replace(/\{(\w+)\}/g, (match, paramKey) => {
      return params[paramKey] !== undefined ? params[paramKey] : match;
    });
  }
  
  return value || key;
};

// Get current language
export const getCurrentLanguage = () => currentLang;

// Set language
export const setLanguage = (lang) => {
  if (lang === 'en' || lang === 'zh') {
    currentLang = lang;
    setLanguagePreference(lang);
    // Trigger language change event for components
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('languageChanged', { detail: { lang } }));
    }
  }
};

// Initialize language from localStorage
export const initLanguage = () => {
  const savedLang = getLanguagePreference();
  console.log('i18n: initLanguage, savedLang from localStorage:', savedLang);
  currentLang = savedLang;
  console.log('i18n: initLanguage, currentLang set to:', currentLang);
};

// Hook for React components (requires React import in component)
export const useTranslation = () => {
  // This will be implemented in components that use React hooks
  return { t, lang: currentLang, setLanguage };
};

// Initialize on module load
if (typeof window !== 'undefined') {
  initLanguage();
}

