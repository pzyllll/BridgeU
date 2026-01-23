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

        <!-- 过滤条件（仅按日期范围过滤） -->
        <div class="filter-section">
          <div class="filter-group">
            <label class="filter-label">
              <i class="el-icon-date"></i>
              {{ t('dailyBriefing.startDate') }}
            </label>
            <el-date-picker
              v-model="filterStartDate"
              type="date"
              :editable="false"
              format="DD-MM-YYYY"
              :placeholder="t('dailyBriefing.selectStartDate')"
              :key="`start-date-${currentLang}`"
              @change="handleStartDateChange"
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
              :editable="false"
              format="DD-MM-YYYY"
              :placeholder="t('dailyBriefing.selectEndDate')"
              :key="`end-date-${currentLang}`"
              @change="handleEndDateChange"
              style="width: 100%;">
            </el-date-picker>
          </div>

          <!-- 来源筛选已取消，过滤逻辑只按日期范围和关键字 -->

          <div class="filter-actions">
            <el-button
              type="primary"
              icon="el-icon-search"
              @click="applyFilters">
              {{ t('dailyBriefing.filter') }}
            </el-button>
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
      // 搜索和过滤（仅按日期范围和关键字）
      searchKeyword: '',
      filterStartDate: null,
      filterEndDate: null,
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

        // 添加日期过滤：使用 Date 对象，统一格式化为 yyyy-MM-dd（按发布日期过滤）
        const safeStart = this.normalizeDateValue(this.filterStartDate);
        const safeEnd = this.normalizeDateValue(this.filterEndDate);
        if (safeStart) {
          params.startDate = safeStart;
        }
        if (safeEnd) {
          params.endDate = safeEnd;
        }
        
        // 添加语言参数（重要：根据当前语言设置传递lang参数）
        params.lang = this.currentLang || 'en';
        
        const response = await axios.get('/api/news/daily-briefing', { params });

        // 调试信息：记录请求参数和响应
        console.log('📊 Daily Briefing Request:', {
          params,
          responseSize: JSON.stringify(response.data).length,
          hasData: response.data.success && response.data.data,
          dataLength: response.data.data?.length || 0,
          totalElements: response.data.pagination?.totalElements || 0
        });

        if (response.data.success) {
          this.newsList = response.data.data || [];
          this.pagination = {
            totalElements: response.data.pagination?.totalElements || 0,
            totalPages: response.data.pagination?.totalPages || 0,
            hasNext: response.data.pagination?.hasNext || false,
            hasPrevious: response.data.pagination?.hasPrevious || false
          };
          
          // 如果没有数据，显示提示信息（仅按日期范围和关键字）
          if (this.newsList.length === 0 && (this.filterStartDate || this.filterEndDate || this.searchKeyword)) {
            console.warn('⚠️ No news found with current filters:', {
              startDate: this.filterStartDate,
              endDate: this.filterEndDate,
              keyword: this.searchKeyword
            });
          }
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
     * 处理开始日期变化
     */
    handleStartDateChange(value) {
      // 直接使用 Date 对象；清空时为 null
      this.filterStartDate = value || null;
    },

    /**
     * 处理结束日期变化
     */
    handleEndDateChange(value) {
      // 直接使用 Date 对象；清空时为 null
      this.filterEndDate = value || null;
    },

    /**
     * 应用筛选条件
     */
    applyFilters() {
      // 验证日期范围
      if (this.filterStartDate && this.filterEndDate) {
        const startDate = new Date(this.filterStartDate);
        const endDate = new Date(this.filterEndDate);
        if (startDate > endDate) {
          this.$message.warning(this.t('dailyBriefing.invalidDateRange'));
          return;
        }
      }

      this.currentPage = 1;
      this.fetchDailyBriefing();
    },

    /**
     * 处理过滤条件变化（已废弃，改为使用 applyFilters）
     */
    handleFilterChange() {
      // 不再自动触发，需要用户点击筛选按钮
    },

    /**
     * 重置所有过滤条件
     */
    resetFilters() {
      this.searchKeyword = '';
      this.filterStartDate = null;
      this.filterEndDate = null;
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
      if (newsId === null || newsId === undefined || newsId === '') {
        // Provide explicit feedback instead of “no response”
        if (this.$message && this.$message.warning) {
          this.$message.warning(this.t('dailyBriefing.invalidNewsId') || '无法打开详情：新闻ID无效');
        } else {
          console.warn('Invalid newsId for viewDetail:', newsId);
        }
        return;
      }
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
     * 规范化日期值：支持 Date 对象和字符串
     * @param {Date|string} value
     * @returns {string|null}
     */
    normalizeDateValue(value) {
      if (!value) return null;
      if (value instanceof Date) {
        const year = value.getFullYear();
        const month = String(value.getMonth() + 1).padStart(2, '0');
        const day = String(value.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      }
      if (typeof value === 'string') {
        const trimmed = value.trim();
        // 忽略带有字母的占位符或未完成输入（如 yyyy-01-Tu）
        if (/[A-Za-z]/.test(trimmed)) {
          return null;
        }
        // 如果已经是 yyyy-MM-dd 格式，直接返回（避免时区转换问题）
        const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
        if (dateRegex.test(trimmed)) {
          // 验证日期是否有效
          const [year, month, day] = trimmed.split('-').map(Number);
          const testDate = new Date(year, month - 1, day);
          if (testDate.getFullYear() === year && 
              testDate.getMonth() + 1 === month && 
              testDate.getDate() === day) {
            return trimmed; // 直接返回，不进行时区转换
          }
        }
        // 支持 ISO 字符串或带 T 的格式，统一转成 yyyy-MM-dd
        const parsed = new Date(trimmed);
        if (!isNaN(parsed.getTime())) {
          const year = parsed.getFullYear();
          const month = String(parsed.getMonth() + 1).padStart(2, '0');
          const day = String(parsed.getDate()).padStart(2, '0');
          return `${year}-${month}-${day}`;
        }
        return trimmed;
      }
      return null;
    },

    /**
     * 验证日期格式是否为 yyyy-MM-dd 格式
     * @param {string} dateStr 日期字符串
     * @returns {boolean} 是否为有效格式
     */
    isValidDateFormat(dateStr) {
      if (!dateStr || typeof dateStr !== 'string') {
        return false;
      }
      // 检查是否为 yyyy-MM-dd 格式（例如：2024-12-25）
      const dateRegex = /^\d{4}-\d{2}-\d{2}$/;
      if (!dateRegex.test(dateStr)) {
        return false;
      }
      // 验证日期是否有效
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) {
        return false;
      }
      // 确保解析后的日期与输入字符串匹配（防止无效日期如 2024-13-45）
      const [year, month, day] = dateStr.split('-').map(Number);
      return date.getFullYear() === year && 
             date.getMonth() + 1 === month && 
             date.getDate() === day;
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
  max-width: 100%;
  margin: 0;
  padding: 0 32px;
  background: transparent;
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

/* 装饰元素 */
.daily-briefing::before {
  content: '◉';
  position: absolute;
  top: 150px;
  right: 100px;
  font-size: 200px;
  color: rgba(155, 89, 182, 0.05);
  z-index: 0;
  pointer-events: none;
  animation: float-decor 4s ease-in-out infinite;
}

@keyframes float-decor {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(180deg); }
}

/* 标题栏样式 - 青春活力渐变 */
.header-bar {
  background: linear-gradient(135deg, rgba(155, 89, 182, 0.15) 0%, rgba(255, 107, 107, 0.15) 50%, rgba(52, 152, 219, 0.15) 100%);
  color: #1A1A1A;
  padding: 32px;
  margin-bottom: 32px;
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(155, 89, 182, 0.2);
  position: relative;
  overflow: hidden;
}

.header-bar::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 107, 107, 0.3) 0%, transparent 70%);
  border-radius: 50%;
  filter: blur(60px);
}

