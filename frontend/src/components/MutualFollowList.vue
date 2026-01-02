<template>
  <div class="mutual-follow-list">
    <!-- Header -->
    <div class="header-section">
      <h1 class="page-title">{{ t('mutualFollowList.title') }}</h1>
      <button v-if="onBack" class="btn" @click="onBack">
        ← {{ t('common.back') }}
      </button>
    </div>

    <!-- Search Bar -->
    <div class="search-section">
      <input
        class="input"
        v-model="searchQuery"
        :placeholder="t('mutualFollowList.searchPlaceholder')"
        @input="handleSearch"
        style="width: 100%; max-width: 500px;"
      />
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>{{ t('mutualFollowList.loading') }}</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <div class="error-message">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="loadMutualFollows">
          {{ t('common.retry') }}
        </button>
      </div>
    </div>

    <!-- Mutual Follows List -->
    <div v-else class="follows-list">
      <div v-if="mutualFollows.length === 0" class="empty-state">
        <p>{{ t('mutualFollowList.noMutualFollows') }}</p>
      </div>
      <div v-else class="user-cards">
        <div
          v-for="user in mutualFollows"
          :key="user.id"
          class="user-card"
        >
          <div class="user-info" @click="handleViewProfile(user.id)">
            <div class="avatar">
              {{ (user.displayName || user.username || 'U')[0].toUpperCase() }}
            </div>
            <div class="user-details">
              <h3 class="user-name">{{ user.displayName || user.username || t('mutualFollowList.anonymous') }}</h3>
              <p class="user-username">@{{ user.username }}</p>
            </div>
          </div>
          <div class="user-actions">
            <button
              class="btn btn-primary"
              @click="handleSendMessage(user.id)"
            >
              {{ t('mutualFollowList.sendMessage') }}
            </button>
            <button
              class="btn"
              @click="handleUnfollow(user.id)"
              :disabled="unfollowingUserId === user.id"
            >
              {{ unfollowingUserId === user.id ? t('mutualFollowList.unfollowing') : t('mutualFollowList.unfollow') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { getCurrentLanguage, t } from '../i18n';

const props = defineProps({
  token: {
    type: String,
    required: true
  },
  onBack: {
    type: Function,
    default: null
  },
  onViewProfile: {
    type: Function,
    default: null
  },
  onSendMessage: {
    type: Function,
    default: null
  }
});

const mutualFollows = ref([]);
const loading = ref(true);
const error = ref(null);
const searchQuery = ref('');
const unfollowingUserId = ref(null);
const lang = ref(getCurrentLanguage());

const loadMutualFollows = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    const params = searchQuery.value ? { q: searchQuery.value } : {};
    const response = await axios.get('/api/users/mutual-follows', {
      params,
      headers: { Authorization: `Bearer ${props.token}` }
    });
    
    if (response.data.success) {
      mutualFollows.value = response.data.data || [];
    } else {
      error.value = response.data.message || t('mutualFollowList.loadFailed');
    }
  } catch (err) {
    console.error('Failed to load mutual follows:', err);
    if (err.response && err.response.status === 401) {
      error.value = t('mutualFollowList.loginRequired');
    } else {
      error.value = err.response?.data?.message || err.message || t('mutualFollowList.loadFailed');
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  loadMutualFollows();
};

const handleViewProfile = (userId) => {
  if (props.onViewProfile) {
    props.onViewProfile(userId);
  } else {
    // Emit event to parent
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('viewUserProfile', { 
        detail: { userId } 
      }));
    }
  }
};

const handleSendMessage = (userId) => {
  if (props.onSendMessage) {
    props.onSendMessage(userId);
  } else {
    // Emit event to parent
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('openMessage', { 
        detail: { userId } 
      }));
    }
  }
};

const handleUnfollow = async (userId) => {
  if (!confirm(t('mutualFollowList.confirmUnfollow'))) {
    return;
  }
  
  unfollowingUserId.value = userId;
  try {
    await axios.delete(`/api/users/${userId}/follow`, {
      headers: { Authorization: `Bearer ${props.token}` }
    });
    
    // Remove from list
    mutualFollows.value = mutualFollows.value.filter(u => u.id !== userId);
    alert(t('mutualFollowList.unfollowed'));
  } catch (err) {
    console.error('Failed to unfollow:', err);
    alert(err.response?.data?.message || t('mutualFollowList.unfollowFailed'));
  } finally {
    unfollowingUserId.value = null;
  }
};

onMounted(() => {
  loadMutualFollows();
  
  // Listen for language changes
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      lang.value = e.detail.lang;
    }
  };
  
  if (typeof window !== 'undefined') {
    window.addEventListener('languageChanged', handleLanguageChange);
  }
  
  return () => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('languageChanged', handleLanguageChange);
    }
  };
});
</script>

<style scoped>
.mutual-follow-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.search-section {
  margin-bottom: 24px;
}

.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.loading-content {
  text-align: center;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
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
  padding: 60px 20px;
  color: #909399;
}

.user-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s;
}

.user-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  cursor: pointer;
}

.avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
}

.user-name {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.user-username {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.user-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .mutual-follow-list {
    padding: 16px;
  }

  .user-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .user-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 12px;
  }
}
</style>

