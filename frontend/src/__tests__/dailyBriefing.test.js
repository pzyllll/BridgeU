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
import DailyBriefing from '../components/vue/DailyBriefing.vue';
import { getCurrentLanguage } from '../i18n';

const stubAll = {
  // Element Plus stubs (we only test methods / state)
  'el-card': true,
  'el-input': true,
  'el-button': true,
  'el-date-picker': true,
  'el-pagination': true,
  'el-tag': true,
  'el-alert': true,
  'el-empty': true,
  'transition-group': true,
};

describe('Feature 1 (Daily Briefing) - UTC-01~06', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Default: avoid real scrolling in tests
    vi.stubGlobal('scrollTo', vi.fn());
  });

  it('UTC-01-1: fetchDailyBriefing() default params (page/size/lang) + success updates state', async () => {
    getCurrentLanguage.mockReturnValueOnce('en');

    const apiPayload = {
      success: true,
      data: [{ id: 1, title: 't1', summary: 's1' }],
      pagination: { totalElements: 11, totalPages: 2, hasNext: true, hasPrevious: false },
    };
    axios.get.mockResolvedValueOnce({ status: 200, data: apiPayload });

    const wrapper = shallowMount(DailyBriefing, {
      global: {
        stubs: stubAll,
        mocks: { $message: { warning: vi.fn() } },
      },
    });

    await flushPromises();

    expect(axios.get).toHaveBeenCalledWith('/api/news/daily-briefing', {
      params: { page: 0, size: 10, lang: 'en' },
    });

    expect(wrapper.vm.loading).toBe(false);
    expect(wrapper.vm.error).toBe(null);
    expect(wrapper.vm.newsList).toEqual(apiPayload.data);
    expect(wrapper.vm.pagination.totalElements).toBe(11);
    expect(wrapper.vm.pagination.totalPages).toBe(2);
  });

  it('UTC-01-2: fetchDailyBriefing() handles success=false without clearing previous newsList', async () => {
    axios.get
      .mockResolvedValueOnce({
        status: 200,
        data: { success: true, data: [{ id: 1, title: 'prev' }], pagination: { totalElements: 1, totalPages: 1 } },
      })
      .mockResolvedValueOnce({
        status: 200,
        data: { success: false, message: 'Error message' },
      });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    // Trigger again -> success=false branch
    await wrapper.vm.fetchDailyBriefing();
    await flushPromises();

    expect(wrapper.vm.error).toBe('Error message');
    // Component logic: does not reset newsList on success=false
    expect(wrapper.vm.newsList).toEqual([{ id: 1, title: 'prev' }]);
    expect(wrapper.vm.loading).toBe(false);
  });

  it('UTC-01-3: fetchDailyBriefing() includes keyword when searchKeyword is set', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    axios.get.mockClear();
    wrapper.vm.searchKeyword = ' Thailand ';

    await wrapper.vm.fetchDailyBriefing();
    await flushPromises();

    expect(axios.get).toHaveBeenCalledWith('/api/news/daily-briefing', {
      params: { page: 0, size: 10, keyword: 'Thailand', lang: 'en' },
    });
  });

  it('UTC-01-4: fetchDailyBriefing() includes startDate/endDate when filters are provided', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    axios.get.mockClear();
    wrapper.vm.filterStartDate = new Date('2026-01-01T00:00:00');
    wrapper.vm.filterEndDate = new Date('2026-01-31T00:00:00');

    await wrapper.vm.fetchDailyBriefing();
    await flushPromises();

    expect(axios.get).toHaveBeenCalledWith('/api/news/daily-briefing', {
      params: { page: 0, size: 10, startDate: '2026-01-01', endDate: '2026-01-31', lang: 'en' },
    });
  });

  it('UTC-01-5: fetchDailyBriefing() includes lang=zh when currentLang is zh', async () => {
    getCurrentLanguage.mockReturnValueOnce('zh');
    axios.get.mockResolvedValueOnce({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    expect(wrapper.vm.currentLang).toBe('zh');
    expect(axios.get).toHaveBeenCalledWith('/api/news/daily-briefing', {
      params: { page: 0, size: 10, lang: 'zh' },
    });
  });

  it('UTC-03: handleSearch() resets currentPage=1 and triggers fetchDailyBriefing()', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    const spy = vi.spyOn(wrapper.vm, 'fetchDailyBriefing').mockResolvedValue();
    wrapper.vm.currentPage = 5;

    wrapper.vm.handleSearch();

    expect(wrapper.vm.currentPage).toBe(1);
    expect(spy).toHaveBeenCalled();
  });

  it('UTC-04-1: applyFilters() validates invalid date range and does not fetch', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const warn = vi.fn();
    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: warn } } },
    });
    await flushPromises();

    const spy = vi.spyOn(wrapper.vm, 'fetchDailyBriefing').mockResolvedValue();
    wrapper.vm.filterStartDate = new Date('2026-01-31T00:00:00');
    wrapper.vm.filterEndDate = new Date('2026-01-01T00:00:00');

    wrapper.vm.applyFilters();

    expect(warn).toHaveBeenCalled();
    expect(spy).not.toHaveBeenCalled();
  });

  it('UTC-05: resetFilters() clears keyword/dates, resets page, and triggers fetchDailyBriefing()', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    const spy = vi.spyOn(wrapper.vm, 'fetchDailyBriefing').mockResolvedValue();
    wrapper.vm.searchKeyword = 'test';
    wrapper.vm.filterStartDate = new Date('2026-01-01T00:00:00');
    wrapper.vm.filterEndDate = new Date('2026-01-02T00:00:00');
    wrapper.vm.currentPage = 5;

    wrapper.vm.resetFilters();

    expect(wrapper.vm.searchKeyword).toBe('');
    expect(wrapper.vm.filterStartDate).toBe(null);
    expect(wrapper.vm.filterEndDate).toBe(null);
    expect(wrapper.vm.currentPage).toBe(1);
    expect(spy).toHaveBeenCalled();
  });

  it('UTC-06: handlePageChange(page) updates currentPage, triggers fetch, and scrolls to top', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    const spy = vi.spyOn(wrapper.vm, 'fetchDailyBriefing').mockResolvedValue();
    const scrollSpy = vi.spyOn(window, 'scrollTo');

    wrapper.vm.handlePageChange(3);
    await wrapper.vm.$nextTick();

    expect(wrapper.vm.currentPage).toBe(3);
    expect(spy).toHaveBeenCalled();
    expect(scrollSpy).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' });
  });

  it('UTC-07: normalizeDateValue(value) normalizes Date and string inputs', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    // Date -> yyyy-MM-dd
    const d = new Date(2026, 0, 2); // local time: 2026-01-02
    expect(wrapper.vm.normalizeDateValue(d)).toBe('2026-01-02');

    // Valid yyyy-MM-dd string -> returns as-is
    expect(wrapper.vm.normalizeDateValue('2026-12-25')).toBe('2026-12-25');

    // Placeholder / invalid input with letters -> null
    expect(wrapper.vm.normalizeDateValue('yyyy-01-Tu')).toBe(null);
  });

  it('UTC-08: formatDate(date) returns dd-MM-yyyy HH:mm', async () => {
    axios.get.mockResolvedValue({ status: 200, data: { success: true, data: [], pagination: { totalElements: 0, totalPages: 0 } } });

    const wrapper = shallowMount(DailyBriefing, {
      global: { stubs: stubAll, mocks: { $message: { warning: vi.fn() } } },
    });
    await flushPromises();

    const d = new Date(2026, 0, 2, 3, 4); // local
    expect(wrapper.vm.formatDate(d)).toBe('02-01-2026 03:04');
    expect(wrapper.vm.formatDate('invalid-date')).toBe('');
  });
});


