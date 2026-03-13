<template>
  <el-config-provider :locale="elementLocale">
    <div class="browser-window">
      <div class="app-container">
      <LoginPage v-if="!isLoggedIn" @login="handleLogin" />
      <template v-else>
        <Sidebar 
          :current-page="currentPage" 
          @navigate="handleNavigate"
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
            <div class="section-title" style="display: flex; justify-content: space-between; align-items: center; padding: 0 32px 24px 32px">
              <span>{{ t('community.title') }}</span>
              <div style="display: flex; gap: 0.5rem; flex-wrap: wrap">
                <span
                  :class="['pill', { active: selectedTag === 'all' }]"
                  @click="selectedTag = 'all'"
                  style="cursor: pointer"
                >
                  {{ t('community.filterAll') }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'study' }]"
                  data-tag="study"
                  @click="selectedTag = 'study'"
                  style="cursor: pointer"
                >
                  📚 #{{ t('community.tagStudy') }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'housing' }]"
                  data-tag="housing"
                  @click="selectedTag = 'housing'"
                  style="cursor: pointer"
                >
                  🏠 #{{ t('community.tagHousing') }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'travel' }]"
                  data-tag="travel"
                  @click="selectedTag = 'travel'"
                  style="cursor: pointer"
                >
                  ✈️ #{{ t('community.tagTravel') }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'part-time-job' }]"
                  data-tag="part-time-job"
                  @click="selectedTag = 'part-time-job'"
                  style="cursor: pointer"
                >
                  💼 #{{ t('community.tagPartTimeJob') }}
                </span>
                <span
                  :class="['pill', { active: selectedTag === 'life-services' }]"
                  data-tag="life-services"
                  @click="selectedTag = 'life-services'"
                  style="cursor: pointer"
                >
                  🛍️ #{{ t('community.tagLifeServices') }}
                </span>
              </div>
            </div>
            <PostList
              :key="`posts-${lang}`"
              @post-click="(postId) => handleNavigate('postDetail', postId)"
              @author-click="handleViewUserProfile"
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

          <!-- Post Detail -->
          <template v-else-if="currentPage === 'postDetail'">
            <PostDetail
              v-if="selectedPostId"
              :key="`detail-${lang}-${selectedPostId}`"
              :post-id="selectedPostId"
              :token="token"
              :current-user-id="user?.id"
              @back="handleBack"
              @author-click="handleViewUserProfile"
            />
            <div v-else class="card">{{ lang === 'zh' ? '帖子未找到' : 'Post not found' }}</div>
          </template>

          <!-- Messages Page -->
          <template v-else-if="currentPage === 'messages'">
            <div v-if="selectedUserId" style="height: calc(100vh - 2rem); overflow-y: auto;">
              <UserProfile
                :user-id="selectedUserId"
                :token="token"
                :current-user-id="user?.id"
                @back="handleBack"
                @post-click="handlePostClickFromProfile"
                @send-message="handleSendMessageFromProfile"
                :on-view-user-profile="handleViewUserProfile"
              />
            </div>
            <div v-else style="display: flex; height: calc(100vh - 2rem); gap: 1rem;">
              <!-- Conversation List -->
              <div style="flex: 0 0 350px; border: 2px solid #ddd; border-radius: 8px; overflow: hidden;">
                <ConversationList
                  :token="token"
                  :selected-conversation-id="selectedConversationId"
                  @select-conversation="handleSelectConversation"
                  @back="handleBack"
                  @view-user-profile="handleViewUserProfile"
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
                  @view-user-profile="handleViewUserProfile"
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
              :on-view-user-profile="handleViewUserProfile"
              v-if="!showMyPostsList && !showMyReportsList"
              :user-id="user?.id"
              :token="token"
              @view-my-posts="showMyPostsList = true"
              @view-my-reports="showMyReportsList = true"
              @view-post-detail="(postId) => handleNavigate('postDetail', postId)"
            />
            <MyPosts
              v-else-if="showMyPostsList"
              :token="token"
              @back="showMyPostsList = false"
              @view-post-detail="(postId) => handleNavigate('postDetail', postId)"
            />
            <MyReports
              v-else-if="showMyReportsList"
              :token="token"
              @back="showMyReportsList = false"
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
import MyReports from './components/MyReports.vue';
import NewPostForm from './components/NewPostForm.vue';
import ConversationList from './components/ConversationList.vue';
import ChatWindow from './components/ChatWindow.vue';
import { fetchMyRejectedPosts } from './api';
import { setLanguagePreference, getLanguagePreference } from './utils/language';
import { setLanguage, getCurrentLanguage, t } from './i18n';
// Element Plus locale for dynamic language switching
import zhCn from 'element-plus/dist/locale/zh-cn.mjs';
import en from 'element-plus/dist/locale/en.mjs';
import { ElConfigProvider, ElMessage } from 'element-plus';

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
const showMyReportsList = ref(false);

