<template>
  <el-config-provider :locale="elementLocale">
    <div class="browser-window">
      <div class="app-container">
      <LoginPage v-if="!isLoggedIn" @login="handleLogin" />
      <template v-else>
        <Sidebar 
          :current-page="currentPage" 
          @navigate="handleNavigate"
          :is-admin="isAdmin"
          :user="user"
        />
        <main class="main-content">
          <!-- Daily Briefing Page -->
          <template v-if="currentPage === 'briefing'">
            <DailyBriefing 
              v-if="!selectedNewsId"
              :key="`briefing-${lang.value}`"
              @view-detail="handleViewNewsDetail" />
            <DailyBriefingDetail
              v-else
              :news-id="selectedNewsId"
              :key="`briefing-detail-${selectedNewsId}-${lang.value}`"
              @back="handleBackToBriefingList" />
          </template>

          <!-- Community Page -->
          <template v-else-if="currentPage === 'community'">
            <div class="section-title" style="display: flex; justify-content: space-between; align-items: center">
              <span>{{ lang === 'zh' ? '社区动态' : 'Community Feed' }}</span>
              <div style="display: flex; gap: 0.5rem">
                <span
                  :class="['pill', { active: selectedTag === 'all' }]"
                  @click="selectedTag = 'all'"
                  style="cursor: pointer"
                >
                  {{ lang === 'zh' ? '全部' : 'All' }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'rent' }]"
                  @click="selectedTag = 'rent'"
                  style="cursor: pointer"
                >
                  🏠 #{{ lang === 'zh' ? '租房' : 'Rent' }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'learning' }]"
                  @click="selectedTag = 'learning'"
                  style="cursor: pointer"
                >
                  📚 #{{ lang === 'zh' ? '学习' : 'Learning' }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'market' }]"
                  @click="selectedTag = 'market'"
                  style="cursor: pointer"
                >
                  🛒 #{{ lang === 'zh' ? '市场' : 'Market' }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'visa' }]"
                  @click="selectedTag = 'visa'"
                  style="cursor: pointer"
                >
                  🛂 #{{ lang === 'zh' ? '签证' : 'Visa' }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'food' }]"
                  @click="selectedTag = 'food'"
                  style="cursor: pointer"
                >
                  🍜 #{{ lang === 'zh' ? '美食' : 'Food' }}
                </span>
              </div>
            </div>
            <PostList
              :key="`posts-${lang}`"
              @post-click="(postId) => handleNavigate('postDetail', postId)"
              :selected-tag="selectedTag"
            />
          </template>

          <!-- Home View (Legacy - redirects to briefing) -->
          <template v-else-if="currentPage === 'home'">
            <DailyBriefing :key="`briefing-${lang}`" />
          </template>

          <!-- New Post -->
          <NewPostForm v-else-if="currentPage === 'post'" :key="`newpost-${lang}`" :current-user-id="user?.id" />

          <!-- Search -->
          <SearchPanel v-else-if="currentPage === 'search'" :key="`search-${lang}`" />

          <!-- AI Assistant -->
          <NlpAssistant v-else-if="currentPage === 'assistant'" :key="`assistant-${lang}`" />

          <!-- Admin Panel -->
          <template v-else-if="currentPage === 'admin'">
            <AdminPanel v-if="isAdmin" :key="`admin-${lang}`" :token="token" />
            <div v-else class="card">{{ lang === 'zh' ? '无权访问' : 'Access Denied' }}</div>
          </template>

          <!-- Post Detail -->
          <template v-else-if="currentPage === 'postDetail'">
            <PostDetail
              v-if="selectedPostId"
              :key="`detail-${lang}-${selectedPostId}`"
              :post-id="selectedPostId"
              :token="token"
              :current-user-id="user?.id"
              @back="handleNavigate('home')"
            />
            <div v-else class="card">{{ lang === 'zh' ? '帖子未找到' : 'Post not found' }}</div>
          </template>

          <!-- Messages Page -->
          <template v-else-if="currentPage === 'messages'">
            <div style="display: flex; height: calc(100vh - 2rem); gap: 1rem;">
              <!-- Conversation List -->
              <div style="flex: 0 0 350px; border: 2px solid #ddd; border-radius: 8px; overflow: hidden;">
                <ConversationList
                  :token="token"
                  :selected-conversation-id="selectedConversationId"
                  @select-conversation="handleSelectConversation"
                  @back="handleBackToCommunity"
                />
              </div>
              <!-- Chat Window -->
              <div style="flex: 1; border: 2px solid #ddd; border-radius: 8px; overflow: hidden;">
                <ChatWindow
                  v-if="selectedConversationId && selectedConversation"
                  :conversation-id="selectedConversationId"
                  :token="token"
                  :current-user-id="user?.id"
                  :other-user="selectedConversation.otherUser"
                  @message-sent="handleMessageSent"
                />
                <div v-else style="display: flex; align-items: center; justify-content: center; height: 100%; color: #666;">
                  <p>{{ lang === 'zh' ? '请选择一个会话开始聊天' : 'Select a conversation to start chatting' }}</p>
                </div>
              </div>
            </div>
          </template>

          <!-- Profile -->
          <template v-else-if="currentPage === 'profile'">
            <MyProfile 
              v-if="!showMyPostsList"
              :user-id="user?.id"
              :token="token"
              @view-my-posts="showMyPostsList = true"
              @view-post-detail="(postId) => handleNavigate('postDetail', postId)"
            />
            <MyPosts
              v-else
              :token="token"
              @back="showMyPostsList = false"
              @view-post-detail="(postId) => handleNavigate('postDetail', postId)"
            />
          </template>

          <!-- Default -->
          <PostList v-else />
        </main>
      </template>
    </div>
  </div>
  </el-config-provider>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import LoginPage from './components/LoginPage.vue';
