import { describe, it, expect, vi, beforeEach } from 'vitest';
import { shallowMount, flushPromises } from '@vue/test-utils';

// Mock i18n helpers used by the component
vi.mock('../i18n', () => ({
  t: (key) => key,
  getCurrentLanguage: vi.fn(() => 'en'),
}));

// Mock axios used directly inside the SFC (not the api.js client)
vi.mock('axios', () => ({
  default: {
    get: vi.fn(),
  },
}));

import axios from 'axios';
import DailyBriefingDetail from '../components/vue/DailyBriefingDetail.vue';
import { getCurrentLanguage } from '../i18n';

const stubAll = {
  'el-card': true,
  'el-button': true,
  'el-tag': true,
  'el-alert': true,
};

describe('Feature 1 (Daily Briefing Detail) - UTC-02', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('UTC-02-1: fetchNewsDetail() successfully retrieves news detail', async () => {
    getCurrentLanguage.mockReturnValueOnce('en');

    axios.get.mockResolvedValueOnce({
      status: 200,
      data: {
        success: true,
        data: { id: 123, title: 'Some title', summary: 'Some summary' },
        originalContent: null,
      },
    });

    const wrapper = shallowMount(DailyBriefingDetail, {
      props: { newsId: 123 },
      global: { stubs: stubAll },
    });

    await flushPromises();

    expect(axios.get).toHaveBeenCalledWith('/api/news/daily-briefing/123', {
      params: { lang: 'en' },
    });

    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.error).toBe(null);
    expect(wrapper.vm.news).toEqual({ id: 123, title: 'Some title', summary: 'Some summary' });
  });

  it('UTC-02-2: fetchNewsDetail() handles 404 error when news not found', async () => {
    axios.get.mockRejectedValueOnce({
      response: { status: 404, data: { message: 'not found' } },
    });

    const wrapper = shallowMount(DailyBriefingDetail, {
      props: { newsId: 999 },
      global: { stubs: stubAll },
    });

    await flushPromises();

    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.news).toBe(null);
    expect(wrapper.vm.error).toBe('dailyBriefingDetail.notFound');
  });

  it('UTC-02-3: fetchNewsDetail() includes language parameter (zh)', async () => {
    getCurrentLanguage.mockReturnValueOnce('zh');
    axios.get.mockResolvedValueOnce({
      status: 200,
      data: { success: true, data: { id: 123, title: 't', summary: 's' }, originalContent: null },
    });

    const wrapper = shallowMount(DailyBriefingDetail, {
      props: { newsId: 123 },
      global: { stubs: stubAll },
    });

    await flushPromises();

    expect(wrapper.vm.currentLang).toBe('zh');
    expect(axios.get).toHaveBeenCalledWith('/api/news/daily-briefing/123', {
      params: { lang: 'zh' },
    });
  });

  it('UTC-02-4: fetchNewsDetail() sets originalContent when available (non-Thai)', async () => {
    axios.get.mockResolvedValueOnce({
      status: 200,
      data: {
        success: true,
        data: { id: 123, title: 't', summary: 's' },
        originalContent: 'English content',
      },
    });

    const wrapper = shallowMount(DailyBriefingDetail, {
      props: { newsId: 123 },
      global: { stubs: stubAll },
    });

    await flushPromises();

    expect(wrapper.vm.originalContent).toBe('English content');
  });

  it('UTC-02-5: fetchNewsDetail() sets originalContent to null when content is Thai (backend filters to null)', async () => {
    axios.get.mockResolvedValueOnce({
      status: 200,
      data: {
        success: true,
        data: { id: 123, title: 't', summary: 's' },
        originalContent: null,
      },
    });

    const wrapper = shallowMount(DailyBriefingDetail, {
      props: { newsId: 123 },
      global: { stubs: stubAll },
    });

    await flushPromises();

    expect(wrapper.vm.originalContent).toBe(null);
  });
});


