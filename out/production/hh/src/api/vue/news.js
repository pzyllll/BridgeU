import axios from 'axios';

// 配置 axios 基础 URL（根据实际情况修改）
const api = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000
});

/**
 * 获取当天新闻简报
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（从 0 开始）
 * @param {number} params.size - 每页大小
 * @returns {Promise} API 响应
 */
export function getDailyBriefing(params = {}) {
  return api.get('/api/news/daily-briefing', { params });
}

export default {
  getDailyBriefing
};

