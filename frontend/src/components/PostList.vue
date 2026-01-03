<template>
  <section style="margin-bottom: 1.5rem">
    <div style="display: flex; gap: 0.5rem; margin-bottom: 1rem">
      <input
        class="input"
        style="flex: 1"
        :placeholder="t('postList.searchPlaceholder')"
        v-model="query"
        @keydown.enter="loadPosts"
      />
      <button class="btn btn-primary" @click="loadPosts">
        {{ t('postList.search') }}
      </button>
    </div>

    <p v-if="loading">{{ t('postList.loading') }}</p>
    <div
      v-else-if="error"
      class="error-message"
      style="padding: 1rem; background: #fee; border: 1px solid #fcc; border-radius: 8px; color: #c33;"
    >
      <p style="margin: 0 0 0.5rem 0; font-weight: 600;">{{ error }}</p>
      <button class="btn btn-primary" @click="loadPosts" style="font-size: 0.875rem;">
        {{ t('common.retry') || '重试' }}
      </button>
    </div>
    <!-- 社区帖子列表：竖向单列布局 -->
    <div v-else class="post-list-vertical">
      <article
        v-for="post in filteredPosts"
        :key="post.id"
        class="post-card"
        style="cursor: pointer"
        @click="onPostClick && onPostClick(post.id)"
      >
        <div class="post-header">
          <div class="post-author">
            <div 
              class="post-avatar"
              :style="{
                backgroundImage: post.authorAvatar ? `url(${post.authorAvatar})` : 'none',
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                cursor: 'pointer'
              }"
              @click.stop="handleViewAuthorProfile(post.authorId)"
            >
              <span v-if="!post.authorAvatar" style="font-size: 18px; color: white; font-weight: bold;">
                {{ (post.authorName || 'U')[0].toUpperCase() }}
              </span>
            </div>
            <div>
              <div 
                class="post-author-name"
                style="cursor: pointer"
                @click.stop="handleViewAuthorProfile(post.authorId)"
              >{{ post.authorName || post.authorId || t('postList.anonymous') }}</div>
              <span class="post-author-badge">{{ t('postList.student') }}</span>
            </div>
          </div>
          <span class="post-tag" v-if="parseTags(post.tags).length > 0">
            {{ getTagEmoji(post.tags) }} #{{ formatTag(parseTags(post.tags)[0]) }}
          </span>
        </div>
        <h3 class="post-title">{{ post.title }}</h3>
        <div v-if="post.imageUrl" style="margin-bottom: 0.5rem">
          <img
            :src="post.imageUrl"
            :alt="post.title || 'post image'"
            style="width: 100%; max-height: 180px; object-fit: cover; border-radius: 8px; border: 1px solid #e5e7eb"
          />
        </div>
        <p v-if="post.score != null" style="color: #7c3aed; font-size: 0.75rem; margin-bottom: 4px">
          {{ t('postList.semanticScore') }}: {{ post.score.toFixed(2) }}
        </p>
        <p class="post-body">{{ post.body }}</p>
        <div class="post-footer">
          <span>{{ formatTime(post.createdAt) }}</span>
          <div class="post-stats">
            <span>❤️ {{ post.likeCount || 0 }}</span>
            <span>💬 {{ post.commentCount || 0 }}</span>
          </div>
        </div>
      </article>
      
      <!-- Loading more indicator -->
      <div v-if="loadingMore" style="text-align: center; padding: 2rem; color: #666;">
        {{ t('postList.loading') }}
      </div>
      
      <!-- No more posts indicator -->
      <div v-if="!hasMore && posts.length > 0" style="text-align: center; padding: 2rem; color: #999; font-size: 0.875rem;">
        {{ lang === 'zh' ? '没有更多帖子了' : 'No more posts' }}
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { fetchPosts } from '../api';
import { getLanguagePreference } from '../utils/language';
import { t, getCurrentLanguage, setLanguage } from '../i18n';

const props = defineProps({
  onPostClick: {
    type: Function,
    default: null
  },
  selectedTag: {
    type: String,
    default: 'all'
  }
});

