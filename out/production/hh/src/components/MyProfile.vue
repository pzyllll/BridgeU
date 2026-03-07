<template>
  <div class="my-profile">
    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>{{ t('myProfile.loading') }}</p>
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
      <!-- Profile Header -->
      <div class="profile-header">
        <div class="avatar-section">
          <div 
            class="avatar" 
            :style="{ backgroundImage: profile.avatar ? `url(${profile.avatar})` : '' }"
          >
            {{ !profile.avatar ? (profile.displayName || profile.username || 'U')[0].toUpperCase() : '' }}
          </div>
          <button 
            v-if="!isEditing" 
            class="btn-edit-avatar" 
            @click="startEditAvatar"
            :title="t('myProfile.changeAvatar')"
          >
            📷
          </button>
        </div>
        <div class="profile-info">
          <h1 class="profile-name">{{ profile.displayName || profile.username || t('myProfile.anonymous') }}</h1>
          <p class="profile-username">@{{ profile.username }}</p>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-value">{{ myPostsCount }}</span>
              <span class="stat-label">{{ t('myProfile.posts') }}</span>
            </div>
            <div class="stat-item clickable" @click="showFollowersModal = true">
              <span class="stat-value">{{ profile.followersCount || 0 }}</span>
              <span class="stat-label">{{ t('myProfile.followers') }}</span>
            </div>
            <div class="stat-item clickable" @click="showMutualFollowsModal = true">
              <span class="stat-value">{{ profile.mutualFollowsCount || 0 }}</span>
              <span class="stat-label">{{ t('myProfile.mutualFollows') }}</span>
            </div>
          </div>
          <div class="profile-actions">
            <button 
              v-if="!isEditing" 
              class="btn btn-primary" 
              @click="startEditing"
            >
              {{ t('myProfile.editProfile') }}
            </button>
            <button 
              v-if="!isEditing" 
              class="btn" 
              @click="viewMyPosts"
            >
              {{ t('myProfile.myCommunityPosts') }}
            </button>
            <button 
              v-if="!isEditing" 
              class="btn" 
              @click="viewMyReports"
            >
              {{ t('postDetail.viewMyReports') }}
            </button>
          </div>
        </div>
      </div>

      <!-- Edit Form -->
      <div v-if="isEditing" class="edit-form card">
        <h2 class="section-title">{{ t('myProfile.editProfile') }}</h2>
        
        <div class="form-group">
          <label>{{ t('myProfile.displayName') }}</label>
          <input 
            type="text" 
            v-model="editForm.displayName" 
            :placeholder="t('myProfile.displayNamePlaceholder')"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label>{{ t('myProfile.avatar') }}</label>
          <div class="avatar-upload-section">
            <div class="avatar-preview-container">
              <div 
                class="avatar-preview" 
                :style="{ backgroundImage: avatarPreview ? `url(${avatarPreview})` : (profile?.avatar ? `url(${profile.avatar})` : '') }"
              >
                {{ !avatarPreview && !profile?.avatar ? (profile?.displayName || profile?.username || 'U')[0].toUpperCase() : '' }}
              </div>
              <div v-if="uploading" class="upload-overlay">
                <div class="spinner-small"></div>
                <p>{{ t('myProfile.uploading') }}</p>
              </div>
            </div>
            <div class="avatar-upload-controls">
              <input 
                type="file" 
                ref="fileInput"
                @change="handleFileSelect"
                accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
                class="file-input"
                id="avatar-upload"
              />
              <label for="avatar-upload" class="btn btn-secondary">
                {{ t('myProfile.selectImage') }}
              </label>
              <button 
                v-if="selectedFile || avatarPreview" 
                class="btn btn-link" 
                @click="clearAvatarSelection"
              >
                {{ t('myProfile.clear') }}
              </button>
              <small class="form-hint">{{ t('myProfile.avatarUploadHint') }}</small>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>{{ t('myProfile.preferredLanguage') }}</label>
          <select v-model="editForm.preferredLanguage" class="form-input">
            <option value="en">English</option>
            <option value="zh">中文</option>
          </select>
        </div>

        <div class="form-actions">
          <button class="btn btn-primary" @click="saveProfile" :disabled="saving">
            {{ saving ? t('common.saving') : t('common.save') }}
          </button>
          <button class="btn" @click="cancelEditing" :disabled="saving">
            {{ t('common.cancel') }}
          </button>
        </div>
      </div>

      <!-- My Posts Section -->
      <div v-if="!isEditing && !showMyPosts" class="posts-section">
        <h2 class="section-title">{{ t('myProfile.myPosts') }}</h2>
        <div v-if="myPosts && myPosts.length > 0" class="posts-preview">
          <div 
            v-for="post in myPosts.slice(0, 5)" 
            :key="post.id"
            class="post-preview-card"
            @click="viewPostDetail(post.id)"
          >
            <div class="post-preview-header">
              <h3 class="post-preview-title">{{ post.title }}</h3>
              <span :class="['status-badge', getStatusClass(post.status)]">
                {{ getStatusText(post.status) }}
              </span>
            </div>
            <div class="post-preview-footer">
              <span class="post-preview-time">{{ formatTime(post.createdAt) }}</span>
            </div>
          </div>
          <button v-if="myPosts.length > 5" class="btn btn-link" @click="viewMyPosts">
            {{ t('myProfile.viewAllPosts') }} ({{ myPosts.length }})
          </button>
        </div>
        <div v-else class="empty-posts">
          <p>{{ t('myProfile.noPosts') }}</p>
        </div>
      </div>
    </div>

    <!-- User List Modals -->
    <UserListModal
      :show="showFollowersModal"
      :userId="profile?.id || userId"
      type="followers"
      :token="token"
      :currentUserId="userId"
      @close="showFollowersModal = false"
      @user-click="handleViewUserProfile"
    />
    <UserListModal
      :show="showMutualFollowsModal"
      :userId="profile?.id || userId"
      type="mutual-follows"
      :token="token"
      :currentUserId="userId"
      @close="showMutualFollowsModal = false"
      @user-click="handleViewUserProfile"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import { getCurrentLanguage, t, setLanguage } from '../i18n';
