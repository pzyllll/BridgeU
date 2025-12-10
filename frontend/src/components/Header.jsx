import { useState, useEffect } from 'react';
import { getLanguagePreference, setLanguagePreference } from '../utils/language';
import { t, setLanguage, getCurrentLanguage } from '../i18n';

const Header = () => {
  const [lang, setLang] = useState(getCurrentLanguage());

  useEffect(() => {
    const handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        setLang(e.detail.lang);
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
  }, []);

  const handleLanguageChange = (newLang) => {
    setLanguage(newLang);
    setLang(newLang);
  };

  return (
    <header className="card" style={{ marginBottom: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ flex: 1 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <p className="pill" style={{ marginLeft: 0 }}>
              {t('header.subtitle')}
            </p>
            {/* Language Selector */}
            <div style={{ display: 'flex', gap: '0.25rem' }}>
              <button
                className={`pill ${lang === 'en' ? 'active' : ''}`}
                onClick={() => handleLanguageChange('en')}
                style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem', cursor: 'pointer' }}
              >
                🇺🇸 EN
              </button>
              <button
                className={`pill ${lang === 'zh' ? 'active' : ''}`}
                onClick={() => handleLanguageChange('zh')}
                style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem', cursor: 'pointer' }}
              >
                🇨🇳 中文
              </button>
            </div>
          </div>
          <h1 style={{ marginTop: '0.25rem', marginBottom: '0.5rem' }}>{t('header.title')}</h1>
          <p style={{ color: '#475467' }}>{t('header.description')}</p>
        </div>
        <img
          src="https://cdn.jsdelivr.net/gh/bytebase/blog-demos/assets/travelers.svg"
          alt="students"
          style={{ width: '200px', maxWidth: '40%' }}
        />
      </div>
    </header>
  );
};

export default Header;

