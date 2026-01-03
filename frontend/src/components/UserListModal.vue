<template>
  <div v-if="show" class="modal-overlay" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h2 class="modal-title">{{ title }}</h2>
        <button class="btn-close" @click="handleClose">×</button>
      </div>
      
      <div class="modal-body">
        <div v-if="loading" class="loading-container">
          <div class="spinner"></div>
          <p>{{ t('userList.loading') }}</p>
        </div>
        
        <div v-else-if="error" class="error-container">
          <p>{{ error }}</p>
          <button class="btn btn-primary" @click="loadUsers">
            {{ t('common.retry') }}
          </button>
        </div>
        
        <div v-else-if="users.length === 0" class="empty-container">
          <p>{{ t('userList.noUsers') }}</p>
        </div>
        
        <div v-else class="users-list">
          <div
            v-for="user in users"
            :key="user.id"
            class="user-item"
            @click="handleUserClick(user.id)"
          >
            <div class="user-avatar" :style="{ backgroundImage: user.avatar ? `url(${user.avatar})` : '' }">
              {{ !user.avatar ? (user.displayName || user.username || 'U')[0].toUpperCase() : '' }}
            </div>
            <div class="user-info">
              <h4 class="user-name">{{ user.displayName || user.username || t('userList.anonymous') }}</h4>
              <p class="user-username">@{{ user.username }}</p>
            </div>
            <button
              v-if="showFollowButton && !user.isOwnProfile"
              class="btn btn-small"
              :class="user.isFollowing ? '' : 'btn-primary'"
              @click.stop="handleToggleFollow(user)"
            >
              {{ user.isFollowing ? t('userList.following') : t('userList.follow') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { getFollowers, getUserMutualFollows, followUser, unfollowUser } from '../api';
import { t } from '../i18n';

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  userId: {
    type: String,
    required: true
  },
  type: {
    type: String,
    required: true, // 'followers' or 'mutual-follows'
    validator: (value) => ['followers', 'mutual-follows'].includes(value)
  },
  token: {
    type: String,
    default: null
  },
  currentUserId: {
    type: String,
    default: null
  },
  onClose: {
    type: Function,
    default: null
  },
  onUserClick: {
    type: Function,
    default: null
  }
});

const emit = defineEmits(['close', 'user-click']);

const users = ref([]);
const loading = ref(false);
const error = ref(null);

const title = computed(() => {
  if (props.type === 'followers') {
    return t('userList.followers');
  } else if (props.type === 'mutual-follows') {
    return t('userList.mutualFollows');
  }
  return '';
});

const showFollowButton = computed(() => {
  return props.token && props.currentUserId && props.userId !== props.currentUserId;
});

const loadUsers = async () => {
  if (!props.userId || !props.token) {
    error.value = t('userList.loginRequired');
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    let response;
    if (props.type === 'followers') {
      response = await getFollowers(props.userId, props.token);
    } else if (props.type === 'mutual-follows') {
      response = await getUserMutualFollows(props.userId, props.token);
    }

    if (response.success) {
      // Mark own profile
      users.value = (response.data || []).map(user => ({
        ...user,
        isOwnProfile: user.id === props.currentUserId
      }));
    } else {
      error.value = response.message || t('userList.loadFailed');
    }
  } catch (err) {
    console.error('Failed to load users:', err);
    error.value = err.response?.data?.message || err.message || t('userList.loadFailed');
  } finally {
    loading.value = false;
  }
};

const handleClose = () => {
  if (props.onClose) {
    props.onClose();
  }
  emit('close');
};

const handleUserClick = (userId) => {
  if (props.onUserClick) {
    props.onUserClick(userId);
  }
  emit('user-click', userId);
};

const handleToggleFollow = async (user) => {
  if (!props.token) {
    alert(t('userList.loginRequired'));
    return;
  }

  try {
    if (user.isFollowing) {
      // Unfollow
      await unfollowUser(user.id, props.token);
      user.isFollowing = false;
    } else {
      // Follow
      await followUser(user.id, props.token);
      user.isFollowing = true;
    }
  } catch (err) {
    console.error('Failed to toggle follow:', err);
    alert(err.response?.data?.message || t('userList.followFailed'));
  }
};

watch(() => props.show, (newVal) => {
  if (newVal) {
    loadUsers();
  } else {
    users.value = [];
    error.value = null;
  }
});
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #ebeef5;
}

.modal-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.btn-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #909399;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f5f7fa;
  color: #303133;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.loading-container,
.error-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.users-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item:hover {
  background: #f5f7fa;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
  background-size: cover;
  background-position: center;
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-username {
  margin: 0;
  font-size: 14px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-small {
  padding: 6px 16px;
  font-size: 14px;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    max-height: 90vh;
  }

  .modal-header {
    padding: 16px 20px;
  }

  .modal-body {
    padding: 20px;
  }
}
</style>

