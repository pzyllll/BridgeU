<template>
  <div class="conversation-list" :key="lang">
    <!-- Header -->
    <div class="header-section">
      <h1 class="page-title">{{ t('messages.title') }}</h1>
      <button v-if="onBack" class="btn" @click="onBack">
        ← {{ t('common.back') }}
      </button>
    </div>

    <!-- Search Section -->
    <div class="search-section">
      <input
        v-model="searchQuery"
        type="text"
        :placeholder="t('messages.searchUsers')"
        class="search-input"
        @input="handleSearch"
      />
      <button v-if="searchQuery" class="btn-clear-search" @click="clearSearch">✕</button>
    </div>

    <!-- Search Results -->
    <div v-if="searchQuery && searchResults.length > 0" class="search-results">
      <h3 class="search-results-title">{{ t('messages.searchResults') }}</h3>
      <div class="user-cards-grid">
        <div
          v-for="user in searchResults"
          :key="user.id"
          class="user-card"
          @click="handleViewUserProfile(user.id)"
        >
          <div class="user-card-avatar" :style="{ backgroundImage: user.avatar ? `url(${user.avatar})` : '' }">
            {{ !user.avatar ? (user.displayName || user.username || 'U')[0].toUpperCase() : '' }}
          </div>
          <div class="user-card-info">
            <h4 class="user-card-name">{{ user.displayName || user.username || t('messages.anonymous') }}</h4>
            <p class="user-card-username">@{{ user.username }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- No Search Results -->
    <div v-if="searchQuery && !searchLoading && searchResults.length === 0" class="no-search-results">
      <p>{{ t('messages.noSearchResults') }}</p>
    </div>

    <!-- Loading State -->
    <div v-if="loading && !searchQuery" class="loading-container">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>{{ t('messages.loading') }}</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error && !searchQuery" class="error-container">
      <div class="error-message">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="loadConversations">
          {{ t('common.retry') }}
        </button>
      </div>
    </div>

    <!-- Conversations List -->
    <div v-else-if="!searchQuery" class="conversations-container">
      <div v-if="conversations.length === 0" class="empty-state">
        <p>{{ t('messages.noConversations') }}</p>
      </div>
      <div v-else class="conversations-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conversation-card"
          :class="{ active: selectedConversationId === conv.id, unread: conv.unreadCount > 0 }"
          @click="handleSelectConversation(conv)"
        >
          <div class="conversation-avatar">
            {{ (conv.otherUser?.displayName || conv.otherUser?.username || 'U')[0].toUpperCase() }}
          </div>
          <div class="conversation-info">
            <div class="conversation-header">
              <h3 class="conversation-name">
                {{ conv.otherUser?.displayName || conv.otherUser?.username || t('messages.anonymous') }}
              </h3>
              <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount }}</span>
              <span class="conversation-time">{{ formatTime(conv.lastMessageAt || conv.createdAt) }}</span>
            </div>
            <p class="conversation-preview">
              {{ getLastMessagePreview(conv) }}
            </p>
          </div>
          <button
            class="btn-icon"
            @click.stop="handleDeleteConversation(conv.id)"
            :title="t('messages.deleteConversation')"
          >
            🗑️
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { getConversations, deleteConversation, searchUsers } from '../api';
import { getCurrentLanguage, t } from '../i18n';

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  selectedConversationId: {
    type: String,
    default: null
  },
  onBack: {
    type: Function,
    default: null
  },
  onSelectConversation: {
    type: Function,
    required: true
  },
  onViewUserProfile: {
    type: Function,
    default: null
  }
});

// Add reactive language state to trigger re-renders
const lang = ref(getCurrentLanguage());

const loading = ref(false);
const error = ref(null);
const conversations = ref([]);
const searchQuery = ref('');
const searchResults = ref([]);
const searchLoading = ref(false);

const loadConversations = async () => {
  if (!props.token) {
    error.value = t('messages.loginRequired');
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const response = await getConversations(props.token);
    if (response.success && response.data) {
      conversations.value = response.data;
    } else {
      error.value = response.message || t('messages.loadFailed');
    }
  } catch (err) {
    console.error('Failed to load conversations:', err);
    error.value = err.response?.data?.message || t('messages.loadFailed');
  } finally {
    loading.value = false;
  }
};

const handleSelectConversation = (conversation) => {
  props.onSelectConversation(conversation);
};

const handleDeleteConversation = async (conversationId) => {
  if (!confirm(t('messages.confirmDelete'))) {
    return;
  }

  try {
    const response = await deleteConversation(conversationId, props.token);
    if (response.success) {
      // Remove from list
      conversations.value = conversations.value.filter(c => c.id !== conversationId);
      // Notify parent
      if (props.selectedConversationId === conversationId) {
        props.onSelectConversation(null);
      }
    } else {
      alert(response.message || t('messages.deleteFailed'));
    }
  } catch (err) {
    console.error('Failed to delete conversation:', err);
    alert(err.response?.data?.message || t('messages.deleteFailed'));
  }
};

