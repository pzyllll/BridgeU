<template>
  <div class="user-profile">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>{{ t('userProfile.loading') }}</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <div class="error-message">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="loadProfile">
          {{ t('common.retry') }}
        </button>
      </div>
    </div>

    <!-- Profile Content -->
    <div v-else-if="profile" class="profile-content">
      <!-- Back Button -->
      <div class="back-section" v-if="onBack">
        <button class="btn" @click="onBack">
          ← {{ t('common.back') }}
        </button>
      </div>

      <!-- Profile Header -->
      <div class="profile-header">
        <div class="avatar-section">
          <div class="avatar" :style="{ backgroundImage: profile.user.avatar ? `url(${profile.user.avatar})` : '' }">
            {{ !profile.user.avatar ? (profile.user.displayName || profile.user.username || 'U')[0].toUpperCase() : '' }}
          </div>
        </div>
        <div class="profile-info">
          <h1 class="profile-name">{{ profile.user.displayName || profile.user.username || t('userProfile.anonymous') }}</h1>
          <p class="profile-username">@{{ profile.user.username }}</p>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ profile.postCount || 0 }}</span>
              <span class="stat-label">{{ t('userProfile.posts') }}</span>
            </div>
            <div class="stat-item clickable" @click="showFollowersModal = true">
              <span class="stat-value">{{ profile.followersCount || 0 }}</span>
              <span class="stat-label">{{ t('userProfile.followers') }}</span>
            </div>
            <div class="stat-item clickable" @click="showMutualFollowsModal = true">
              <span class="stat-value">{{ profile.mutualFollowsCount || 0 }}</span>
              <span class="stat-label">{{ t('userProfile.mutualFollows') }}</span>
            </div>
          </div>
          <div class="profile-actions" v-if="token && !profile.isOwnProfile">
            <button
              class="btn"
              :class="profile.isFollowing ? '' : 'btn-primary'"
              @click="handleToggleFollow"
            >
              {{ profile.isFollowing ? t('userProfile.following') : t('userProfile.follow') }}
            </button>
            <button
              class="btn btn-primary"
              @click="handleSendMessage"
              :title="!profile.isFollowing ? t('userProfile.followFirstToMessage') : ''"
            >
              {{ t('userProfile.sendMessage') }}
            </button>
          </div>
        </div>
      </div>

      <!-- User Posts -->
      <div class="posts-section">
        <h2 class="section-title">{{ t('userProfile.userPosts') }}</h2>
        <div v-if="profile.posts && profile.posts.length > 0" class="posts-list">
          <article
            v-for="post in profile.posts"
            :key="post.id"
            class="post-card"
            @click="handlePostClick(post.id)"
          >
            <div class="post-header">
              <span class="post-tag" v-if="post.tags && post.tags.length > 0">
                {{ getTagEmoji(post.tags) }} #{{ formatTag(post.tags[0]) }}
              </span>
            </div>
            <h3 class="post-title">{{ post.title }}</h3>
            <div v-if="post.imageUrl" class="post-image">
              <img :src="post.imageUrl" :alt="post.title" />
            </div>
            <p class="post-body">{{ truncateText(post.body, 150) }}</p>
            <div class="post-footer">
              <span>{{ formatTime(post.createdAt) }}</span>
              <div class="post-stats">
                <span>❤️ {{ post.likeCount || 0 }}</span>
                <span>💬 {{ post.commentCount || 0 }}</span>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="empty-posts">
          <p>{{ t('userProfile.noPosts') }}</p>
        </div>
      </div>
    </div>

    <!-- User List Modals -->
    <UserListModal
      :show="showFollowersModal"
      :userId="userId"
      type="followers"
      :token="token"
      :currentUserId="currentUserId"
      @close="showFollowersModal = false"
      @user-click="handleViewUserProfile"
    />
    <UserListModal
      :show="showMutualFollowsModal"
      :userId="userId"
      type="mutual-follows"
      :token="token"
      :currentUserId="currentUserId"
      @close="showMutualFollowsModal = false"
      @user-click="handleViewUserProfile"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { getCurrentLanguage, t } from '../i18n';
import UserListModal from './UserListModal.vue';

const props = defineProps({
  userId: {
    type: [String, Number],
    required: true
  },
  token: {
    type: String,
    default: null
  },
  currentUserId: {
    type: [String, Number],
    default: null
  },
  onBack: {
    type: Function,
    default: null
  },
  onPostClick: {
    type: Function,
    default: null
  },
  onSendMessage: {
    type: Function,
    default: null
  }
});

const profile = ref(null);
const loading = ref(true);
const error = ref(null);
const lang = ref(getCurrentLanguage());
const showFollowersModal = ref(false);
const showMutualFollowsModal = ref(false);

const loadProfile = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    const currentLang = getCurrentLanguage();
    const response = await axios.get(`/api/users/${props.userId}`, {
      params: { lang: currentLang },
      headers: props.token ? { Authorization: `Bearer ${props.token}` } : {}
    });
    
    if (response.data.success) {
      profile.value = response.data;
    } else {
      error.value = response.data.message || t('userProfile.loadFailed');
    }
  } catch (err) {
    console.error('Failed to load user profile:', err);
    if (err.response && err.response.status === 404) {
      error.value = t('userProfile.notFound');
    } else {
      error.value = err.response?.data?.message || err.message || t('userProfile.loadFailed');
    }
  } finally {
    loading.value = false;
  }
};