import UserListModal from './UserListModal.vue';
import { parseBackendDate, formatBangkokAbsolute } from '../utils/datetime';

const props = defineProps({
  userId: {
    type: [String, Number],
    required: true
  },
  token: {
    type: String,
    required: true
  },
  onViewUserProfile: {
    type: Function,
    default: null
  }
});

const emit = defineEmits(['viewMyPosts', 'viewPostDetail', 'viewMyReports']);

const profile = ref(null);
const myPosts = ref([]);
const loading = ref(true);
const error = ref(null);
const isEditing = ref(false);
const saving = ref(false);
const showMyPosts = ref(false);
const lang = ref(getCurrentLanguage());
const selectedFile = ref(null);
const avatarPreview = ref(null);
const uploading = ref(false);
const fileInput = ref(null);
const showFollowersModal = ref(false);
const showMutualFollowsModal = ref(false);

const editForm = ref({
  displayName: '',
  avatar: '',
  preferredLanguage: 'en'
});

const myPostsCount = computed(() => myPosts.value?.length || 0);

const handleViewUserProfile = (userId) => {
  // Close the modals first
  showFollowersModal.value = false;
  showMutualFollowsModal.value = false;
  // Emit event to parent to navigate to user profile
  if (props.onViewUserProfile) {
    props.onViewUserProfile(userId);
  } else {
    // Fallback for non-prop usage patterns
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('viewUserProfile', {
        detail: { userId }
      }));
    }
  }
};

const loadProfile = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    console.log('Loading profile for user:', props.userId, 'Token:', props.token ? 'Present' : 'Missing');
    
    // Load user profile
    const profileResponse = await axios.get(`/api/users/me`, {
      headers: { Authorization: `Bearer ${props.token}` }
    });
    
    console.log('Profile response:', profileResponse.data);
    
    if (profileResponse.data.success) {
      profile.value = profileResponse.data.data;
      editForm.value = {
        displayName: profile.value.displayName || profile.value.username || '',
        avatar: profile.value.avatar || '',
        preferredLanguage: profile.value.preferredLanguage || 'en'
      };
      console.log('Profile loaded successfully:', profile.value);
    } else {
      error.value = profileResponse.data.message || t('myProfile.loadFailed');
      console.error('Profile load failed:', profileResponse.data.message);
    }

    // Load my posts
    await loadMyPosts();
  } catch (err) {
    console.error('Failed to load profile:', err);
    console.error('Error details:', {
      message: err.message,
      response: err.response?.data,
      status: err.response?.status
    });
    
    // Filter out browser extension errors
    if (err.message && (
      err.message.includes('content-all.js') ||
      err.message.includes('chrome-extension') ||
      err.message.includes('Could not establish connection')
    )) {
      console.warn('Browser extension error ignored:', err.message);
      // Try to continue loading anyway
      error.value = null;
    } else if (err.response && err.response.status === 401) {
      error.value = t('myProfile.unauthorized');
    } else {
      error.value = err.response?.data?.message || err.message || t('myProfile.loadFailed');
    }
  } finally {
    loading.value = false;
  }
};

