<template>
  <div class="daily-briefing">
    <!-- 顶部标题栏 -->
    <div class="header-bar">
      <h2 class="title">Intelligence Robot</h2>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-card shadow="never">
        <div class="loading-content">
          <i class="el-icon-loading"></i>
          <span>加载中...</span>
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
      <el-card
        v-for="news in newsList"
        :key="news.id"
        class="news-card"
        shadow="hover">
        <div slot="header" class="card-header">
          <h3 class="news-title">{{ news.title }}</h3>
        </div>
        
        <div class="news-content">
          <!-- AI 中文总结 -->
          <div class="summary-section">
            <div class="section-label">AI 总结：</div>
            <p class="summary-text">{{ news.summary || '暂无总结' }}</p>
          </div>

          <!-- 来源和发布时间 -->
          <div class="meta-info">
            <div class="meta-item">
              <i class="el-icon-link"></i>
              <span class="label">来源：</span>
              <span class="value">{{ news.source || '未知' }}</span>
            </div>
            <div class="meta-item">
              <i class="el-icon-time"></i>
              <span class="label">发布时间：</span>
              <span class="value">{{ formatDate(news.publishDate) || formatDate(news.createTime) }}</span>
            </div>
          </div>

          <!-- Read Original 链接 -->
          <div class="action-section">
            <el-button
              type="primary"
              size="small"
              icon="el-icon-view"
              @click="openOriginalUrl(news.originalUrl)"
              :disabled="!news.originalUrl">
              Read Original
            </el-button>
          </div>
        </div>
      </el-card>

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
      <el-card shadow="never">
        <el-empty description="暂无新闻数据"></el-empty>
      </el-card>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

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
      }
    };
  },
  mounted() {
    this.fetchDailyBriefing();
  },
  methods: {
    /**
     * 获取当天新闻简报
     */
    async fetchDailyBriefing() {
      this.loading = true;
      this.error = null;

      try {
        // 计算页码（从 0 开始）
        const page = this.currentPage - 1;
        
        const response = await axios.get('/api/news/daily-briefing', {
          params: {
            page: page,
            size: this.pageSize
          }
        });

        if (response.data.success) {
          this.newsList = response.data.data || [];
          this.pagination = {
            totalElements: response.data.pagination.totalElements || 0,
            totalPages: response.data.pagination.totalPages || 0,
            hasNext: response.data.pagination.hasNext || false,
            hasPrevious: response.data.pagination.hasPrevious || false
          };
        } else {
          this.error = response.data.message || '获取新闻失败';
        }
      } catch (error) {
        console.error('获取新闻简报失败:', error);
        this.error = error.response?.data?.message || error.message || '网络请求失败，请稍后重试';
      } finally {
        this.loading = false;
      }
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
     * 打开原文链接
     */
    openOriginalUrl(url) {
      if (url) {
        window.open(url, '_blank');
      }
    },

    /**
     * 格式化日期
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

      return `${year}-${month}-${day} ${hours}:${minutes}`;
    }
  }
};
</script>

<style scoped>
.daily-briefing {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 标题栏样式 */
.header-bar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 30px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.header-bar .title {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 1px;
}

/* 加载状态 */
.loading-container,
.error-container,
.empty-container {
  margin-bottom: 20px;
}

.loading-content {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #909399;
}

.loading-content i {
  font-size: 24px;
  margin-right: 10px;
  animation: rotating 2s linear infinite;
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

.news-card {
  transition: all 0.3s ease;
  border-radius: 8px;
}

.news-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

/* 卡片头部 */
.card-header {
  padding: 0;
}

.news-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  line-height: 1.5;
}

/* 新闻内容 */
.news-content {
  padding: 10px 0;
}

/* 总结部分 */
.summary-section {
  margin-bottom: 20px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.summary-text {
  margin: 0;
  font-size: 15px;
  color: #606266;
  line-height: 1.8;
  text-align: justify;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

/* 元信息 */
.meta-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 15px;
  padding: 12px;
  background-color: #fafafa;
  border-radius: 4px;
}

.meta-item {
  display: flex;
  align-items: center;
  font-size: 14px;
  color: #909399;
}

.meta-item i {
  margin-right: 6px;
  font-size: 16px;
  color: #c0c4cc;
}

.meta-item .label {
  margin-right: 4px;
  font-weight: 500;
}

.meta-item .value {
  color: #606266;
}

/* 操作按钮 */
.action-section {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .daily-briefing {
    padding: 15px;
  }

  .header-bar {
    padding: 15px 20px;
  }

  .header-bar .title {
    font-size: 22px;
  }

  .news-title {
    font-size: 18px;
  }

  .meta-info {
    flex-direction: column;
    gap: 10px;
  }

  .action-section {
    justify-content: center;
  }
}
</style>

