<template>
  <div class="daily-briefing">
    <!-- 顶部标题栏 -->
    <div class="header-bar">
      <div class="header-content">
        <div class="header-left">
          <i class="el-icon-news header-icon"></i>
          <h2 class="title">{{ t('dailyBriefing.title') }}</h2>
        </div>
        <div class="header-right">
          <el-tag type="info" size="small">
            <i class="el-icon-refresh"></i>
            {{ t('dailyBriefing.updatedAt') }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 搜索和过滤栏 -->
    <el-card class="filter-card" shadow="never">
      <div class="filter-container">
        <!-- 搜索框 -->
        <div class="search-section">
          <el-input
            v-model="searchKeyword"
            :placeholder="t('dailyBriefing.searchPlaceholder')"
            clearable
            @keyup.enter.native="handleSearch"
            @clear="handleSearch">
            <el-button
              slot="append"
              icon="el-icon-search"
              @click="handleSearch">
            </el-button>
          </el-input>
        </div>

        <!-- 过滤条件 -->
        <div class="filter-section">
          <div class="filter-group">
            <label class="filter-label">
              <i class="el-icon-date"></i>
              {{ t('dailyBriefing.startDate') }}
            </label>
            <el-date-picker
              v-model="filterStartDate"
              type="date"
              format="dd-MM-yyyy"
              value-format="yyyy-MM-dd"
              :placeholder="t('dailyBriefing.selectStartDate')"
              :key="`start-date-${currentLang}`"
              @change="handleFilterChange"
              style="width: 100%;">
            </el-date-picker>
          </div>

          <div class="filter-group">
            <label class="filter-label">
              <i class="el-icon-date"></i>
              {{ t('dailyBriefing.endDate') }}
            </label>
            <el-date-picker
              v-model="filterEndDate"
              type="date"
              format="dd-MM-yyyy"
              value-format="yyyy-MM-dd"
              :placeholder="t('dailyBriefing.selectEndDate')"
              :key="`end-date-${currentLang}`"
              @change="handleFilterChange"
              style="width: 100%;">
            </el-date-picker>
          </div>

          <div class="filter-group">
            <label class="filter-label">
              <i class="el-icon-link"></i>
              {{ t('dailyBriefing.source') }}
            </label>
            <el-select
              v-model="filterSource"
              :placeholder="t('dailyBriefing.selectSource')"
              :key="`source-select-${currentLang}`"
              clearable
              @change="handleFilterChange"
              style="width: 100%;">
              <el-option
                v-for="source in sourceOptions"
                :key="source.value"
                :label="source.label"
                :value="source.value">
              </el-option>
            </el-select>
          </div>

          <div class="filter-actions">
            <el-button
              type="default"
              icon="el-icon-refresh-left"
              @click="resetFilters">
              {{ t('dailyBriefing.reset') }}
            </el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-card shadow="never" class="loading-card">
        <div class="loading-content">
          <i class="el-icon-loading loading-icon"></i>
          <span class="loading-text">{{ t('dailyBriefing.loading') }}</span>
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

    <!-- 新闻列表 -->
    <div v-else-if="newsList.length > 0" class="news-list">
      <transition-group name="news-fade" tag="div">
        <el-card
          v-for="news in newsList"
          :key="news.id"
          class="news-card"
          shadow="hover">
          <div slot="header" class="card-header">
            <div class="card-header-content">
              <h3 class="news-title">{{ news.title }}</h3>
              <el-tag
                :type="getSourceTagType(news.source)"
                size="small"
                class="source-tag">
                <i class="el-icon-link"></i>
                {{ news.source || 'Unknown' }}
              </el-tag>
            </div>
          </div>
          
          <div class="news-content">
            <!-- AI 总结 -->
            <div class="summary-section">
              <div class="section-header">
                <i class="el-icon-magic-stick summary-icon"></i>
                <span class="section-label">{{ t('dailyBriefing.aiSummary') }}</span>
              </div>
              <p class="summary-text">{{ news.summary || t('dailyBriefing.noSummary') }}</p>
            </div>

            <!-- 元信息 -->
            <div class="meta-info">
              <div class="meta-item">
                <i class="el-icon-time meta-icon"></i>
                <span class="meta-label">{{ t('dailyBriefing.publishTime') }}</span>
                <span class="meta-value">{{ formatDate(news.publishDate) || formatDate(news.createTime) }}</span>
              </div>
              <div class="meta-item" v-if="news.crawlTime">
                <i class="el-icon-download meta-icon"></i>
                <span class="meta-label">{{ t('dailyBriefing.crawlTime') }}</span>
                <span class="meta-value">{{ formatDate(news.crawlTime) }}</span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="action-section">
              <el-button
                type="default"
                icon="el-icon-view"
                @click="viewDetail(news.id)"
                class="detail-button">
                {{ t('dailyBriefing.viewDetail') }}
              </el-button>
              <el-button
                type="primary"
                icon="el-icon-top-right"
                @click="openOriginalUrl(news.originalUrl)"
                :disabled="!news.originalUrl"
                class="original-button">
                {{ t('dailyBriefing.readOriginal') }}
                <i class="el-icon-top-right external-icon"></i>
              </el-button>
            </div>
          </div>
        </el-card>
      </transition-group>

      <!-- 分页 -->
      <div class="pagination-container" v-if="pagination.totalPages > 1">
        <el-pagination
          @current-change="handlePageChange"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="pagination.totalElements"
          layout="total, prev, pager, next, jumper"
          background>
        </el-pagination>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-container">
      <el-card shadow="never" class="empty-card">
        <el-empty
          :description="t('dailyBriefing.noData')"
          :image-size="120">
          <el-button
            type="primary"
            @click="resetFilters">
            {{ t('dailyBriefing.resetFilters') }}
          </el-button>
        </el-empty>
      </el-card>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { t as translate, getCurrentLanguage } from '../../i18n';

export default {
  name: 'DailyBriefing',
  data() {
    return {
      newsList: [],
      loading: false,
      error: null,
      currentPage: 1,
      pageSize: 10,
      pagination: {
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
        hasPrevious: false
      },
      // 搜索和过滤
      searchKeyword: '',
      filterStartDate: null,
      filterEndDate: null,
      filterSource: null,
      sourceOptions: [], // 将从后端动态获取
      // 当前语言
      currentLang: getCurrentLanguage()
    };
  },
  computed: {
    // 使用计算属性来确保翻译文本响应语言变化
    currentLanguage() {
      return this.currentLang;
    }
  },
  watch: {
    // 监听语言变化，触发重新渲染
    currentLang() {
      this.$forceUpdate();
    }
  },
  mounted() {
    this.fetchDailyBriefing();
    this.fetchNewsSources(); // 获取所有可用的新闻来源
    // 监听语言变化
    this.handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        this.currentLang = e.detail.lang;
        // 强制重新渲染以更新所有翻译文本
        this.$forceUpdate();
        this.fetchDailyBriefing();
      }
    };
    window.addEventListener('languageChanged', this.handleLanguageChange);
  },
  beforeUnmount() {
    // 清理事件监听器
    if (this.handleLanguageChange) {
      window.removeEventListener('languageChanged', this.handleLanguageChange);
    }
  },
  methods: {
    // 添加 t 方法以便在模板和 methods 中使用
    // 使用响应式的 currentLang 确保翻译文本更新
    t(key, params) {
      // 使用当前组件的响应式语言变量
      // translate 函数会读取全局语言，但我们需要确保组件重新渲染
      return translate(key, params);
    },
    /**
     * 获取所有可用的新闻来源
     */
    async fetchNewsSources() {
      try {
        const response = await axios.get('/api/news/sources');
        if (response.data.success && response.data.data) {
          // 将来源数组转换为选项格式
          this.sourceOptions = response.data.data.map(source => ({
            label: source,
            value: source
          }));
          console.log('Loaded news sources:', this.sourceOptions.length, this.sourceOptions);
        } else {
          console.warn('Failed to fetch news sources:', response.data.message);
          // 如果获取失败，使用空数组
          this.sourceOptions = [];
        }
      } catch (error) {
        console.error('Error fetching news sources:', error);
        // 如果出错，使用空数组
        this.sourceOptions = [];
      }
    },
    /**
     * 获取新闻简报
     */
    async fetchDailyBriefing() {
      this.loading = true;
      this.error = null;

      try {
        // 计算页码（从 0 开始）
        const page = this.currentPage - 1;
        
        // 构建查询参数
        const params = {
          page: page,
          size: this.pageSize
        };

        // 添加搜索关键词
        if (this.searchKeyword && this.searchKeyword.trim()) {
          params.keyword = this.searchKeyword.trim();
        }

        // 添加日期过滤
        if (this.filterStartDate) {
          params.startDate = this.filterStartDate;
        }
        if (this.filterEndDate) {
          params.endDate = this.filterEndDate;
        }

        // 添加来源过滤
        if (this.filterSource) {
          params.source = this.filterSource;
        }
        
        // 添加语言参数（重要：根据当前语言设置传递lang参数）
        params.lang = this.currentLang || 'en';
        
        const response = await axios.get('/api/news/daily-briefing', { params });

        if (response.data.success) {
          this.newsList = response.data.data || [];
          this.pagination = {
            totalElements: response.data.pagination?.totalElements || 0,
            totalPages: response.data.pagination?.totalPages || 0,
            hasNext: response.data.pagination?.hasNext || false,
            hasPrevious: response.data.pagination?.hasPrevious || false
          };
        } else {
          this.error = response.data.message || t('dailyBriefing.fetchFailed');
        }
      } catch (error) {
        console.error('获取新闻简报失败:', error);
        this.error = error.response?.data?.message || error.message || t('dailyBriefing.networkError');
      } finally {
        this.loading = false;
      }
    },

    /**
     * 处理搜索
     */
    handleSearch() {
      this.currentPage = 1;
      this.fetchDailyBriefing();
    },

    /**
     * 处理过滤条件变化
     */
    handleFilterChange() {
      this.currentPage = 1;
      this.fetchDailyBriefing();
    },

    /**
     * 重置所有过滤条件
     */
    resetFilters() {
      this.searchKeyword = '';
      this.filterStartDate = null;
      this.filterEndDate = null;
      this.filterSource = null;
      this.currentPage = 1;
      this.fetchDailyBriefing();
    },

    /**
     * 处理分页变化
     */
    handlePageChange(page) {
      this.currentPage = page;
      this.fetchDailyBriefing();
      // 滚动到顶部
      this.$nextTick(() => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      });
    },

    /**
     * 查看详情
     */
    viewDetail(newsId) {
      this.$emit('view-detail', newsId);
    },

    /**
     * 打开原文链接
     */
    openOriginalUrl(url) {
      if (url) {
        window.open(url, '_blank');
      }
    },

    /**
     * 获取来源标签类型
     */
    getSourceTagType(source) {
      if (source === 'Bangkok Post') {
        return 'primary';
      } else if (source === 'The Nation Thailand') {
        return 'success';
      }
      return 'info';
    },

    /**
     * 格式化日期（日-月-年 时:分）
     */
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
      return `${day}-${month}-${year} ${hours}:${minutes}`;
    }
  }
};
</script>