// Navigation history stack to track page visits
const navigationHistory = ref([]);

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
    console.warn('App: Received auth:unauthorized event, logging out user', e?.detail);
    
    // 统一 401 友好提示
    ElMessage.warning(
      t('auth.sessionExpired')
    );

    // 统一触发前端退出登录流程
    if (isLoggedIn.value) {
      handleLogout();
    } else {
      // 即使本地认为未登录，也确保清掉残留 token / user
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      sessionStorage.removeItem('sessionActive');
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
    // 401 情况统一交给全局拦截器 + App.vue 的 auth:unauthorized 监听来处理
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
  // Clear navigation history on login
  navigationHistory.value = [];
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
  showMyReportsList.value = false;
  // Clear navigation history on logout
  navigationHistory.value = [];
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
  // 管理员面板入口已移除：避免手动触发后出现残留页面状态
  if (page === 'admin') {
    currentPage.value = 'briefing';
    selectedPostId.value = null;
    selectedNewsId.value = null;
    selectedUserId.value = null;
    selectedConversationId.value = null;
    selectedConversation.value = null;
    showMyPostsList.value = false;
    showMyReportsList.value = false;
    ElMessage.info(lang.value === 'zh' ? '管理面板已移除' : 'Admin panel has been removed');
    return;
  }
  
  // Save current page state to history before navigating (except for initial load)
  // Only save if we're not already on the target page
  if (currentPage.value !== page || (page === 'postDetail' && selectedPostId.value !== postId)) {
    // Don't save detail pages (postDetail, news detail) to history as they're temporary views
    // Only save main pages
    const mainPages = ['briefing', 'community', 'post', 'search', 'messages', 'profile'];
    if (mainPages.includes(currentPage.value)) {
      navigationHistory.value.push({
        page: currentPage.value,
        postId: selectedPostId.value,
        newsId: selectedNewsId.value,
        userId: selectedUserId.value,
        conversationId: selectedConversationId.value,
        conversation: selectedConversation.value,
        showMyPostsList: showMyPostsList.value,
        showMyReportsList: showMyReportsList.value,
        selectedTag: selectedTag.value
      });
      // Limit history size to prevent memory issues
      if (navigationHistory.value.length > 20) {
        navigationHistory.value.shift();
      }
    }
  }
  
  if (page === 'postDetail' && postId) {
    selectedPostId.value = postId;
    currentPage.value = 'postDetail';
    selectedNewsId.value = null;
    selectedUserId.value = null;
    selectedConversationId.value = null;
    selectedConversation.value = null;
    showMyPostsList.value = false;
    showMyReportsList.value = false;
  } else if (page === 'messages') {
    currentPage.value = 'messages';
    selectedPostId.value = null;
    selectedNewsId.value = null;
    selectedUserId.value = null;
    showMyPostsList.value = false;
    showMyReportsList.value = false;
    // Keep conversation selection when navigating to messages
  } else if (page === 'profile') {
    currentPage.value = 'profile';
    showMyPostsList.value = false;
    showMyReportsList.value = false;
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
    showMyReportsList.value = false;
  }
};

// Handle back navigation using history stack
const handleBack = () => {
  if (navigationHistory.value.length > 0) {
    const previousState = navigationHistory.value.pop();
    // Restore previous page state
    currentPage.value = previousState.page;
    selectedPostId.value = previousState.postId;
    selectedNewsId.value = previousState.newsId;
    selectedUserId.value = previousState.userId;
    selectedConversationId.value = previousState.conversationId;
    selectedConversation.value = previousState.conversation;
    showMyPostsList.value = previousState.showMyPostsList;
    showMyReportsList.value = previousState.showMyReportsList;
    selectedTag.value = previousState.selectedTag || 'all';
  } else {
    // If no history, default to community page
    currentPage.value = 'community';
    selectedPostId.value = null;
    selectedNewsId.value = null;
    selectedUserId.value = null;
    selectedConversationId.value = null;
    selectedConversation.value = null;
    showMyPostsList.value = false;
    showMyReportsList.value = false;
    selectedTag.value = 'all';
  }
};

