<template>
  <section style="margin-bottom: 1.5rem; padding: 0 32px">
    <div style="display: flex; gap: 12px; margin-bottom: 24px; position: relative">
      <div style="flex: 1; position: relative">
        <span style="position: absolute; left: 16px; top: 50%; transform: translateY(-50%); color: #999; font-size: 16px;">🔍</span>
        <input
          class="input"
          style="flex: 1; padding-left: 44px"
          :placeholder="t('postList.searchPlaceholder')"
          v-model="query"
          @keydown.enter="loadPosts(true)"
        />
      </div>
      <button class="btn btn-primary" @click="loadPosts(true)" style="min-width: 100px">
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
      <button class="btn btn-primary" @click="loadPosts(true)" style="font-size: 0.875rem;">
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
            </div>
          </div>
          <span class="post-tag" v-if="parseTags(post.tags).length > 0">
            {{ getTagEmoji(post.tags) }} #{{ formatTag(parseTags(post.tags)[0]) }}
          </span>
        </div>
        <h3 class="post-title">{{ post.title }}</h3>
        <div v-if="post.imageUrl" style="margin-bottom: 1rem; position: relative; padding: 8px; background: linear-gradient(135deg, rgba(182, 156, 255, 0.12), rgba(52, 152, 219, 0.10)); border-radius: 20px; transform: rotate(-1deg); transition: transform 0.3s;">
          <div style="position: absolute; bottom: -8px; right: -8px; width: 100%; height: 100%; background: linear-gradient(135deg, rgba(182, 156, 255, 0.22), rgba(52, 152, 219, 0.18)); border-radius: 20px; z-index: -1; transform: rotate(2deg);"></div>
          <img
            :src="post.imageUrl"
            :alt="post.title || 'post image'"
            style="width: 100%; max-height: 200px; object-fit: cover; border-radius: 16px; border: 3px solid #fff; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15); display: block;"
          />
        </div>
        <p v-if="post.score != null" style="color: #7c3aed; font-size: 0.75rem; margin-bottom: 4px">
          {{ t('postList.semanticScore') }}: {{ post.score.toFixed(2) }}
        </p>
        <p class="post-body">{{ post.body }}</p>
        <div class="post-footer">
          <span>{{ formatTime(post.createdAt) }}</span>
          <div class="post-stats">
            <span class="stat-item" style="display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; background: rgba(182, 156, 255, 0.14); border-radius: 20px; font-weight: 600; color: var(--color-primary);">
              <span style="font-size: 18px;">❤️</span>
              {{ post.likeCount || 0 }}
            </span>
            <span class="stat-item" style="display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; background: rgba(52, 152, 219, 0.1); border-radius: 20px; font-weight: 600; color: #3498DB;">
              <span style="font-size: 18px;">💬</span>
              {{ post.commentCount || 0 }}
            </span>
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
import { parseBackendDate, formatBangkokAbsolute } from '../utils/datetime';

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
// 用于避免并发请求导致的重复数据：每次请求递增 ID，旧请求返回时直接丢弃
const currentRequestId = ref(0);

const loadPosts = async (reset = false) => {
  const thisRequestId = ++currentRequestId.value;

  if (reset) {
    page.value = 0;
    posts.value = [];
    hasMore.value = true;
  }
  
  // 如果没有更多数据并且不是重置，就不再加载
  if (!hasMore.value && !reset) return;

  // 防止在已有请求进行中时再次触发“加载更多”，造成并发请求同一页
  if (!reset && (loading.value || loadingMore.value)) return;
  
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

    // 如果在本次请求期间又发起了更新的请求，则忽略本次结果，避免旧数据覆盖新数据 / 造成重复
    if (thisRequestId !== currentRequestId.value) {
      return;
    }
    
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
    // 只在这是最近一次请求时，才重置 loading 状态，避免覆盖更新请求的状态
    if (thisRequestId === currentRequestId.value) {
      loading.value = false;
      loadingMore.value = false;
    }
  }
};

// Infinite scroll handler
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const windowHeight = window.innerHeight;
  const documentHeight = document.documentElement.scrollHeight;
  
  // Load more when near bottom (100px from bottom)
  // 额外判断 loading，避免在已有请求时再次触发导致同一页数据被重复追加
  if (scrollTop + windowHeight >= documentHeight - 100 && hasMore.value && !loadingMore.value && !loading.value) {
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
      // 只更新语言，真正的重新加载由 watch(lang) 统一处理，避免重复追加数据
      lang.value = newLang;
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

