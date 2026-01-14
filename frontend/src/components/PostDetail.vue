<template>
  <section v-if="loading" class="card">
    <p>{{ t('postDetail.loading') }}</p>
  </section>
  <section v-else-if="error || !postDetail" class="card">
    <p style="color: #dc2626">{{ error || t('postDetail.notFound') }}</p>
    <button v-if="onBack" class="btn" @click="onBack">
      {{ t('postDetail.back') }}
    </button>
  </section>
  <section v-else class="card">
    <div v-if="onBack" style="margin-bottom: 1rem">
      <button class="btn" @click="onBack" style="margin-bottom: 1rem">
        ← {{ t('postDetail.back') }}
      </button>
    </div>

    <!-- Post Header -->
    <div class="post-header" style="margin-bottom: 1rem; padding-bottom: 1rem; border-bottom: 1px solid #e5e7eb">
      <div class="post-author" style="display: flex; align-items: center; gap: 1rem">
        <div 
          class="post-avatar" 
          :style="{
            backgroundImage: postDetail.authorAvatar ? `url(${postDetail.authorAvatar})` : 'none',
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            cursor: 'pointer'
          }"
          @click="handleViewAuthorProfile"
        >
          <span v-if="!postDetail.authorAvatar" style="font-size: 20px; color: white; font-weight: bold;">
            {{ (postDetail.authorDisplayName || postDetail.authorName || 'U')[0].toUpperCase() }}
          </span>
        </div>
        <div style="flex: 1">
          <div style="display: flex; align-items: center; gap: 0.5rem">
            <div 
              class="post-author-name"
              style="cursor: pointer"
              @click="handleViewAuthorProfile"
            >{{ postDetail.authorDisplayName || postDetail.authorName || t('postList.anonymous') }}</div>
            <span class="post-author-badge">{{ t('postList.student') }}</span>
          </div>
          <div 
            style="font-size: 0.875rem; color: #666; margin-top: 0.25rem; cursor: pointer"
            @click="handleViewAuthorProfile"
          >
            @{{ postDetail.authorName }}
          </div>
        </div>
        <button
          v-if="currentUserId && postDetail.authorId && currentUserId !== postDetail.authorId"
          :class="['btn', postDetail.isFollowing ? '' : 'btn-primary']"
          @click="handleToggleFollow"
          :disabled="isTogglingFollow"
          style="padding: 0.5rem 1rem; font-size: 0.875rem"
        >
          {{ isTogglingFollow ? t('common.loading') : (postDetail.isFollowing ? t('postDetail.following') : t('postDetail.follow')) }}
        </button>
      </div>
    </div>

    <!-- Post Content -->
    <h1 class="post-title" style="
      font-size: 1.5rem;
      margin-bottom: 1.5rem;
      font-weight: bold;
      line-height: 1.4;
      word-break: break-word
    ">
      {{ post?.title || t('postDetail.noTitle') }}
    </h1>
    <div v-if="post?.imageUrl" style="margin-bottom: 1.5rem">
      <img
        :src="post.imageUrl"
        :alt="post.title || 'post image'"
        style="width: 100%; max-height: 360px; object-fit: cover; border-radius: 12px; border: 1px solid #e5e7eb"
      />
    </div>
    <div class="post-body" style="
      margin-bottom: 0;
      padding-bottom: 1rem;
      line-height: 1.8;
      word-break: break-word;
      overflow: visible;
      font-size: 0.9375rem;
      color: #374151;
      position: relative;
      z-index: 0;
      display: block;
      width: 100%;
      clear: both
    ">
      <div v-if="!post?.body || !post.body.trim()" style="
        color: #666;
        font-style: italic;
        padding: 1.5rem;
        background: #f9fafb;
        border-radius: 8px;
        text-align: center;
        border: 2px dashed #d1d5db
      ">
        {{ t('postDetail.noContent') }}
      </div>
      <div v-else v-html="formatContent(post.body)"></div>
    </div>

    <!-- Post Actions -->
    <div style="
      display: flex;
      gap: 1rem;
      padding: 1rem 0;
      border-top: 2px dashed #e5e7eb;
      border-bottom: 2px dashed #e5e7eb;
      margin-top: 2rem;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
      align-items: center;
      position: relative;
      z-index: 2;
      clear: both;
      width: 100%;
      box-sizing: border-box
    ">
      <button
        class="btn"
        @click="handleToggleLike"
        :style="{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          padding: '0.5rem 1rem',
          background: postDetail.isLiked ? '#fee2e2' : 'transparent',
          color: postDetail.isLiked ? '#dc2626' : 'inherit'
        }"
      >
        <span>{{ postDetail.isLiked ? '❤️' : '🤍' }}</span>
        <span>{{ postDetail.likeCount || 0 }}</span>
      </button>
      <div style="display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 1rem">
        <span>💬</span>
        <span>{{ postDetail.commentCount || 0 }}</span>
      </div>
      <div style="
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 1rem;
        color: #666;
        font-size: 0.875rem;
        margin-left: auto
      ">
        <span>{{ formatDate(post?.createdAt) }}</span>
      </div>
      <button
        v-if="token"
        class="btn"
        @click="showReportDialog = true"
        style="
          padding: 0.5rem 1rem;
          font-size: 0.875rem;
          background: #fee2e2;
          color: #dc2626;
          border: 1px solid #fecaca;
        "
      >
        🚩 {{ t('postDetail.report') }}
      </button>
    </div>

    <!-- Comment Form -->
    <div v-if="token" style="
      margin-top: 1.5rem;
      margin-bottom: 2rem;
      padding: 1.5rem;
      background: #f9fafb;
      border-radius: 8px;
      border: 2px solid #e5e7eb;
      position: relative;
      z-index: 2;
      clear: both
    ">
      <h3 style="
        margin-bottom: 1rem;
        font-size: 1rem;
        font-weight: bold
      ">
        {{ t('postDetail.addComment') }}
      </h3>
      <textarea
        class="input"
        v-model="commentContent"
        :placeholder="t('postDetail.commentPlaceholder')"
        rows="4"
        style="
          width: 100%;
          margin-bottom: 0.75rem;
          min-height: 100px;
          resize: vertical
        "
      />
      <button
        class="btn btn-primary"
        @click="handleAddComment"
        :disabled="submittingComment || !commentContent.trim()"
        style="min-width: 100px"
      >
        {{ submittingComment ? t('postDetail.submitting') : t('postDetail.submitComment') }}
      </button>
    </div>

    <!-- Comments List -->
    <div>
      <div style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
        padding-bottom: 0.5rem;
        border-bottom: 2px solid #e5e7eb
      ">
        <h3 style="
          font-size: 1.125rem;
          font-weight: bold;
          margin: 0
        ">
          {{ t('postDetail.comments') }} ({{ postDetail.comments?.length || 0 }})
        </h3>
        <button
          v-if="postDetail.comments && postDetail.comments.length > 0"
          class="btn"
          @click="handleGenerateSummary"
          :disabled="loadingSummary"
          style="
            padding: 0.5rem 1rem;
            font-size: 0.875rem;
            background: #f3f4f6;
            border: 1px solid #d1d5db;
            display: flex;
            align-items: center;
            gap: 0.5rem
          "
        >
          <span v-if="loadingSummary">⏳</span>
          <span v-else>🤖</span>
          <span>{{ loadingSummary ? t('postDetail.summarizing') : t('postDetail.summarizeComments') }}</span>
        </button>
      </div>
      
      <!-- Comment Summary Display -->
      <div
        v-if="commentSummary"
        style="
          margin-bottom: 1.5rem;
          padding: 1rem;
          background: #f0f9ff;
          border: 2px solid #3b82f6;
          border-radius: 8px;
          border-left: 4px solid #3b82f6
        "
      >
        <div style="
          display: flex;
          align-items: center;
          gap: 0.5rem;
          margin-bottom: 0.75rem;
          font-weight: bold;
          color: #1e40af;
          font-size: 0.875rem
        ">
          <span>📝</span>
          <span>{{ t('postDetail.commentSummary') }}</span>
          <span style="font-weight: normal; color: #666; font-size: 0.75rem">
            ({{ summaryCommentCount }} {{ t('postDetail.comments') }})
          </span>
        </div>
        <div style="
          line-height: 1.8;
          color: #1e3a8a;
          white-space: pre-wrap;
          word-break: break-word
        ">
          {{ commentSummary }}
        </div>
      </div>
      <div v-if="postDetail.comments && postDetail.comments.length > 0" style="display: flex; flex-direction: column; gap: 1rem">
        <div
          v-for="comment in postDetail.comments"
          :key="comment.id"
          style="
            padding: 1rem;
            background: #fff;
            border-radius: 8px;
            border: 2px solid #e5e7eb;
            transition: all 0.2s
          "
        >
          <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem">
            <div 
              class="post-avatar" 
              style="width: 32px; height: 32px"
              :style="{
                backgroundImage: comment.authorAvatar ? `url(${comment.authorAvatar})` : 'none',
                backgroundSize: 'cover',
                backgroundPosition: 'center'
              }"
            >
              <span v-if="!comment.authorAvatar" style="font-size: 12px; color: white; font-weight: bold;">
                {{ (comment.authorDisplayName || comment.authorName || 'U')[0].toUpperCase() }}
              </span>
            </div>
            <div>
              <div class="post-author-name" style="font-size: 0.875rem">
                {{ comment.authorDisplayName || comment.authorName || t('postList.anonymous') }}
              </div>
              <div style="font-size: 0.75rem; color: #666">
                {{ formatDate(comment.createdAt) }}
              </div>
            </div>
          </div>
          <div style="display: flex; justify-content: space-between; align-items: flex-start">
            <div style="margin-left: 2.5rem; line-height: 1.6; flex: 1">
            {{ comment.content }}
            </div>
            <div style="display: flex; gap: 0.5rem; margin-left: 1rem">
              <button
                v-if="token && comment.authorId === currentUserId"
                class="btn"
                @click="handleDeleteComment(comment.id)"
                :disabled="deletingCommentId === comment.id"
                style="
                  padding: 0.25rem 0.5rem;
                  font-size: 0.75rem;
                  background: #fee2e2;
                  color: #dc2626;
                  border: 1px solid #fecaca;
                "
              >
                {{ deletingCommentId === comment.id ? t('postDetail.deleting') : t('postDetail.delete') }}
              </button>
              <button
                v-if="token"
                class="btn"
                @click="openCommentReportDialog(comment.id)"
                style="
                  padding: 0.25rem 0.5rem;
                  font-size: 0.75rem;
                  background: #f3f4f6;
                  color: #666;
                  border: 1px solid #d1d5db;
                "
              >
                🚩
              </button>
            </div>
          </div>
        </div>
      </div>
      <p v-else style="color: #666; font-style: italic">{{ t('postDetail.noComments') }}</p>
    </div>

    <!-- Report Dialog -->
    <div
      v-if="showReportDialog"
      style="
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
      "
      @click.self="showReportDialog = false"
    >
      <div
        style="
          background: white;
          padding: 2rem;
          border-radius: 12px;
          max-width: 500px;
          width: 90%;
          max-height: 80vh;
          overflow-y: auto;
        "
      >
        <h3 style="margin-bottom: 1.5rem; font-size: 1.25rem; font-weight: bold">
          {{ t('postDetail.reportPost') }}
        </h3>
        <div style="margin-bottom: 1rem">
          <label style="display: block; margin-bottom: 0.5rem; font-weight: 600">
            {{ t('postDetail.reportReasons') }} <span style="color: #dc2626">*</span>
          </label>
          <div style="display: flex; flex-direction: column; gap: 0.5rem">
            <label
              v-for="reason in reportReasons"
              :key="reason.value"
              style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer"
            >
              <input
                type="checkbox"
                :value="reason.value"
                v-model="selectedReportReasons"
                style="cursor: pointer"
              />
              <span>{{ reason.label }}</span>
            </label>
          </div>
        </div>
        <div style="margin-bottom: 1.5rem">
          <label style="display: block; margin-bottom: 0.5rem; font-weight: 600">
            {{ t('postDetail.reportDescription') }}
          </label>
          <textarea
            v-model="reportDescription"
            :placeholder="t('postDetail.reportDescriptionPlaceholder')"
            rows="4"
            style="
              width: 100%;
              padding: 0.75rem;
              border: 1px solid #d1d5db;
              border-radius: 6px;
              font-family: inherit;
              resize: vertical;
            "
          />
        </div>
        <div style="display: flex; gap: 1rem; justify-content: flex-end">
          <button
            class="btn"
            @click="showReportDialog = false"
            style="padding: 0.75rem 1.5rem"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            class="btn btn-primary"
            @click="handleSubmitReport('POST', postId)"
            :disabled="selectedReportReasons.length === 0 || submittingReport"
            style="padding: 0.75rem 1.5rem"
          >
            {{ submittingReport ? t('postDetail.submitting') : t('postDetail.submitReport') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Comment Report Dialog -->
    <div
      v-if="showCommentReportDialog"
      style="
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
      "
      @click.self="showCommentReportDialog = false"
    >
      <div
        style="
          background: white;
          padding: 2rem;
          border-radius: 12px;
          max-width: 500px;
          width: 90%;
          max-height: 80vh;
          overflow-y: auto;
        "
      >
        <h3 style="margin-bottom: 1.5rem; font-size: 1.25rem; font-weight: bold">
          {{ t('postDetail.reportComment') }}
        </h3>
        <div style="margin-bottom: 1rem">
          <label style="display: block; margin-bottom: 0.5rem; font-weight: 600">
            {{ t('postDetail.reportReasons') }} <span style="color: #dc2626">*</span>
          </label>
          <div style="display: flex; flex-direction: column; gap: 0.5rem">
            <label
              v-for="reason in reportReasons"
              :key="reason.value"
              style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer"
            >
              <input
                type="checkbox"
                :value="reason.value"
                v-model="selectedReportReasons"
                style="cursor: pointer"
              />
              <span>{{ reason.label }}</span>
            </label>
          </div>
        </div>
        <div style="margin-bottom: 1.5rem">
          <label style="display: block; margin-bottom: 0.5rem; font-weight: 600">
            {{ t('postDetail.reportDescription') }}
          </label>
          <textarea
            v-model="reportDescription"
            :placeholder="t('postDetail.reportDescriptionPlaceholder')"
            rows="4"
            style="
              width: 100%;
              padding: 0.75rem;
              border: 1px solid #d1d5db;
              border-radius: 6px;
              font-family: inherit;
              resize: vertical;
            "
          />
        </div>
        <div style="display: flex; gap: 1rem; justify-content: flex-end">
          <button
            class="btn"
            @click="showCommentReportDialog = false; reportTargetId = null"
            style="padding: 0.75rem 1.5rem"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            class="btn btn-primary"
            @click="handleSubmitReport('COMMENT', reportTargetId)"
            :disabled="selectedReportReasons.length === 0 || submittingReport"
            style="padding: 0.75rem 1.5rem"
          >
            {{ submittingReport ? t('postDetail.submitting') : t('postDetail.submitReport') }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { fetchPostDetail, addComment, toggleLike, toggleFollow, getCommentSummary, deleteComment, submitReport } from '../api';
import { getCurrentLanguage, t } from '../i18n';

// Filter out browser extension errors
if (typeof window !== 'undefined') {
  const originalError = console.error;
  console.error = (...args) => {
    const errorMsg = args.join(' ');
    if (errorMsg.includes('content-all.js') || 
        errorMsg.includes('chrome-extension') ||
        errorMsg.includes('Could not establish connection') ||
        errorMsg.includes('Receiving end does not exist')) {
      return;
    }
    originalError.apply(console, args);
  };
}

const props = defineProps({
  postId: {
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
  onAuthorClick: {
    type: Function,
    default: null
  }
});

const postDetail = ref(null);
const loading = ref(true);
const error = ref(null);
const commentContent = ref('');
const submittingComment = ref(false);
const lang = ref(getCurrentLanguage());
const commentSummary = ref(null);
const loadingSummary = ref(false);
const summaryCommentCount = ref(0);
const showReportDialog = ref(false);
const showCommentReportDialog = ref(false);
const reportTargetId = ref(null);
const selectedReportReasons = ref([]);
const reportDescription = ref('');
const submittingReport = ref(false);
const deletingCommentId = ref(null);
const isTogglingFollow = ref(false);

// Report reasons
const reportReasons = computed(() => {
  const langValue = lang.value || 'en';
  if (langValue === 'zh') {
    return [
      { value: 'Spam', label: '垃圾信息' },
      { value: 'Fraud or Scam', label: '欺诈或诈骗' },
      { value: 'Illegal Service Promotion', label: '非法服务推广' },
      { value: 'Abusive Language', label: '辱骂性语言' },
      { value: 'Other', label: '其他' }
    ];
  }
  return [
    { value: 'Spam', label: 'Spam' },
    { value: 'Fraud or Scam', label: 'Fraud or Scam' },
    { value: 'Illegal Service Promotion', label: 'Illegal Service Promotion' },
    { value: 'Abusive Language', label: 'Abusive Language' },
    { value: 'Other', label: 'Other' }
  ];
});

const post = computed(() => postDetail.value?.post);

const formatDate = (dateString) => {
  if (!dateString) {
    return t('postDetail.justNow');
  }
  
  let date;
  try {
    date = new Date(dateString);
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

  if (diffMins < 1) return t('postDetail.justNow');
  if (diffMins < 60) return `${diffMins}${t('postDetail.minutesAgo')}`;
  if (diffHours < 24) return `${diffHours}${t('postDetail.hoursAgo')}`;
  if (diffDays < 7) return `${diffDays}${t('postDetail.daysAgo')}`;
  
  // 使用泰国时区格式化日期显示
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Bangkok',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
  return formatter.format(date);
};

const formatContent = (text) => {
  if (!text) return '';
  
  // 简单的 Markdown 格式化
  let formatted = text;
  
  // 处理行内粗体 **text**
  formatted = formatted.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  
  // 处理分隔线
  formatted = formatted.replace(/^---+$/gm, '<hr style="border: none; border-top: 2px dashed #d1d5db; margin: 1.5rem 0; background: none" />');
  
  // 处理标题行
  formatted = formatted.replace(/^\*\*(.+?)\*\*$/gm, '<h3 style="font-size: 1rem; font-weight: bold; margin-top: 1.5rem; margin-bottom: 0.75rem; color: #111827">$1</h3>');
  
  // 处理链接
  formatted = formatted.replace(/(🔗)\s*\*\*(.+?)\*\*:\s*(.+)/g, (match, emoji, linkText, linkUrl) => {
    const isUrl = linkUrl.trim().match(/^https?:\/\//);
    if (isUrl) {
      return `<div style="margin-top: 1.5rem; margin-bottom: 2rem; padding: 0.75rem 1rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #3b82f6; position: relative; z-index: 1; display: block; width: 100%; box-sizing: border-box; clear: both">
        <div style="font-weight: bold; margin-bottom: 0.5rem; font-size: 0.875rem; color: #374151; display: block; width: 100%">
          🔗 ${linkText}:
        </div>
        <a href="${linkUrl.trim()}" target="_blank" rel="noopener noreferrer" style="color: #2563eb; text-decoration: underline; word-break: break-all; font-size: 0.875rem; display: inline-block; width: 100%; max-width: 100%; overflow-wrap: break-word">
          ${linkUrl.trim()}
        </a>
      </div>`;
    } else {
      return `<div style="margin-top: 1.5rem; margin-bottom: 2rem; padding: 0.75rem 1rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #3b82f6; position: relative; z-index: 1; display: block; width: 100%; box-sizing: border-box; clear: both">
        <div style="font-weight: bold; margin-bottom: 0.5rem; font-size: 0.875rem; color: #374151; display: block; width: 100%">
          🔗 ${linkText}:
        </div>
        <span style="font-size: 0.875rem; color: #6b7280; display: block; word-break: break-word">
          ${linkUrl.trim()}
        </span>
      </div>`;
    }
  });
  
  // 分割成段落
  const paragraphs = formatted.split(/\n\s*\n/).filter(p => p.trim());
  return paragraphs.map(p => `<p style="margin-top: 1rem; margin-bottom: 0.75rem; line-height: 1.8; position: relative; z-index: 0; display: block; width: 100%; clear: both">${p.trim()}</p>`).join('');
};

const loadPostDetail = async () => {
  if (!props.postId) return;
  
  loading.value = true;
  error.value = null;
  try {
    const savedLang = localStorage.getItem('userLanguage');
    const currentLang = getCurrentLanguage();
    const langToUse = lang.value || savedLang || currentLang || 'en';
    
    console.log('🔍 PostDetail: Loading post detail', { 
      postId: props.postId, 
      langToUse,
      lang: lang.value,
      savedLang,
      currentLang,
      'localStorage.userLanguage': localStorage.getItem('userLanguage')
    });
    const data = await fetchPostDetail(props.postId, langToUse);
    console.log('📦 PostDetail: Received data:', {
      hasPost: !!data.post,
      postTitle: data.post?.title,
      postTitleLength: data.post?.title?.length || 0,
      postBody: data.post?.body ? `${data.post.body.substring(0, 100)}...` : 'EMPTY',
      postBodyLength: data.post?.body?.length || 0,
      hasContentZh: !!data.post?.contentZh,
      hasContentEn: !!data.post?.contentEn,
      'requestedLang': langToUse,
      fullData: data
    });
    // Ensure isFollowing is properly initialized
    postDetail.value = {
      ...data,
      isFollowing: data.isFollowing !== undefined ? data.isFollowing : false
    };
  } catch (err) {
    console.error('Failed to load post detail:', err);
    error.value = 'Failed to load post';
  } finally {
    loading.value = false;
  }
};

const handleAddComment = async () => {
  if (!commentContent.value.trim() || !props.token) {
    return;
  }

  submittingComment.value = true;
  try {
    const savedLang = localStorage.getItem('userLanguage');
    const currentLang = getCurrentLanguage();
    const langToUse = lang.value || savedLang || currentLang || 'en';
    
    await addComment(props.postId, commentContent.value, langToUse, props.token);
    commentContent.value = '';
    // Clear summary when new comment is added
    commentSummary.value = null;
    summaryCommentCount.value = 0;
    await loadPostDetail();
  } catch (err) {
    console.error('Failed to add comment:', err);
    alert(t('postDetail.commentFailed'));
  } finally {
    submittingComment.value = false;
  }
};

const handleGenerateSummary = async () => {
  if (!props.postId || !postDetail.value?.comments || postDetail.value.comments.length === 0) {
    return;
  }

  loadingSummary.value = true;
  try {
    const savedLang = localStorage.getItem('userLanguage');
    const currentLang = getCurrentLanguage();
    const langToUse = lang.value || savedLang || currentLang || 'en';
    
    const response = await getCommentSummary(props.postId, langToUse);
    
    if (response.summary) {
      commentSummary.value = response.summary;
      summaryCommentCount.value = response.commentCount || 0;
    } else if (response.error) {
      alert(response.message || response.error);
    }
  } catch (err) {
    console.error('Failed to generate comment summary:', err);
    alert(t('postDetail.summaryFailed') || 'Failed to generate comment summary');
  } finally {
    loadingSummary.value = false;
  }
};

const handleToggleLike = async () => {
  if (!props.token) {
    alert(t('postDetail.loginRequired'));
    return;
  }

  try {
    const result = await toggleLike(props.postId, props.token);
    postDetail.value = {
      ...postDetail.value,
      isLiked: result.liked,
      likeCount: result.likeCount
    };
  } catch (err) {
    console.error('Failed to toggle like:', err);
  }
};

const handleDeleteComment = async (commentId) => {
  if (!confirm(t('postDetail.confirmDeleteComment'))) {
    return;
  }
  
  deletingCommentId.value = commentId;
  try {
    await deleteComment(props.postId, commentId, props.token);
    // Reload post detail to refresh comments
    await loadPostDetail();
    alert(t('postDetail.commentDeleted'));
  } catch (err) {
    console.error('Failed to delete comment:', err);
    alert(err.response?.data?.message || t('postDetail.commentDeleteFailed'));
  } finally {
    deletingCommentId.value = null;
  }
};

const openCommentReportDialog = (commentId) => {
  reportTargetId.value = commentId;
  selectedReportReasons.value = [];
  reportDescription.value = '';
  showCommentReportDialog.value = true;
};

const handleSubmitReport = async (targetType, targetId) => {
  if (!targetId || selectedReportReasons.value.length === 0) {
    return;
  }
  
  submittingReport.value = true;
  try {
    await submitReport(targetType, targetId, selectedReportReasons.value, reportDescription.value, props.token);
    alert(t('postDetail.reportSubmitted'));
    showReportDialog.value = false;
    showCommentReportDialog.value = false;
    reportTargetId.value = null;
    selectedReportReasons.value = [];
    reportDescription.value = '';
  } catch (err) {
    console.error('Failed to submit report:', err);
    alert(err.response?.data?.message || t('postDetail.reportFailed'));
  } finally {
    submittingReport.value = false;
  }
};

const handleViewAuthorProfile = () => {
  if (postDetail.value?.authorId && props.onAuthorClick) {
    props.onAuthorClick(postDetail.value.authorId);
  }
};

const handleToggleFollow = async () => {
  if (!props.token) {
    alert(t('postDetail.loginRequired'));
    return;
  }

  if (!postDetail.value || !postDetail.value.authorId || isTogglingFollow.value) {
    return;
  }

  isTogglingFollow.value = true;
  try {
    // Pass current following state to avoid unnecessary error logging
    // Use nullish coalescing to properly handle undefined
    const currentFollowing = postDetail.value?.isFollowing ?? false;
    const result = await toggleFollow(postDetail.value.authorId, props.token, currentFollowing);
    if (result && (result.success !== false || result.following !== undefined)) {
    postDetail.value = {
      ...postDetail.value,
        isFollowing: result.following !== undefined ? result.following : !currentFollowing
    };
    } else {
      // If toggle failed, show error message
      const errorMsg = result?.message || t('postDetail.followFailed');
      alert(errorMsg);
    }
  } catch (err) {
    // If we get a 400 error with "Already following", it means the state was out of sync
    // Try to fix it by refreshing the post detail
    if (err.response?.status === 400 && 
        (err.response?.data?.message?.includes('Already following') || 
         err.response?.data?.message?.includes('already following'))) {
      // State is out of sync, reload the post detail to get correct state
      console.warn('Follow state out of sync, reloading post detail...');
      await loadPostDetail();
      // After reload, explicitly set isFollowing to true since we got "Already following" error
      if (postDetail.value) {
        postDetail.value.isFollowing = true;
      }
      // Don't show error to user, just silently fix the state
      return;
    }
    
    // Only log unexpected errors
    if (!err.isExpectedError) {
    console.error('Failed to toggle follow:', err);
    }
    const errorMsg = err.response?.data?.message || t('postDetail.followFailed');
    // Only show alert for unexpected errors
    if (!err.isExpectedError && err.response?.status !== 400) {
      alert(errorMsg);
    }
  } finally {
    isTogglingFollow.value = false;
  }
};

onMounted(() => {
  const savedLang = localStorage.getItem('userLanguage');
  const currentLang = getCurrentLanguage();
  const langToSet = (savedLang === 'zh' || savedLang === 'en') ? savedLang : (currentLang || 'en');
  lang.value = langToSet;
  
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      console.log('PostDetail: Language changed to:', e.detail.lang);
      const previousLang = lang.value;
      lang.value = e.detail.lang;
      
      // 如果对话框是打开的，保持打开状态，只更新翻译文本
      // 不触发数据重新加载，避免对话框闪烁
      if (showReportDialog.value || showCommentReportDialog.value) {
        // 对话框打开时，只更新UI，不重新加载数据
        return;
      }
      
      // 只有在对话框关闭时才重新加载数据
      if (props.postId && previousLang !== lang.value) {
        // 延迟加载，避免频繁请求
        setTimeout(() => {
          if (!showReportDialog.value && !showCommentReportDialog.value) {
            loadPostDetail();
          }
        }, 100);
      }
    }
  };
  
  if (typeof window !== 'undefined') {
    window.addEventListener('languageChanged', handleLanguageChange);
  }
  
  loadPostDetail();
  
  return () => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('languageChanged', handleLanguageChange);
    }
  };
});

watch(() => props.postId, () => {
  if (props.postId) {
    loadPostDetail();
  }
});

watch(lang, () => {
  // 当语言变化时，如果对话框没有打开，才重新加载数据
  // 这样可以避免对话框在语言切换时关闭和闪烁
  if (props.postId && !showReportDialog.value && !showCommentReportDialog.value) {
    loadPostDetail();
  }
});
</script>

<style scoped>
/* Styles are in styles.css */
</style>