import Sidebar from './components/Sidebar.vue';
import PostList from './components/PostList.vue';
import PostDetail from './components/PostDetail.vue';
import SearchPanel from './components/SearchPanel.vue';
import DailyBriefing from './components/vue/DailyBriefing.vue';
import DailyBriefingDetail from './components/vue/DailyBriefingDetail.vue';
import UserProfile from './components/UserProfile.vue';
import MyProfile from './components/MyProfile.vue';
import MyPosts from './components/MyPosts.vue';
import NlpAssistant from './components/NlpAssistant.vue';
import NewPostForm from './components/NewPostForm.vue';
import AdminPanel from './components/AdminPanel.vue';
import ConversationList from './components/ConversationList.vue';
import ChatWindow from './components/ChatWindow.vue';
import { fetchMyRejectedPosts } from './api';
import { setLanguagePreference, getLanguagePreference } from './utils/language';
import { setLanguage, getCurrentLanguage } from './i18n';
// Element Plus locale for dynamic language switching
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import en from 'element-plus/dist/locale/en.mjs';
import { ElConfigProvider } from 'element-plus';

// Get initial auth state from localStorage
const getInitialAuthState = () => {
  try {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (savedToken && savedUser) {
      return {
        isLoggedIn: true,
        token: savedToken,
        user: JSON.parse(savedUser)
      };
    }
  } catch (e) {
    console.error('Failed to restore auth state:', e);
  }
  return { isLoggedIn: false, token: null, user: null };
};

const initialAuth = getInitialAuthState();
const isLoggedIn = ref(initialAuth.isLoggedIn);
const currentPage = ref('briefing'); // Default to briefing page
const selectedPostId = ref(null);
const selectedNewsId = ref(null); // For news detail page
const selectedUserId = ref(null); // For user profile page
const selectedConversationId = ref(null); // For messages page
const selectedConversation = ref(null); // For messages page - stores conversation data
const user = ref(initialAuth.user);
const token = ref(initialAuth.token);
const lang = ref(getCurrentLanguage());
const selectedTag = ref('all');
const rejectedPosts = ref([]);
const showMyPostsList = ref(false);

const isAdmin = computed(() => user.value?.role === 'ADMIN');

// Element Plus locale - 响应语言变化
const elementLocale = computed(() => {
  return lang.value === 'zh' ? zhCn : en;
});

// Listen for language changes and auth errors
onMounted(() => {
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      console.log('App: Language changed to:', e.detail.lang);
      lang.value = e.detail.lang;
      // Element Plus locale 会通过 computed 自动更新
    }
  };
  
  // Listen for unauthorized errors from API interceptor
  const handleUnauthorized = (e) => {
    console.warn('App: Received auth:unauthorized event, logging out user');
    if (isLoggedIn.value) {
      handleLogout();
    }
  };
  
  window.addEventListener('languageChanged', handleLanguageChange);
  window.addEventListener('auth:unauthorized', handleUnauthorized);
  
  // Session management
  sessionStorage.setItem('sessionActive', 'true');
  const wasSessionActive = sessionStorage.getItem('sessionActive');
  if (!wasSessionActive && initialAuth.isLoggedIn) {
    console.log('App: New session detected, clearing auth state');
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    isLoggedIn.value = false;
    user.value = null;
    token.value = null;
  }
  
  // Restore language preference from user
  if (isLoggedIn.value && user.value?.preferredLanguage) {
    const userLang = user.value.preferredLanguage;
    if (userLang === 'zh' || userLang === 'en') {
      setLanguagePreference(userLang);
      setLanguage(userLang);
    }
  }
  
  // Load rejected posts
  if (token.value) {
    loadRejectedPosts();
  }
  
  return () => {
    window.removeEventListener('languageChanged', handleLanguageChange);
    window.removeEventListener('auth:unauthorized', handleUnauthorized);
    window.removeEventListener('viewUserProfile', handleViewUserProfile);
  };
});