const handleViewNewsDetail = (newsId) => {
  if (newsId === null || newsId === undefined || newsId === '') {
    ElMessage.warning(lang.value === 'zh' ? '无法打开详情：新闻ID无效' : 'Cannot open detail: invalid news id');
    return;
  }
  // Save current state to history before showing news detail
  if (currentPage.value === 'briefing' && !selectedNewsId.value) {
    navigationHistory.value.push({
      page: currentPage.value,
      postId: selectedPostId.value,
      newsId: null,
      userId: selectedUserId.value,
      conversationId: selectedConversationId.value,
      conversation: selectedConversation.value,
      showMyPostsList: showMyPostsList.value,
      showMyReportsList: showMyReportsList.value,
      selectedTag: selectedTag.value
    });
    if (navigationHistory.value.length > 20) {
      navigationHistory.value.shift();
    }
  }
  // Ensure we are on the briefing page and have a valid id
  currentPage.value = 'briefing';
  selectedNewsId.value = Number(newsId);
  // Give a visible feedback (scroll to top) even if detail request is slow
  if (typeof window !== 'undefined') {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
};

const handleBackToBriefingList = () => {
  // If we have history, go back to previous page, otherwise just close the detail
  if (navigationHistory.value.length > 0) {
    handleBack();
  } else {
    selectedNewsId.value = null;
  }
};

const handleBackFromProfile = () => {
  selectedUserId.value = null;
  currentPage.value = 'community'; // Go back to community page
};

const handlePostClickFromProfile = (postId) => {
  // Save current state (profile page) to history before showing post detail
  if (currentPage.value === 'profile' || currentPage.value === 'messages') {
    navigationHistory.value.push({
      page: currentPage.value,
      postId: null,
      newsId: selectedNewsId.value,
      userId: selectedUserId.value,
      conversationId: selectedConversationId.value,
      conversation: selectedConversation.value,
      showMyPostsList: showMyPostsList.value,
      showMyReportsList: showMyReportsList.value,
      selectedTag: selectedTag.value
    });
    if (navigationHistory.value.length > 20) {
      navigationHistory.value.shift();
    }
  }
  selectedPostId.value = postId;
  currentPage.value = 'postDetail';
};

const handleSendMessageFromProfile = async (userId) => {
  // Navigate to messages page
  handleNavigate('messages');
  
  // Create or get conversation with the user
  try {
    const { createOrGetConversation, followUser } = await import('./api');
    
    // First, try to create conversation
    let response;
    try {
      response = await createOrGetConversation(userId, token.value);
    } catch (err) {
      // If it fails with "must follow", try to follow first
      if (err.response?.status === 400 && 
          (err.response?.data?.message?.includes('follow') || 
           err.response?.data?.message?.includes('Follow'))) {
        // User needs to follow first, try to follow automatically
        try {
          const followResponse = await followUser(userId, token.value);
          if (followResponse && followResponse.success) {
            // Wait a bit for the follow to be saved
            await new Promise(resolve => setTimeout(resolve, 300));
            // Now try to create conversation again
            response = await createOrGetConversation(userId, token.value);
          } else {
            throw new Error(followResponse?.message || 'Failed to follow user');
          }
        } catch (followErr) {
          // If follow fails, show the follow error
          console.error('Failed to follow user:', followErr);
          const followErrorMsg = followErr.response?.data?.message || followErr.message || 'Failed to follow user';
          alert(followErrorMsg);
          return;
        }
      } else {
        // Other error, re-throw
        throw err;
      }
    }
    
    if (response && response.success) {
      // Set the selected conversation
      selectedConversationId.value = response.conversationId;
      // Wait a bit for the conversation list to update
      await new Promise(resolve => setTimeout(resolve, 500));
    } else {
      const errorMsg = response?.message || 'Failed to start conversation';
      alert(errorMsg);
    }
  } catch (err) {
    console.error('Failed to create conversation:', err);
    const errorMsg = err.response?.data?.message || err.message || 'Failed to start conversation';
    
    // Show user-friendly error message
    if (errorMsg.includes('follow') || errorMsg.includes('Follow')) {
      alert('Please follow this user first to start a conversation.');
    } else {
      alert(errorMsg);
    }
  }
};

const handleViewUserProfile = (userId) => {
  console.log('App.vue: handleViewUserProfile called with userId:', userId);
  if (!userId) {
    console.warn('App.vue: handleViewUserProfile called with null/undefined userId');
    return;
  }
  
  // Save current state to history before showing user profile (if not already on messages page)
  if (currentPage.value !== 'messages') {
    navigationHistory.value.push({
      page: currentPage.value,
      postId: selectedPostId.value,
      newsId: selectedNewsId.value,
      userId: null,
      conversationId: selectedConversationId.value,
      conversation: selectedConversation.value,
      showMyPostsList: showMyPostsList.value,
      showMyReportsList: showMyReportsList.value,
      selectedTag: selectedTag.value
    });
    if (navigationHistory.value.length > 20) {
      navigationHistory.value.shift();
    }
  }
  
  // Set the selected user ID
  selectedUserId.value = userId;
  
  // Navigate to messages page to show user profile
  if (currentPage.value !== 'messages') {
    console.log('App.vue: Navigating to messages page');
    currentPage.value = 'messages';
  } else {
    // If already on messages page, force re-render
    console.log('App.vue: Already on messages page, forcing re-render');
    const tempId = selectedUserId.value;
    selectedUserId.value = null;
    setTimeout(() => {
      selectedUserId.value = tempId;
    }, 0);
  }
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

// handleBackToCommunity is now replaced by handleBack, but keeping for backward compatibility
const handleBackToCommunity = () => {
  handleBack();
};
</script>

<style scoped>
/* Styles are in styles.css */
</style>