.header-bar::after {
  content: '✨';
  position: absolute;
  top: 20px;
  right: 40px;
  font-size: 32px;
  opacity: 0.4;
  animation: sparkle 2s ease-in-out infinite;
}

@keyframes sparkle {
  0%, 100% { transform: scale(1) rotate(0deg); opacity: 0.4; }
  50% { transform: scale(1.2) rotate(180deg); opacity: 0.8; }
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
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: #1A1A1A;
  line-height: 1.3;
  position: relative;
  z-index: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #FF6B6B 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 过滤卡片 */
.filter-card {
  margin-bottom: 32px;
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  background: #fff;
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
  font-size: 13px;
  font-weight: 500;
  color: #666666;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
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
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border-radius: 24px;
  border: none;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  position: relative;
}

.news-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, #FF6B6B 0%, #9B59B6 50%, #3498DB 100%);
  opacity: 0;
  transition: opacity 0.3s;
}

.news-card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.15);
}

.news-card:hover::before {
  opacity: 1;
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
  font-size: 20px;
  font-weight: 600;
  color: #1A1A1A;
  line-height: 1.5;
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
  gap: 10px;
  margin-bottom: 16px;
}

.summary-icon {
  font-size: 24px;
  animation: rotate 3s linear infinite;
  color: #FF6B6B;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: #606266;
}

.summary-text {
  margin: 0;
  font-size: 15px;
  color: #2C3E50;
  line-height: 1.7;
  text-align: justify;
  padding: 24px 28px;
  background: linear-gradient(135deg, #FFF9E6 0%, #FFF5E6 100%);
  border-radius: 20px;
  border: none;
  box-shadow: 0 4px 16px rgba(243, 156, 18, 0.2);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  /* 对话气泡样式 */
  clip-path: polygon(0% 0%, 100% 0%, 100% 85%, 95% 100%, 0% 100%);
}

.summary-text::before {
  content: '🤖✨';
  position: absolute;
  top: 20px;
  right: 24px;
  font-size: 20px;
  opacity: 0.7;
  animation: bounce 2s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-5px); }
}

.summary-text::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 40px;
  width: 0;
  height: 0;
  border-left: 12px solid transparent;
  border-right: 12px solid transparent;
  border-top: 12px solid #FFF9E6;
  filter: drop-shadow(0 2px 4px rgba(243, 156, 18, 0.2));
}

.news-card:hover .summary-text {
  background: linear-gradient(135deg, #FFF5E6 0%, #FFEED6 100%);
  box-shadow: 0 6px 24px rgba(243, 156, 18, 0.3);
  transform: translateY(-2px);
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

