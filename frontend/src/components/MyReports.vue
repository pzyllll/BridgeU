<template>
  <div class="my-reports">
    <div class="reports-header">
      <button class="btn btn-back" @click="$emit('back')">
        ← {{ t('back') }}
      </button>
      <h1 class="page-title">{{ t('postDetail.myReports') }}</h1>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>{{ t('postDetail.loadingReports') }}</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-container">
      <div class="error-message">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="loadReports">
          {{ t('common.retry') }}
        </button>
      </div>
    </div>

    <!-- Reports List -->
    <div v-else-if="reports && reports.length > 0" class="reports-list">
      <div 
        v-for="report in reports" 
        :key="report.id"
        class="report-card"
      >
        <div class="report-header">
          <div class="report-meta">
            <span class="report-target-badge" :class="getTargetTypeClass(report.targetType)">
              {{ getTargetTypeText(report.targetType) }}
            </span>
            <span class="report-status-badge" :class="getStatusClass(report.status)">
              {{ getStatusText(report.status) }}
            </span>
          </div>
          <div class="report-date">
            {{ formatDate(report.createdAt) }}
          </div>
        </div>

        <div class="report-content">
          <div class="report-field">
            <span class="field-label">{{ t('postDetail.reportTarget') }} ID:</span>
            <span class="field-value">{{ report.targetId }}</span>
          </div>

          <div class="report-field" v-if="report.reasons && report.reasons.length > 0">
            <span class="field-label">{{ t('postDetail.reportReasons') }}:</span>
            <div class="reasons-list">
              <span 
                v-for="(reason, index) in report.reasons" 
                :key="index"
                class="reason-badge"
              >
                {{ reason }}
              </span>
            </div>
          </div>

          <div class="report-field" v-if="report.description">
            <span class="field-label">{{ t('postDetail.reportDescription') }}:</span>
            <p class="field-value">{{ report.description }}</p>
          </div>

          <!-- Review Information -->
          <div v-if="report.status !== 'PENDING'" class="review-section">
            <div class="review-header">
              <h3>{{ t('postDetail.reviewNotes') }}</h3>
              <span v-if="report.reviewedAt" class="review-date">
                {{ t('postDetail.reviewDate') }}: {{ formatDate(report.reviewedAt) }}
              </span>
            </div>

            <div v-if="report.isViolation !== null" class="violation-status">
              <span :class="['violation-badge', report.isViolation ? 'violation-found' : 'no-violation']">
                {{ report.isViolation ? t('postDetail.violationFound') : t('postDetail.noViolation') }}
              </span>
              <span v-if="report.aiConfidence" class="confidence-score">
                {{ t('postDetail.aiConfidence') }}: {{ (report.aiConfidence * 100).toFixed(1) }}%
              </span>
            </div>

            <div v-if="report.violationSnippet" class="violation-snippet">
              <strong>{{ t('postDetail.violationSnippet') }}:</strong>
              <p>{{ report.violationSnippet }}</p>
            </div>

            <div v-if="report.reviewNotes" class="review-notes">
              <p>{{ report.reviewNotes }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-icon">📋</div>
      <p class="empty-message">{{ t('postDetail.noReports') }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { getMyReports } from '../api';
import { getCurrentLanguage, t } from '../i18n';
import { parseBackendDate, formatBangkokAbsolute } from '../utils/datetime';

const props = defineProps({
  token: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['back']);

const reports = ref([]);
const loading = ref(true);
const error = ref(null);
const lang = ref(getCurrentLanguage());

const loadReports = async () => {
  loading.value = true;
  error.value = null;

  try {
    const data = await getMyReports(props.token);
    reports.value = Array.isArray(data) ? data : [];
    console.log('Loaded reports:', reports.value);
  } catch (err) {
    console.error('Failed to load reports:', err);
    error.value = err.response?.data?.message || err.message || t('postDetail.loadReportsFailed');
  } finally {
    loading.value = false;
  }
};

const getTargetTypeClass = (targetType) => {
  return targetType === 'POST' ? 'target-post' : 'target-comment';
};

const getTargetTypeText = (targetType) => {
  return targetType === 'POST' 
    ? t('postDetail.reportTargetPost') 
    : t('postDetail.reportTargetComment');
};

const getStatusClass = (status) => {
  const statusMap = {
    'PENDING': 'status-pending',
    'REVIEWED': 'status-reviewed',
    'RESOLVED': 'status-resolved',
    'DISMISSED': 'status-dismissed'
  };
  return statusMap[status] || 'status-unknown';
};

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': t('postDetail.reportStatusPending'),
    'REVIEWED': t('postDetail.reportStatusReviewed'),
    'RESOLVED': t('postDetail.reportStatusResolved'),
    'DISMISSED': t('postDetail.reportStatusDismissed')
  };
  return statusMap[status] || status;
};

const formatDate = (timestamp) => {
  if (!timestamp) {
    return lang.value === 'zh' ? '未知时间' : 'Unknown time';
  }
  
  const date = parseBackendDate(timestamp);
  if (!date) {
    return lang.value === 'zh' ? '无效时间' : 'Invalid time';
  }
  
  return formatBangkokAbsolute(date) || (lang.value === 'zh' ? '无效时间' : 'Invalid time');
};

onMounted(() => {
  loadReports();
});
</script>

<style scoped>
.my-reports {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.reports-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.btn-back {
  background: transparent;
  border: 1px solid #dcdfe6;
  color: #606266;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
}

.btn-back:hover {
  background: #f5f7fa;
  border-color: #c0c4cc;
}

.page-title {
  font-size: 28px;
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

.loading-content,
.error-message {
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

.reports-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.report-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.report-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.report-target-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.target-post {
  background: #e1f3ff;
  color: #1890ff;
}

.target-comment {
  background: #f0f9ff;
  color: #096dd9;
}

.report-status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-reviewed {
  background: #d1ecf1;
  color: #0c5460;
}

.status-resolved {
  background: #d4edda;
  color: #155724;
}

.status-dismissed {
  background: #f8d7da;
  color: #721c24;
}

.report-date {
  font-size: 14px;
  color: #909399;
}

.report-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

.field-value {
  color: #303133;
  font-size: 14px;
}

.reasons-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.reason-badge {
  padding: 4px 10px;
  background: #f0f2f5;
  color: #606266;
  border-radius: 4px;
  font-size: 12px;
}

.review-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.review-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.review-date {
  font-size: 12px;
  color: #909399;
}

.violation-status {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}

.violation-badge {
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
}

.violation-found {
  background: #fef0f0;
  color: #f56c6c;
}

.no-violation {
  background: #f0f9ff;
  color: #409eff;
}

.confidence-score {
  font-size: 13px;
  color: #606266;
}

.violation-snippet {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border-left: 3px solid #e6a23c;
}

.violation-snippet strong {
  display: block;
  margin-bottom: 8px;
  color: #606266;
}

.violation-snippet p {
  margin: 0;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
}

.review-notes {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.review-notes p {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-message {
  font-size: 16px;
  color: #909399;
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

.btn-primary:hover {
  background: var(--color-primary-dark);
}

@media (max-width: 768px) {
  .my-reports {
    padding: 16px;
  }

  .report-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .review-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>

