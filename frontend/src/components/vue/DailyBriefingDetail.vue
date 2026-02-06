<template>
  <div class="news-detail">
    <!-- 返回按钮 -->
    <div class="back-section">
      <el-button
        icon="el-icon-arrow-left"
        @click="goBack"
        link>
        {{ t('dailyBriefingDetail.back') }}
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-card shadow="never" class="loading-card">
        <div class="loading-content">
          <i class="el-icon-loading loading-icon"></i>
          <span class="loading-text">{{ t('dailyBriefingDetail.loading') }}</span>
        </div>
      </el-card>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <el-card shadow="never">
        <el-alert
          :title="error"
          type="error"
          :closable="false"
          show-icon>
        </el-alert>
      </el-card>
    </div>

    <!-- 新闻详情 -->
    <div v-else-if="news" class="news-content">
      <el-card shadow="hover" class="detail-card">
        <!-- 标题和来源 -->
        <div slot="header" class="card-header">
          <div class="header-content">
            <h1 class="news-title">{{ news.title }}</h1>
            <el-tag
              :type="getSourceTagType(news.source)"
              size="medium"
              class="source-tag">
              <i class="el-icon-link"></i>
              {{ news.source || 'Unknown' }}
            </el-tag>
          </div>
        </div>

        <!-- AI 总结 -->
        <div class="summary-section">
          <div class="section-header">
            <i class="el-icon-magic-stick summary-icon"></i>
            <span class="section-label">{{ t('dailyBriefingDetail.aiSummary') }}</span>
          </div>
          <div class="summary-text" v-if="news.summary && news.summary.trim()">
            {{ news.summary }}
          </div>
          <div class="summary-text no-summary" v-else>
            <i class="el-icon-info"></i>
            {{ t('dailyBriefingDetail.noSummary') }}
          </div>
        </div>

        <!-- 原始内容（如果可用且不是泰文） -->
        <div class="content-section" v-if="originalContent && originalContent.trim()">
          <div class="section-header">
            <i class="el-icon-document content-icon"></i>
            <span class="section-label">{{ t('dailyBriefingDetail.originalContent') }}</span>
          </div>
          <div class="content-text">{{ originalContent }}</div>
        </div>

        <!-- 元信息 -->
        <div class="meta-info">
          <div class="meta-item">
            <i class="el-icon-time meta-icon"></i>
            <span class="meta-label">{{ t('dailyBriefingDetail.publishTime') }}</span>
            <span class="meta-value">{{ formatDate(news.publishDate) || formatDate(news.createTime) }}</span>
          </div>
          <div class="meta-item" v-if="news.createTime">
            <i class="el-icon-download meta-icon"></i>
            <span class="meta-label">{{ t('dailyBriefingDetail.crawlTime') }}</span>
            <span class="meta-value">{{ formatDate(news.createTime) }}</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-section">
          <el-button
            type="primary"
            icon="el-icon-top-right"
            @click="openOriginalUrl(news.originalUrl)"
            :disabled="!news.originalUrl"
            class="original-button">
            {{ t('dailyBriefingDetail.readOriginal') }}
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { t as translate, getCurrentLanguage } from '../../i18n';