<style scoped>
.daily-briefing {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  background: linear-gradient(to bottom, #f5f7fa 0%, #ffffff 100%);
  min-height: 100vh;
}

/* 标题栏样式 */
.header-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 28px 32px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  position: relative;
  overflow: hidden;
}

.header-bar::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  50% { transform: translate(-10%, -10%) rotate(180deg); }
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 32px;
  opacity: 0.9;
}

.header-bar .title {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0.5px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.header-right {
  display: flex;
  align-items: center;
}

/* 过滤卡片 */
.filter-card {
  margin-bottom: 24px;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-section {
  width: 100%;
}

.filter-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  align-items: end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label i {
  color: #909399;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
}

/* 加载状态 */
.loading-container,
.error-container,
.empty-container {
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
  color: #409eff;
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

/* 新闻列表 */
.news-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.news-fade-enter-active,
.news-fade-leave-active {
  transition: all 0.4s ease;
}

.news-fade-enter,
.news-fade-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.news-card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}

.news-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  border-color: #c0c4cc;
}

/* 卡片头部 */
.card-header {
  padding: 0;
  background: linear-gradient(to right, #f8f9fa 0%, #ffffff 100%);
}

.card-header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 4px 0;
}

.news-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  line-height: 1.6;
  flex: 1;
  transition: color 0.3s;
}