// Watch for token changes to load rejected posts
watch(token, (newToken) => {
  if (newToken) {
    loadRejectedPosts();
  }
});

const loadRejectedPosts = async () => {
  try {
    if (!token.value) return;
    const data = await fetchMyRejectedPosts();
    rejectedPosts.value = Array.isArray(data) ? data : [];
  } catch (e) {
    console.error('Failed to load rejected posts', e);
    
    // 如果是认证错误，清除 token 并登出用户
    if (e.response?.status === 401 || e.isAuthError) {
      console.warn('🔒 Authentication failed (401), logging out user');
      handleLogout();
      // 不需要显示错误消息，因为用户会被重定向到登录页面
    }
  }
};

const handleLogin = (userData, authToken) => {
  console.log('App: handleLogin called, userData:', userData);
  isLoggedIn.value = true;
  user.value = userData;
  token.value = authToken;
  
  if (authToken) {
    localStorage.setItem('token', authToken);
    console.log('App: Token saved to localStorage');
  }
  if (userData) {
    localStorage.setItem('user', JSON.stringify(userData));
  }
  currentPage.value = 'briefing'; // Default to briefing page
  
  if (userData && userData.preferredLanguage) {
    const userLang = userData.preferredLanguage;
    console.log('App: User preferredLanguage:', userLang);
    if (userLang === 'zh' || userLang === 'en') {
      console.log('App: Setting language to:', userLang);
      setLanguagePreference(userLang);
      setLanguage(userLang);
    }
  } else {
    const savedLang = localStorage.getItem('userLanguage');
    if (savedLang && (savedLang === 'zh' || savedLang === 'en')) {
      console.log('App: Using saved language from localStorage:', savedLang);
      setLanguage(savedLang);
    }
  }
};

const handleLogout = () => {
  console.log('App: Logging out user');
  isLoggedIn.value = false;
  user.value = null;
  token.value = null;
  rejectedPosts.value = [];
  showMyPostsList.value = false;
  currentPage.value = 'briefing'; // Reset to default page
  selectedPostId.value = null;
  selectedNewsId.value = null;
  selectedUserId.value = null;
  selectedConversationId.value = null;
  selectedConversation.value = null;
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  sessionStorage.removeItem('sessionActive');
};

const handleNavigate = (page, postId = null) => {
  if (page === 'logout') {
    handleLogout();
    return;
  }
  if (page === 'postDetail' && postId) {
    selectedPostId.value = postId;
    currentPage.value = 'postDetail';
    selectedNewsId.value = null;
    selectedUserId.value = null;
    selectedConversationId.value = null;
    selectedConversation.value = null;
    showMyPostsList.value = false;
  } else if (page === 'messages') {
    currentPage.value = 'messages';
    selectedPostId.value = null;
    selectedNewsId.value = null;
    selectedUserId.value = null;
    showMyPostsList.value = false;
    // Keep conversation selection when navigating to messages
  } else if (page === 'profile') {
    currentPage.value = 'profile';
    showMyPostsList.value = false;
    selectedPostId.value = null;
    selectedNewsId.value = null;
    selectedUserId.value = null;
    selectedConversationId.value = null;
    selectedConversation.value = null;
  } else {
    currentPage.value = page;
    selectedPostId.value = null;
    selectedNewsId.value = null; // Reset news detail when navigating away
    selectedUserId.value = null; // Reset user profile when navigating away
    selectedConversationId.value = null; // Reset conversation when navigating away
    selectedConversation.value = null;
    showMyPostsList.value = false;
  }
};

const handleViewNewsDetail = (newsId) => {
  selectedNewsId.value = newsId;
};

const handleBackToBriefingList = () => {
  selectedNewsId.value = null;
};

const handleBackFromProfile = () => {
  selectedUserId.value = null;
  currentPage.value = 'community'; // Go back to community page
};

const handlePostClickFromProfile = (postId) => {
  selectedPostId.value = postId;
  currentPage.value = 'postDetail';
};

const handleSendMessageFromProfile = (userId) => {
  // Navigate to messages page and create/select conversation
  handleNavigate('messages');
  // The conversation will be created when user selects it from the list
  // or we can auto-create it here if needed
};

const handleSelectConversation = (conversation) => {
  if (conversation) {
    selectedConversationId.value = conversation.id;
    selectedConversation.value = conversation;
  } else {
    selectedConversationId.value = null;
    selectedConversation.value = null;
  }
};

const handleMessageSent = () => {
  // Refresh conversation list when a message is sent
  // This will be handled by the ConversationList component if we expose a refresh method
  console.log('Message sent, conversation list should refresh');
};

const handleBackToCommunity = () => {
  currentPage.value = 'community';
  selectedConversationId.value = null;
  selectedConversation.value = null;
};
</script>

<style scoped>
/* Styles are in styles.css */
</style>