export default {
  name: 'DailyBriefingDetail',
  props: {
    newsId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      news: null,
      originalContent: null,
      loading: false,
      error: null,
      currentLang: getCurrentLanguage()
    };
  },
  watch: {
    currentLang() {
      this.$forceUpdate();
      this.fetchNewsDetail();
    }
  },
  mounted() {
    this.fetchNewsDetail();
    // 监听语言变化
    this.handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        this.currentLang = e.detail.lang;
      }
    };
    window.addEventListener('languageChanged', this.handleLanguageChange);
  },
  beforeUnmount() {
    if (this.handleLanguageChange) {
      window.removeEventListener('languageChanged', this.handleLanguageChange);
    }
  },
  methods: {
    t(key, params) {
      return translate(key, params);
    },
    async fetchNewsDetail() {
      this.loading = true;
      this.error = null;

      try {
        const params = {
          lang: this.currentLang || 'en'
        };
        const response = await axios.get(`/api/news/daily-briefing/${this.newsId}`, { params });

        // Per activity diagram: First check if HTTP request succeeds (status 200)
        if (response.status === 200) {
          // Then check if Response data.success is true
          if (response.data.success) {
            // Frontend sets news = response.data.data
            this.news = response.data.data;
            // Frontend sets originalContent = response.data.originalContent
            this.originalContent = response.data.originalContent || null;
          } else {
            // Frontend shows error message from response.data.message or localized message
            this.error = response.data.message || this.t('dailyBriefingDetail.fetchFailed');
          }
        } else {
          // Non-200 status code (should not normally occur with axios, but handle per activity diagram)
          this.error = this.t('dailyBriefingDetail.networkError');
        }
      } catch (error) {
        console.error('获取新闻详情失败:', error);
        // Handle errors according to activity diagram in BridgeU-SRS.md
        // Check if HTTP response was received
        if (error.response) {
          // HTTP response received - check status code
          if (error.response.status === 404) {
            // 404: Frontend shows localized error message using i18n key 'dailyBriefingDetail.notFound'
            this.error = this.t('dailyBriefingDetail.notFound');
          } else if (error.response.status === 401) {
            // 401: Authentication token invalid or expired
            // The api.js interceptor will handle token clearing and trigger auth:unauthorized event
            // App.vue will handle logout and redirect to login page
            // Component can show user-friendly message if needed, but redirect will happen automatically
            if (error.userMessage) {
              this.error = error.userMessage;
            } else {
              this.error = this.currentLang === 'zh' ? '登录已过期，请重新登录' : 'Login expired, please log in again';
            }
          } else if (error.response.status === 500) {
            // 500: Frontend shows error message from response.data.message
            this.error = error.response.data?.message || this.t('dailyBriefingDetail.networkError');
          } else if (error.response.status === 200) {
            // 200: Check response.data.success (handled in try block above)
            // This should not happen here, but handle for safety
            if (error.response.data && !error.response.data.success) {
              this.error = error.response.data.message || this.t('dailyBriefingDetail.fetchFailed');
            }
          } else if (error.response.data && error.response.data.message) {
            // Other HTTP status codes: Show error message if available
            this.error = error.response.data.message;
          } else {
            // HTTP error without message
            this.error = this.t('dailyBriefingDetail.networkError');
          }
        } else {
          // Network error or timeout (no HTTP response received)
          // Per activity diagram: check if error.response exists and error.response.data.message exists
          // Note: This check is per activity diagram specification, even though in JavaScript
          // if error.response exists, code would enter the if branch above
          if (error.response && error.response.data && error.response.data.message) {
            // Frontend shows error message from error.response.data.message
            this.error = error.response.data.message;
          } else {
            // Frontend shows localized message using i18n key 'dailyBriefingDetail.networkError' or error.message
            this.error = error.message || this.t('dailyBriefingDetail.networkError');
          }
        }
      } finally {
        this.loading = false;
      }
    },
    goBack() {
      this.$emit('back');
    },
    openOriginalUrl(url) {
      if (url) {
        window.open(url, '_blank');
      }
    },
    getSourceTagType(source) {
      if (source === 'Bangkok Post') {
        return 'primary';
      } else if (source === 'The Nation Thailand') {
        return 'success';
      }
      return 'info';
    },
    formatDate(date) {
      if (!date) return '';
      
      const d = new Date(date);
      if (isNaN(d.getTime())) return '';

      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      const hours = String(d.getHours()).padStart(2, '0');
      const minutes = String(d.getMinutes()).padStart(2, '0');

      // 根据当前语言格式化日期：日-月-年 时:分
      if (this.currentLang === 'zh') {
        return `${day}-${month}-${year} ${hours}:${minutes}`;
      } else {
        return `${day}-${month}-${year} ${hours}:${minutes}`;
      }
    }
  }
};
</script>

<style scoped>
.news-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  background: linear-gradient(to bottom, #f5f7fa 0%, #ffffff 100%);
  min-height: 100vh;
}

.back-section {
  margin-bottom: 20px;
}

.loading-container,
.error-container {
  margin-bottom: 24px;
}

.loading-card {
  border-radius: 12px;
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  color: #909399;
}

.loading-icon {
  font-size: 36px;
  margin-bottom: 16px;
  animation: rotating 2s linear infinite;
  color: var(--color-primary);
}

.loading-text {
  font-size: 16px;
  font-weight: 500;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.detail-card {
  border-radius: 12px;
  border: 1px solid #e4e7ed;
}

.card-header {
  padding: 0;
  background: linear-gradient(to right, #f8f9fa 0%, #ffffff 100%);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 4px 0;
}

.news-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.5;
  flex: 1;
}

.source-tag {
  flex-shrink: 0;
  font-weight: 500;
  padding: 6px 16px;
  font-size: 14px;
}

.source-tag i {
  margin-right: 4px;
}

.summary-section {
  margin: 24px 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.summary-icon {
  font-size: 20px;
  color: var(--color-primary);
}

.section-label {
  font-size: 16px;
  font-weight: 600;
  color: #606266;
}

.summary-text {
  font-size: 16px;
  color: #606266;
  line-height: 1.8;
  text-align: justify;
  padding: 20px 24px;
  background: linear-gradient(to right, #f5f7fa 0%, #fafbfc 100%);
  border-radius: 8px;
  border-left: 4px solid var(--color-primary);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  white-space: pre-wrap;
  word-wrap: break-word;
}

.summary-text.no-summary {
  color: #909399;
  font-style: italic;
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-section {
  margin: 24px 0;
}

.content-icon {
  font-size: 20px;
  color: #67c23a;
}

.content-text {
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
  text-align: justify;
  padding: 20px 24px;
  background: linear-gradient(to right, #f0f9ff 0%, #f5f7fa 100%);
  border-radius: 8px;
  border-left: 4px solid #67c23a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 600px;
  overflow-y: auto;
}

.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin: 24px 0;
  padding: 20px 24px;
  background: linear-gradient(to right, #fafafa 0%, #f5f7fa 100%);
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.meta-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #909399;
  gap: 6px;
}

.meta-icon {
  font-size: 16px;
  color: #c0c4cc;
}

.meta-label {
  font-weight: 500;
  margin-right: 4px;
}

.meta-value {
  color: #606266;
  font-weight: 500;
}

.action-section {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #ebeef5;
}

.original-button {
  font-weight: 500;
  padding: 12px 24px;
  border-radius: 6px;
  transition: all 0.3s;
}

.original-button:hover {
  transform: translateX(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

@media (max-width: 768px) {
  .news-detail {
    padding: 16px;
  }

  .news-title {
    font-size: 22px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .meta-info {
    flex-direction: column;
    gap: 12px;
  }

  .action-section {
    justify-content: center;
  }
}
</style>