const posts = ref([]);
const query = ref('');
const loading = ref(false);
const error = ref(null);
const lang = ref(getCurrentLanguage());
const page = ref(0);
const size = ref(20);
const hasMore = ref(true);
const loadingMore = ref(false);

const loadPosts = async (reset = false) => {
  if (reset) {
    page.value = 0;
    posts.value = [];
    hasMore.value = true;
  }
  
  if (!hasMore.value && !reset) return;
  
  loading.value = reset;
  loadingMore.value = !reset;
  error.value = null;
  
  try {
    const savedLang = localStorage.getItem('userLanguage');
    const currentLang = getCurrentLanguage();
    const langToUse = lang.value || savedLang || currentLang || 'en';
    
    console.log('PostList: Loading posts, page:', page.value);
    
    const result = await fetchPosts({ 
      q: query.value || undefined, 
      lang: langToUse,
      page: page.value,
      size: size.value
    });
    console.log('PostList: Posts loaded:', result?.length || 0, 'posts');
    
    if (reset) {
      posts.value = result || [];
    } else {
      posts.value = [...posts.value, ...(result || [])];
    }
    
    // Check if there are more posts
    hasMore.value = (result || []).length === size.value;
    if (hasMore.value) {
      page.value++;
    }
    
    error.value = null;
  } catch (err) {
    console.error('Failed to load posts:', err);
    if (reset) {
      posts.value = [];
    }
    
    // 设置用户友好的错误消息
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      error.value = err.userMessage || (lang.value === 'zh' 
        ? '请求超时，请检查网络连接或稍后重试。如果后端服务器未运行，请先启动后端服务。'
        : 'Request timeout. Please check your network connection or try again later. If the backend server is not running, please start it first.');
    } else if (err.code === 'ERR_NETWORK' || err.message?.includes('Network Error')) {
      error.value = lang.value === 'zh'
        ? '网络错误，无法连接到服务器。请检查后端服务器是否运行在 http://localhost:8080'
        : 'Network error. Cannot connect to server. Please check if the backend server is running at http://localhost:8080';
    } else {
      error.value = err.userMessage || err.message || (lang.value === 'zh'
        ? '加载帖子失败，请稍后重试'
        : 'Failed to load posts, please try again later');
    }
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

// Infinite scroll handler
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const windowHeight = window.innerHeight;
  const documentHeight = document.documentElement.scrollHeight;
  
  // Load more when near bottom (100px from bottom)
  if (scrollTop + windowHeight >= documentHeight - 100 && hasMore.value && !loadingMore.value) {
    loadPosts(false);
  }
};

const parseTags = (tags) => {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  if (typeof tags === 'string') {
    if (tags.startsWith('[')) {
      try {
        return JSON.parse(tags);
      } catch (e) {
        // Parsing failed, treat as comma-separated
      }
    }
    return tags.split(',').map(t => t.trim());
  }
  return [];
};

const getTagEmoji = (tags) => {
  const tagList = parseTags(tags);
  // Map to standard tags: Study, Housing, Travel, Part-time Job, Life Services
  if (tagList.some(t => {
    const lower = t.toLowerCase();
    return lower === 'study' || ['learning', 'course', 'class', 'education'].includes(lower);
  })) return '📚';
  if (tagList.some(t => {
    const lower = t.toLowerCase();
    return lower === 'housing' || ['rent', 'rental', 'accommodation', 'apartment'].includes(lower);
  })) return '🏠';
  if (tagList.some(t => {
    const lower = t.toLowerCase();
    return lower === 'travel' || ['tourism', 'trip', 'visa', 'studyabroad'].includes(lower);
  })) return '✈️';
  if (tagList.some(t => {
    const lower = t.toLowerCase();
    return lower === 'part-time job' || lower === 'parttime job' || ['part-time', 'parttime', 'job', 'work', 'employment'].includes(lower);
  })) return '💼';
  if (tagList.some(t => {
    const lower = t.toLowerCase();
    return lower === 'life services' || ['life', 'service', 'services', 'food', 'lifestyle', 'market', 'secondhand'].includes(lower);
  })) return '🛒';
  return '📝';
};