const loadMyPosts = async () => {
  try {
    const postsResponse = await axios.get(`/api/users/me/posts`, {
      headers: { Authorization: `Bearer ${props.token}` },
      params: { lang: lang.value }
    });
    
    if (postsResponse.data.success) {
      myPosts.value = postsResponse.data.data || [];
    }
  } catch (err) {
    console.error('Failed to load my posts:', err);
    // Don't show error for posts, just log it
  }
};

const startEditing = () => {
  isEditing.value = true;
};

const startEditAvatar = () => {
  isEditing.value = true;
  // Focus on file input
  setTimeout(() => {
    if (fileInput.value) {
      fileInput.value.click();
    }
  }, 100);
};

const handleFileSelect = (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 验证文件类型
  const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
  if (!validTypes.includes(file.type)) {
    alert(t('myProfile.invalidFileType'));
    return;
  }
  
  // 验证文件大小（5MB）
  const maxSize = 5 * 1024 * 1024;
  if (file.size > maxSize) {
    alert(t('myProfile.fileTooLarge'));
    return;
  }
  
  selectedFile.value = file;
  
  // 创建预览
  const reader = new FileReader();
  reader.onload = (e) => {
    avatarPreview.value = e.target.result;
  };
  reader.readAsDataURL(file);
};

const clearAvatarSelection = () => {
  selectedFile.value = null;
  avatarPreview.value = null;
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const cancelEditing = () => {
  isEditing.value = false;
  // Reset form
  if (profile.value) {
    editForm.value = {
      displayName: profile.value.displayName || profile.value.username || '',
      avatar: profile.value.avatar || '',
      preferredLanguage: profile.value.preferredLanguage || 'en'
    };
  }
  // Clear file selection
  clearAvatarSelection();
};

const uploadAvatar = async () => {
  if (!selectedFile.value) {
    return null; // 没有选择新文件，返回null表示不更新头像
  }
  
  uploading.value = true;
  
  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    
    const response = await axios.post(`/api/users/me/avatar`, formData, {
      headers: {
        Authorization: `Bearer ${props.token}`
        // 不要手动设置 Content-Type，让 axios 自动处理 FormData 的 boundary
      }
    });
    
    if (response.data.success) {
      console.log('Avatar uploaded successfully:', response.data.url);
      return response.data.url;
    } else {
      throw new Error(response.data.message || t('myProfile.uploadFailed'));
    }
  } catch (err) {
    console.error('Failed to upload avatar:', err);
    alert(err.response?.data?.message || err.message || t('myProfile.uploadFailed'));
    throw err;
  } finally {
    uploading.value = false;
  }
};

const saveProfile = async () => {
  saving.value = true;
  
  try {
    // 如果选择了新头像，先上传
    let avatarUrl = null;
    if (selectedFile.value) {
      try {
        avatarUrl = await uploadAvatar();
      } catch (err) {
        // 上传失败，不继续保存
        saving.value = false;
        return;
      }
    }
    
    // 准备更新数据
    const updateData = {
      displayName: editForm.value.displayName,
      preferredLanguage: editForm.value.preferredLanguage
    };
    
    // 如果有新头像URL，添加到更新数据中
    if (avatarUrl) {
      updateData.avatar = avatarUrl;
    }
    
    const response = await axios.put(`/api/users/me`, updateData, {
      headers: { Authorization: `Bearer ${props.token}` }
    });
    
    if (response.data.success) {
      // Update local profile
      profile.value = response.data.data;
      
      // 清除文件选择
      clearAvatarSelection();
      
      // Update language if changed
      if (editForm.value.preferredLanguage !== lang.value) {
        setLanguage(editForm.value.preferredLanguage);
        lang.value = editForm.value.preferredLanguage;
        // Trigger language change event
        window.dispatchEvent(new CustomEvent('languageChanged', { 
          detail: { lang: editForm.value.preferredLanguage } 
        }));
      }
      
      // Update localStorage user data
      const savedUser = localStorage.getItem('user');
      if (savedUser) {
        const userData = JSON.parse(savedUser);
        // Keep username and displayName in sync for the logged-in user
        userData.username = profile.value.username;
        userData.displayName = profile.value.displayName || profile.value.username;
        userData.avatar = profile.value.avatar;
        userData.preferredLanguage = profile.value.preferredLanguage;
        localStorage.setItem('user', JSON.stringify(userData));
      }
      
      isEditing.value = false;
      
      // Show success message
      alert(t('myProfile.updateSuccess'));
    } else {
      alert(response.data.message || t('myProfile.updateFailed'));
    }
  } catch (err) {
    console.error('Failed to update profile:', err);
    alert(err.response?.data?.message || t('myProfile.updateFailed'));
  } finally {
    saving.value = false;
  }
};