.news-card:hover .news-title {
  color: #409eff;
}

.source-tag {
  flex-shrink: 0;
  font-weight: 500;
  padding: 4px 12px;
}

.source-tag i {
  margin-right: 4px;
}

/* 新闻内容 */
.news-content {
  padding: 20px 0;
}

/* 总结部分 */
.summary-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.summary-icon {
  font-size: 18px;
  color: #409eff;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #606266;
}

.summary-text {
  margin: 0;
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
  text-align: justify;
  padding: 16px 20px;
  background: linear-gradient(to right, #f5f7fa 0%, #fafbfc 100%);
  border-radius: 8px;
  border-left: 4px solid #409eff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  transition: all 0.3s;
}

.news-card:hover .summary-text {
  background: linear-gradient(to right, #ecf5ff 0%, #f5f7fa 100%);
  border-left-color: #66b1ff;
}

/* 元信息 */
.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 20px;
  padding: 16px 20px;
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

/* 操作按钮 */
.action-section {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.detail-button {
  font-weight: 500;
  padding: 10px 20px;
  border-radius: 6px;
  transition: all 0.3s;
}

.detail-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.original-button {
  font-weight: 500;
  padding: 10px 20px;
  border-radius: 6px;
  transition: all 0.3s;
}

.original-button:hover {
  transform: translateX(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.external-icon {
  margin-left: 6px;
  font-size: 12px;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 40px;
  padding: 24px 0;
}

/* 空状态 */
.empty-card {
  border-radius: 12px;
  padding: 40px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .daily-briefing {
    padding: 16px;
  }

  .header-bar {
    padding: 20px 24px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-icon {
    font-size: 24px;
  }

  .header-bar .title {
    font-size: 24px;
  }

  .filter-section {
    grid-template-columns: 1fr;
  }

  .card-header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .news-title {
    font-size: 18px;
  }

  .meta-info {
    flex-direction: column;
    gap: 12px;
  }

  .action-section {
    justify-content: center;
  }

  .summary-text {
    padding: 12px 16px;
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .header-bar {
    padding: 16px 20px;
  }

  .header-bar .title {
    font-size: 20px;
  }

  .news-title {
    font-size: 16px;
  }
}
</style>