const getLastMessagePreview = (conv) => {
  if (!conv.lastMessage) {
    return t('messages.emptyConversation');
  }
  const content = conv.lastMessage.content || '';
  return content.length > 50 ? content.substring(0, 50) + '...' : content;
};

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = [];
    return;
  }

  searchLoading.value = true;
  try {
    const response = await searchUsers(searchQuery.value.trim(), 20, props.token);
    if (response.success) {
      searchResults.value = response.data || [];
    } else {
      searchResults.value = [];
    }
  } catch (err) {
    console.error('Failed to search users:', err);
    searchResults.value = [];
  } finally {
    searchLoading.value = false;
  }
};

const clearSearch = () => {
  searchQuery.value = '';
  searchResults.value = [];
};

const handleViewUserProfile = (userId) => {
  if (props.onViewUserProfile) {
    props.onViewUserProfile(userId);
  }
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  
  // Parse timestamp and validate
  let time;
  try {
    time = new Date(timestamp);
    // Check if date is invalid or before 1971 (to catch 1970 dates)
    if (isNaN(time.getTime()) || time.getTime() < 0 || time.getFullYear() < 1971) {
      time = new Date(); // Use current time as fallback
    }
  } catch (e) {
    time = new Date(); // Use current time as fallback
  }
  
  // Calculate time difference (timezone-independent)
  const now = new Date();
  const diffMs = now.getTime() - time.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  // Show relative time for recent messages
  if (diffMins < 1) {
    return t('messages.justNow');
  } else if (diffMins < 60) {
    return `${diffMins}${t('messages.minutesAgo')}`;
  } else if (diffHours < 24) {
    return `${diffHours}${t('messages.hoursAgo')}`;
  } else if (diffDays < 7) {
    return `${diffDays}${t('messages.daysAgo')}`;
  } else {
    // For older messages, format with Thailand timezone
    const formatter = new Intl.DateTimeFormat(lang.value === 'zh' ? 'zh-CN' : 'en-US', {
      timeZone: 'Asia/Bangkok',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
    return formatter.format(time);
  }
};

// Listen for language changes
const handleLanguageChange = (e) => {
  if (e && e.detail && e.detail.lang) {
    lang.value = e.detail.lang;
    // Force re-render by updating a reactive value
    // The t() function will automatically use the new language
  }
};

onMounted(() => {
  loadConversations();
  
  // Add language change listener
  if (typeof window !== 'undefined') {
    window.addEventListener('languageChanged', handleLanguageChange);
  }
});

onUnmounted(() => {
  // Clean up language change listener
  if (typeof window !== 'undefined') {
    window.removeEventListener('languageChanged', handleLanguageChange);
  }
});

// Expose refresh method
defineExpose({
  refresh: loadConversations
});
</script>

<style scoped>
.conversation-list {
  padding: 1rem;
  max-width: 800px;
  margin: 0 auto;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: bold;
  margin: 0;
}

.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.loading-content {
  text-align: center;
}

.spinner {
  border: 3px solid #f3f3f3;
  border-top: 3px solid #333;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  text-align: center;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  color: #666;
}

.conversations-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.conversation-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.conversation-card:hover {
  border-color: #333;
  background: #f9f9f9;
}

.conversation-card.active {
  border-color: #333;
  background: #f0f0f0;
}

.conversation-card.unread {
  border-color: #4a90e2;
  background: #f0f7ff;
}

.conversation-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #333;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.conversation-name {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-badge {
  background: #4a90e2;
  color: white;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 0.75rem;
  font-weight: bold;
  min-width: 20px;
  text-align: center;
}

.conversation-time {
  font-size: 0.75rem;
  color: #666;
  white-space: nowrap;
}

.conversation-preview {
  font-size: 0.875rem;
  color: #666;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0.5rem;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.btn-icon:hover {
  opacity: 1;
}

.btn {
  padding: 0.5rem 1rem;
  border: 2px solid #333;
  background: white;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn:hover {
  background: #333;
  color: white;
}

.btn-primary {
  background: #333;
  color: white;
}

.btn-primary:hover {
  background: #555;
}

.search-section {
  position: relative;
  margin-bottom: 1.5rem;
}

.search-input {
  width: 100%;
  padding: 0.75rem 2.5rem 0.75rem 1rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  font-family: inherit;
}

.search-input:focus {
  outline: none;
  border-color: #333;
}

.btn-clear-search {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0.25rem 0.5rem;
  color: #666;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.btn-clear-search:hover {
  opacity: 1;
}

.search-results {
  margin-bottom: 1.5rem;
}

.search-results-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #333;
}

.user-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 2px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.user-card:hover {
  border-color: #333;
  background: #f9f9f9;
}

.user-card-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #333;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1.2rem;
  flex-shrink: 0;
  background-size: cover;
  background-position: center;
}

.user-card-info {
  flex: 1;
  min-width: 0;
}

.user-card-name {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.25rem 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-card-username {
  font-size: 0.875rem;
  color: #666;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-search-results {
  text-align: center;
  padding: 2rem 1rem;
  color: #666;
}
</style>