const formatTag = (tag) => {
  if (!tag) return 'Post';
  // Map to standard tags: Study, Housing, Travel, Part-time Job, Life Services
  const lower = tag.toLowerCase();
  // Direct match for standard tags
  if (lower === 'study') return 'Study';
  if (lower === 'housing') return 'Housing';
  if (lower === 'travel') return 'Travel';
  if (lower === 'part-time job' || lower === 'parttime job') return 'Part-time Job';
  if (lower === 'life services') return 'Life Services';
  
  // Map common variations to standard tags
  if (['learning', 'course', 'class', 'education'].includes(lower)) return 'Study';
  if (['rent', 'rental', 'accommodation', 'apartment'].includes(lower)) return 'Housing';
  if (['tourism', 'trip', 'visa', 'studyabroad'].includes(lower)) return 'Travel';
  if (['part-time', 'parttime', 'job', 'work', 'employment'].includes(lower)) return 'Part-time Job';
  if (['life', 'service', 'services', 'food', 'lifestyle', 'market', 'secondhand'].includes(lower)) return 'Life Services';
  
  // If no match, return the original tag (but should be one of the 5 standard tags)
  return tag;
};

const handleViewAuthorProfile = (authorId) => {
  if (authorId && props.onAuthorClick) {
    props.onAuthorClick(authorId);
  }
};

const formatTime = (timestamp) => {
  if (!timestamp) {
    return lang.value === 'zh' ? '刚刚' : 'Just now';
  }
  
  let date;
  try {
    date = new Date(timestamp);
    if (isNaN(date.getTime()) || date.getTime() < 0 || date.getFullYear() < 1971) {
      date = new Date();
    }
  } catch (e) {
    date = new Date();
  }
  
  // 时间差计算（时间戳差值与时区无关）
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);
  
  if (diffMins < 1) return lang.value === 'zh' ? '刚刚' : 'Just now';
  if (diffMins < 60) return `${diffMins}${lang.value === 'zh' ? '分钟前' : 'm ago'}`;
  if (diffHours < 24) return `${diffHours}${lang.value === 'zh' ? '小时前' : 'h ago'}`;
  if (diffDays < 7) return `${diffDays}${lang.value === 'zh' ? '天前' : 'd ago'}`;
  
  // 使用泰国时区格式化日期显示
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Bangkok',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
  const parts = formatter.formatToParts(date);
  const year = parts.find(p => p.type === 'year').value;
  const month = parts.find(p => p.type === 'month').value;
  const day = parts.find(p => p.type === 'day').value;
  return `${day}-${month}-${year}`;
};

const filteredPosts = computed(() => {
  if (props.selectedTag === 'all') {
    return posts.value;
  }
  return posts.value.filter((post) => {
    const tagList = parseTags(post.tags).map(t => t.toLowerCase());
    return tagList.includes(props.selectedTag.toLowerCase());
  });
});

onMounted(() => {
  const savedLang = localStorage.getItem('userLanguage');
  console.log('PostList: Initial load, localStorage userLanguage:', savedLang);
  
  const currentLang = getCurrentLanguage();
  console.log('PostList: Initial load, currentLang from i18n:', currentLang);
  
  const langToSet = (savedLang === 'zh' || savedLang === 'en') ? savedLang : (currentLang || 'en');
  console.log('PostList: Setting lang state to:', langToSet);
  lang.value = langToSet;
  
  if (savedLang && savedLang !== currentLang && (savedLang === 'zh' || savedLang === 'en')) {
    setLanguage(savedLang);
  }
  
  loadPosts(true);
  
  // Add scroll listener for infinite scroll
  window.addEventListener('scroll', handleScroll);
  
  return () => {
    window.removeEventListener('scroll', handleScroll);
  };
});

// Listen for language changes
onMounted(() => {
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      const newLang = e.detail.lang;
      lang.value = newLang;
      setTimeout(() => {
        loadPosts();
      }, 100);
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

// Reload posts when language changes
watch(lang, (newLang) => {
  if (newLang) {
    console.log('PostList: Language changed to:', newLang, 'reloading posts...');
    loadPosts(true);
  }
});

// Reload posts when search query changes
watch(query, () => {
  loadPosts(true);
});
</script>

<style scoped>
/* Styles are in styles.css */
</style>