const viewMyPosts = () => {
  emit('viewMyPosts');
};

const viewPostDetail = (postId) => {
  emit('viewPostDetail', postId);
};

const viewMyReports = () => {
  emit('viewMyReports');
};

const getStatusClass = (status) => {
  const statusMap = {
    'PENDING_REVIEW': 'status-pending',
    'APPROVED': 'status-approved',
    'REJECTED': 'status-rejected'
  };
  return statusMap[status] || 'status-unknown';
};

const getStatusText = (status) => {
  const statusMap = {
    'PENDING_REVIEW': lang.value === 'zh' ? '待审核' : 'Pending Review',
    'APPROVED': lang.value === 'zh' ? '已发布' : 'Published',
    'REJECTED': lang.value === 'zh' ? '已拒绝' : 'Rejected'
  };
  return statusMap[status] || status;
};

const formatTime = (timestamp) => {
  if (!timestamp) {
    return lang.value === 'zh' ? '未知时间' : 'Unknown time';
  }
  
  const date = parseBackendDate(timestamp);
  if (!date) {
    console.warn('Invalid timestamp:', timestamp);
    return lang.value === 'zh' ? '无效时间' : 'Invalid time';
  }
  
  // 使用 UTC 时间戳计算时间差（时间戳是时区无关的）
  const nowUtc = Date.now();
  const dateUtc = date.getTime();
  const diffMs = nowUtc - dateUtc;
  
  // 如果时间在未来（可能是数据问题），显示曼谷时间
  if (diffMs < 0) {
    return formatBangkokAbsolute(date) || (lang.value === 'zh' ? '无效时间' : 'Invalid time');
  }
  
  // 计算时间差
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  
  // 显示相对时间
  if (diffMins < 1) return lang.value === 'zh' ? '刚刚' : 'Just now';
  if (diffMins < 60) return `${diffMins}${lang.value === 'zh' ? '分钟前' : 'm ago'}`;
  if (diffHours < 24) return `${diffHours}${lang.value === 'zh' ? '小时前' : 'h ago'}`;
  if (diffDays < 7) return `${diffDays}${lang.value === 'zh' ? '天前' : 'd ago'}`;
  
  // 超过7天，显示曼谷时间的日期和时间
  return formatBangkokAbsolute(date) || (lang.value === 'zh' ? '无效时间' : 'Invalid time');
};

onMounted(() => {
  console.log('MyProfile component mounted');
  console.log('Props:', { userId: props.userId, hasToken: !!props.token });
  
  if (!props.token) {
    console.error('No token provided to MyProfile component');
    error.value = t('myProfile.unauthorized');
    loading.value = false;
    return;
  }
  
  loadProfile();
  
  // Listen for language changes
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      lang.value = e.detail.lang;
      loadMyPosts();
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
.my-profile {
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
  border-top: 4px solid var(--color-primary);
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
  position: relative;
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

.btn-edit-avatar {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-primary);
  border: 2px solid white;
  color: white;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.btn-edit-avatar:hover {
  background: var(--color-primary-dark);
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

.edit-form {
  margin-bottom: 24px;
  padding: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #303133;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #303133;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

.form-hint {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-primary-dark);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-link {
  background: transparent;
  color: var(--color-primary);
  text-decoration: underline;
  padding: 8px 0;
}

.btn-secondary {
  background: #909399;
  color: white;
}

.btn-secondary:hover {
  background: #a6a9ad;
}

.avatar-upload-section {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.avatar-preview-container {
  position: relative;
  flex-shrink: 0;
}

.avatar-preview {
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
  border: 2px solid #dcdfe6;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

.spinner-small {
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  animation: spin 1s linear infinite;
  margin-bottom: 8px;
}

.avatar-upload-controls {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-input {
  display: none;
}

.avatar-upload-controls label {
  display: inline-block;
  cursor: pointer;
  margin-bottom: 0;
}

.posts-section {
  margin-top: 24px;
}

.posts-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-preview-card {
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s;
}

.post-preview-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.post-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: start;
  margin-bottom: 8px;
}

.post-preview-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-approved {
  background: #d4edda;
  color: #155724;
}

.status-rejected {
  background: #f8d7da;
  color: #721c24;
}

.post-preview-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.post-preview-time {
  font-size: 12px;
  color: #909399;
}

.empty-posts {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

@media (max-width: 768px) {
  .my-profile {
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

