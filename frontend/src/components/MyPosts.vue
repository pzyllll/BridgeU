<template>
  <div class="my-posts">
    <div class="header-section">
      <button class="btn btn-back" @click="$emit('back')">
        ← {{ t('common.back') }}
      </button>
      <h1 class="page-title">{{ t('myPosts.title') }}</h1>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>{{ t('myPosts.loading') }}</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <div class="error-message">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="loadPosts">
          {{ t('common.retry') }}
        </button>
      </div>
    </div>

    <!-- Posts List -->
    <div v-else-if="posts && posts.length > 0" class="posts-list">
      <div 
        v-for="post in posts" 
        :key="post.id"
        class="post-card"
        @click="viewPostDetail(post.id)"
      >
        <div class="post-header">
          <h3 class="post-title">{{ post.title }}</h3>
          <div class="post-header-actions">
            <span :class="['status-badge', getStatusClass(post.status)]">
              {{ getStatusText(post.status) }}
            </span>
            <button
              class="btn btn-delete"
              @click.stop="confirmDelete(post)"
            >
              {{ deletingPostId === post.id ? t('myPosts.deleting') : t('myPosts.delete') }}
            </button>
          </div>
        </div>
        
        <div class="post-body-preview">
          {{ truncateText(post.body, 200) }}
        </div>

        <div class="post-footer">
          <div class="post-meta">
            <span class="post-time">{{ formatTime(post.createdAt) }}</span>
            <span v-if="post.reviewedAt" class="post-reviewed">
              {{ t('myPosts.reviewedAt') }}: {{ formatTime(post.reviewedAt) }}
            </span>
          </div>
          <div class="post-stats">
            <span>❤️ {{ post.likeCount || 0 }}</span>
            <span>💬 {{ post.commentCount || 0 }}</span>
          </div>
        </div>

        <!-- Review Note (if rejected) -->
        <div v-if="post.status === 'REJECTED' && post.reviewNote" class="review-note">
          <strong>{{ t('myPosts.reviewNote') }}:</strong>
          <p>{{ post.reviewNote }}</p>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-posts">
      <p>{{ t('myPosts.noPosts') }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { getCurrentLanguage, t } from '../i18n';
import { parseBackendDate, formatBangkokAbsolute } from '../utils/datetime';

const props = defineProps({
  token: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['back', 'viewPostDetail']);

const posts = ref([]);
const loading = ref(true);
const error = ref(null);
const lang = ref(getCurrentLanguage());
const deletingPostId = ref(null);

const loadPosts = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    const response = await axios.get(`/api/users/me/posts`, {
      headers: { Authorization: `Bearer ${props.token}` },
      params: { lang: lang.value }
    });
    
    if (response.data.success) {
      posts.value = response.data.data || [];
    } else {
      error.value = response.data.message || t('myPosts.loadFailed');
    }
  } catch (err) {
    console.error('Failed to load my posts:', err);
    if (err.response && err.response.status === 401) {
      error.value = t('myPosts.unauthorized');
    } else {
      error.value = err.response?.data?.message || err.message || t('myPosts.loadFailed');
    }
  } finally {
    loading.value = false;
  }
};

const confirmDelete = (post) => {
  const message = t('myPosts.confirmDelete');
  if (window.confirm(message)) {
    deletePost(post.id);
  }
};

const deletePost = async (postId) => {
  if (deletingPostId.value) {
    return;
  }

  deletingPostId.value = postId;

  try {
    const response = await axios.delete(`/api/posts/${postId}`, {
      headers: { Authorization: `Bearer ${props.token}` },
    });

    if (response.data && response.data.success) {
      posts.value = posts.value.filter((p) => p.id !== postId);
      alert(t('myPosts.deleted'));
    } else {
      alert(response.data?.message || t('myPosts.deleteFailed'));
    }
  } catch (err) {
    console.error('Failed to delete post:', err);
    alert(err.response?.data?.message || t('myPosts.deleteFailed'));
  } finally {
    deletingPostId.value = null;
  }
};

const viewPostDetail = (postId) => {
  emit('viewPostDetail', postId);
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

const truncateText = (text, maxLength) => {
  if (!text) return '';
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

onMounted(() => {
  loadPosts();
  
  // Listen for language changes
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      lang.value = e.detail.lang;
      loadPosts();
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
.my-posts {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.header-section {
  margin-bottom: 24px;
}

.btn-back {
  background: transparent;
  border: none;
  color: var(--color-primary);
  cursor: pointer;
  font-size: 14px;
  margin-bottom: 16px;
  padding: 8px 0;
}

.btn-back:hover {
  text-decoration: underline;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
  margin: 0;
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
  align-items: start;
  margin-bottom: 12px;
}

.post-title {
  margin: 0;
  font-size: 20px;
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
  margin-left: 12px;
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

.post-body-preview {
  margin: 12px 0;
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

.post-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.post-time {
  font-size: 14px;
  color: #909399;
}

.post-reviewed {
  font-size: 12px;
  color: #c0c4cc;
}

.post-stats {
  display: flex;
  gap: 16px;
}

.review-note {
  margin-top: 12px;
  padding: 12px;
  background: #fef2f2;
  border-left: 4px solid #f87171;
  border-radius: 4px;
}

.review-note strong {
  color: #991b1b;
  display: block;
  margin-bottom: 4px;
}

.review-note p {
  margin: 0;
  color: #7f1d1d;
  font-size: 14px;
}

.empty-posts {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 999px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: var(--color-primary);
  color: white;
}

.btn-delete {
  background: #a855f7; /* 紫色按钮 */
  border: 1px solid #a855f7;
  color: #ffffff;
  border-radius: 999px;
  padding: 8px 18px;
  font-weight: 500;
}

.btn-delete:hover {
  background: #9333ea;
  border-color: #9333ea;
  box-shadow: 0 8px 24px rgba(148, 63, 255, 0.35);
  transform: translateY(-1px);
}

.btn-primary:hover {
  background: var(--color-primary-dark);
}

@media (max-width: 768px) {
  .my-posts {
    padding: 16px;
  }

  .post-header {
    flex-direction: column;
    gap: 8px;
  }

  .status-badge {
    margin-left: 0;
    align-self: flex-start;
  }
}
</style>

