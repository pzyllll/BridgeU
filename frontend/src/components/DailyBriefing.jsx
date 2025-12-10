import { useEffect, useState } from 'react';
import { fetchDailyBriefing } from '../api';
import { getCurrentLanguage, t, setLanguage } from '../i18n';

const DEFAULT_PAGE_SIZE = 5;

const DailyBriefing = () => {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [lang, setLang] = useState(getCurrentLanguage());
  const [pagination, setPagination] = useState({
    totalPages: 0,
    totalElements: 0,
    size: DEFAULT_PAGE_SIZE,
    hasNext: false,
    hasPrevious: false,
  });

  const loadData = async (targetPage = 0) => {
    setLoading(true);
    setError(null);
    try {
      // Get current language from multiple sources to ensure accuracy
      const savedLang = localStorage.getItem('userLanguage');
      const currentLang = getCurrentLanguage();
      const langToUse = lang || savedLang || currentLang || 'en';
      
      console.log('=== DailyBriefing: Loading data ===');
      console.log('  - lang state:', lang);
      console.log('  - localStorage userLanguage:', savedLang);
      console.log('  - currentLang from i18n:', currentLang);
      console.log('  - final langToUse:', langToUse);
      console.log('  - page:', targetPage, 'size:', pagination.size);
      
      const data = await fetchDailyBriefing({ page: targetPage, size: pagination.size, lang: langToUse });
      console.log('DailyBriefing: Data loaded:', data.data?.length || 0, 'items');
      
      // Log first item details for debugging
      if (data.data && data.data.length > 0) {
        const firstItem = data.data[0];
        console.log('DailyBriefing: First item details:', {
          id: firstItem.id,
          title: firstItem.title,
          titleZh: firstItem.titleZh ? firstItem.titleZh.substring(0, 50) + '...' : null,
          titleEn: firstItem.titleEn ? firstItem.titleEn.substring(0, 50) + '...' : null,
          summary: firstItem.summary ? firstItem.summary.substring(0, 50) + '...' : null,
          summaryZh: firstItem.summaryZh ? firstItem.summaryZh.substring(0, 50) + '...' : null,
          summaryEn: firstItem.summaryEn ? firstItem.summaryEn.substring(0, 50) + '...' : null,
          requestedLang: langToUse,
          displayedTitle: firstItem.title?.substring(0, 50),
          displayedSummary: firstItem.summary?.substring(0, 50)
        });
      }
      
      setItems(data.data || []);
      setPagination({
        totalPages: data.pagination?.totalPages ?? 0,
        totalElements: data.pagination?.totalElements ?? 0,
        size: data.pagination?.size ?? DEFAULT_PAGE_SIZE,
        hasNext: data.pagination?.hasNext ?? false,
        hasPrevious: data.pagination?.hasPrevious ?? false,
      });
      setPage(data.pagination?.page ?? targetPage);
    } catch (err) {
      // 过滤掉浏览器扩展相关的错误
      const isExtensionError = err.message && (
        err.message.includes('content-all.js') ||
        err.message.includes('chrome-extension') ||
        err.message.includes('moz-extension')
      );
      
      if (isExtensionError) {
        console.warn('⚠️ 检测到浏览器扩展相关错误，已忽略:', err.message);
        // 如果是扩展错误，不显示错误信息，继续尝试加载
        setError(null);
        setLoading(false);
        return;
      }
      
      console.error('DailyBriefing: Failed to load data:', err);
      console.error('DailyBriefing: Error details:', {
        message: err.message,
        response: err.response,
        request: err.request,
        config: err.config
      });
      
      // 提供更详细的错误信息
      let errorMessage = 'Failed to fetch news, please try again later';
      if (err.message === 'Network Error' || err.message.includes('Failed to fetch')) {
        errorMessage = '无法连接到服务器，请检查后端服务是否运行 (http://localhost:8080)';
      } else if (err.response) {
        errorMessage = `服务器错误: ${err.response.status} ${err.response.statusText}`;
      } else if (err.request) {
        errorMessage = '请求已发送但未收到响应，请检查网络连接';
      }
      
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Initialize language and load data
  useEffect(() => {
    // Force re-initialize language from localStorage
    const savedLang = localStorage.getItem('userLanguage');
    console.log('DailyBriefing: Initial load, localStorage userLanguage:', savedLang);
    
    // Get current language from i18n (which reads from localStorage)
    const currentLang = getCurrentLanguage();
    console.log('DailyBriefing: Initial load, currentLang from i18n:', currentLang);
    
    // Use savedLang if available, otherwise use currentLang
    const langToSet = (savedLang === 'zh' || savedLang === 'en') ? savedLang : (currentLang || 'en');
    console.log('DailyBriefing: Setting lang state to:', langToSet);
    setLang(langToSet);
    
    // Also update i18n if needed
    if (savedLang && savedLang !== currentLang && (savedLang === 'zh' || savedLang === 'en')) {
      setLanguage(savedLang);
    }
    
    // Load data with the correct language
    loadData(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Listen for language changes and reload data
  useEffect(() => {
    const handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        const newLang = e.detail.lang;
        console.log('DailyBriefing: Language changed to:', newLang);
        setLang(newLang);
        // Reload data with new language
        setTimeout(() => {
          loadData(page);
        }, 100);
      }
    };
    
    if (typeof window !== 'undefined') {
      window.addEventListener('languageChanged', handleLanguageChange);
      return () => {
        if (typeof window !== 'undefined') {
          window.removeEventListener('languageChanged', handleLanguageChange);
        }
      };
    }
  }, [page]);

  // Reload data when language changes
  useEffect(() => {
    if (lang) {
      loadData(page);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lang]);

  const handlePrev = () => {
    if (page <= 0) return;
    loadData(page - 1);
  };

  const handleNext = () => {
    if (page + 1 >= pagination.totalPages) return;
    loadData(page + 1);
  };

  return (
    <div className="briefing-card">
      <div className="robot-badge">
        <span>🤖</span> INTELLIGENCE ROBOT
      </div>
      
      <div style={{ display: 'flex', gap: '1rem' }}>
        <div className="robot-icon" style={{ animation: 'spin-slow 3s linear infinite' }}>
          🔄
        </div>
        <div style={{ flex: 1 }}>
          <h3 className="briefing-title">{t('briefing.title')}</h3>
          <div className="briefing-source">
            {t('briefing.source')} · {t('briefing.totalItems', { count: pagination.totalElements })}
          </div>

          {loading && <p>{t('briefing.loading')}</p>}
          {error && <p style={{ color: '#dc2626' }}>{t('briefing.error')}</p>}

          {!loading && !error && items.length === 0 && <p>{t('briefing.noNews')}</p>}

          {!loading && !error && items.length > 0 && (
            <div style={{ marginTop: '0.5rem' }}>
              {items.slice(0, 3).map((news) => {
                console.log('DailyBriefing: Rendering news item:', {
                  id: news.id,
                  title: news.title,
                  titleZh: news.titleZh,
                  titleEn: news.titleEn,
                  summary: news.summary?.substring(0, 50),
                  summaryZh: news.summaryZh?.substring(0, 50),
                  summaryEn: news.summaryEn?.substring(0, 50),
                  currentLang: lang
                });
                return (
                  <div key={news.id} className="briefing-item">
                    <strong>{news.title}</strong>
                    {news.summary && (
                      <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                        {news.summary.substring(0, 100)}...
                      </div>
                    )}
                    <span style={{ marginLeft: '0.5rem', fontSize: '12px', color: '#666' }}>
                      - {news.source || 'Unknown Source'}
                    </span>
                    {news.originalUrl && (
                      <a
                        href={news.originalUrl}
                        target="_blank"
                        rel="noreferrer"
                        style={{ marginLeft: '0.5rem', color: '#2563eb', fontSize: '12px' }}
                      >
                        {t('briefing.readOriginal')}
                      </a>
                    )}
                  </div>
                );
              })}
            </div>
          )}

          {pagination.totalPages > 1 && (
            <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
              <button className="btn" disabled={page === 0} onClick={handlePrev} style={{ padding: '4px 8px', fontSize: '12px' }}>
                {t('briefing.previous')}
              </button>
              <span style={{ fontSize: '12px', lineHeight: '24px' }}>
                {page + 1}/{pagination.totalPages}
              </span>
              <button className="btn" disabled={page + 1 >= pagination.totalPages} onClick={handleNext} style={{ padding: '4px 8px', fontSize: '12px' }}>
                {t('briefing.next')}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DailyBriefing;