const handleToggleFollow = async () => {
  if (!props.token) {
    alert(t('userProfile.loginRequired'));
    return;
  }
  
  try {
    if (profile.value?.isFollowing) {
      // Unfollow
      const response = await axios.delete(
        `/api/users/${props.userId}/follow`,
        { headers: { Authorization: `Bearer ${props.token}` } }
      );
      if (response.data.success && profile.value) {
        profile.value.isFollowing = false;
      }
    } else {
      // Follow
      const response = await axios.post(
        `/api/users/${props.userId}/follow`,
        {},
        { headers: { Authorization: `Bearer ${props.token}` } }
      );
      if (response.data.success && profile.value) {
        profile.value.isFollowing = true;
      }
    }
  } catch (err) {
    console.error('Failed to toggle follow:', err);
    alert(err.response?.data?.message || t('userProfile.followFailed'));
  }
};

const handlePostClick = (postId) => {
  if (props.onPostClick) {
    props.onPostClick(postId);
  }
};

const handleSendMessage = () => {
  if (props.onSendMessage) {
    props.onSendMessage(props.userId);
  }
};

const handleViewUserProfile = (userId) => {
  // Close the modals first
  showFollowersModal.value = false;
  showMutualFollowsModal.value = false;
  // Emit event to parent to navigate to user profile
  // The parent (App.vue) should handle this navigation
  if (props.onViewUserProfile) {
    props.onViewUserProfile(userId);
  }
};

const getTagEmoji = (tags) => {
  if (!tags || !Array.isArray(tags)) return '📝';
  const tagList = tags.map(t => t.toLowerCase());
  if (tagList.some(t => ['study', 'learning', 'course', 'class', 'education'].includes(t))) return '📚';
  if (tagList.some(t => ['housing', 'rent', 'rental', 'accommodation', 'apartment'].includes(t))) return '🏠';
  if (tagList.some(t => ['travel', 'tourism', 'trip', 'visa', 'studyabroad'].includes(t))) return '✈️';
  if (tagList.some(t => ['part-time', 'parttime', 'job', 'work', 'employment'].includes(t))) return '💼';
  if (tagList.some(t => ['life', 'service', 'services', 'food', 'lifestyle', 'market', 'secondhand'].includes(t))) return '🛒';
  return '📝';
};

const formatTag = (tag) => {
  if (!tag) return 'Post';
  const lower = tag.toLowerCase();
  if (['study', 'learning', 'course', 'class', 'education'].includes(lower)) return 'Study';
  if (['housing', 'rent', 'rental', 'accommodation', 'apartment'].includes(lower)) return 'Housing';
  if (['travel', 'tourism', 'trip', 'visa', 'studyabroad'].includes(lower)) return 'Travel';
  if (['part-time', 'parttime', 'job', 'work', 'employment'].includes(lower)) return 'Part-time Job';
  if (['life', 'service', 'services', 'food', 'lifestyle', 'market', 'secondhand'].includes(lower)) return 'Life Services';
  return tag;
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  
  if (diffMins < 1) return lang.value === 'zh' ? '刚刚' : 'Just now';
  if (diffMins < 60) return `${diffMins}${lang.value === 'zh' ? '分钟前' : 'm ago'}`;
  if (diffHours < 24) return `${diffHours}${lang.value === 'zh' ? '小时前' : 'h ago'}`;
  if (diffDays < 7) return `${diffDays}${lang.value === 'zh' ? '天前' : 'd ago'}`;
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${day}-${month}-${year}`;
};

const truncateText = (text, maxLength) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

onMounted(() => {
  loadProfile();
  
  // Listen for language changes
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      lang.value = e.detail.lang;
      loadProfile();
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
.user-profile {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
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

.back-section {
  margin-bottom: 20px;
}

.profile-header {
  display: flex;
  gap: 24px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: bold;
  color: white;
  background-size: cover;
  background-position: center;
}

.profile-info {
  flex: 1;
}

.profile-name {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.profile-username {
  margin: 0 0 16px 0;
  color: #909399;
  font-size: 16px;
}

.profile-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
}

.stat-item.clickable {
  cursor: pointer;
  transition: opacity 0.2s;
}

.stat-item.clickable:hover {
  opacity: 0.7;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.profile-actions {
  display: flex;
  gap: 12px;
}

.posts-section {
  margin-top: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #303133;
}

.posts-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.post-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s;
}

.post-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.post-tag {
  padding: 4px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  font-size: 14px;
  color: #1e40af;
}

.post-title {
  margin: 0 0 12px 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.post-image {
  margin-bottom: 12px;
}

.post-image img {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border-radius: 8px;
}

.post-body {
  margin: 0 0 12px 0;
  color: #606266;
  line-height: 1.6;
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 14px;
  color: #909399;
}

.post-stats {
  display: flex;
  gap: 16px;
}

.empty-posts {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

@media (max-width: 768px) {
  .user-profile {
    padding: 16px;
  }

  .profile-header {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .profile-actions {
    justify-content: center;
  }
}
</style>

