import { useState, useEffect } from 'react';
import { t, getCurrentLanguage, setLanguage } from '../i18n';

const Sidebar = ({ currentPage, onNavigate, isAdmin = false, user = null }) => {
  const [lang, setLang] = useState(getCurrentLanguage());

  useEffect(() => {
    const handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        setLang(e.detail.lang);
      }
    };

  const switchLanguage = (newLang) => {
    setLanguage(newLang);
    setLang(newLang);
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

  const navItems = [
    { id: 'home', label: t('sidebar.communityFeed'), icon: '🏠' },
    { id: 'post', label: t('sidebar.newPost'), icon: '➕' },
    { id: 'search', label: t('sidebar.semanticSearch'), icon: '🔍' },
    { id: 'assistant', label: t('sidebar.aiAssistant'), icon: '🤖' },
  ];

  return (
    <aside className="sidebar">
      {/* Logo */}
      <div className="sidebar-logo">
        <div className="logo-icon">🌐</div>
        <span>BridgeU</span>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        <div className="nav-section-title">{t('sidebar.platform')}</div>
        {navItems.map((item) => (
          <div
            key={item.id}
            className={`nav-item ${currentPage === item.id ? 'active' : ''}`}
            onClick={() => onNavigate(item.id)}
          >
            <span>{item.icon}</span>
            <span>{item.label}</span>
          </div>
        ))}

        {/* Admin Entry */}
        {isAdmin && (
          <>
            <div className="nav-section-title" style={{ marginTop: '1rem' }}>{t('sidebar.admin')}</div>
            <div
              className={`nav-item ${currentPage === 'admin' ? 'active' : ''}`}
              onClick={() => onNavigate('admin')}
              style={{ background: currentPage === 'admin' ? '#ffd700' : 'transparent' }}
            >
              <span>🔧</span>
              <span>{t('sidebar.adminPanel')}</span>
            </div>
          </>
        )}

        <div style={{ marginTop: 'auto', borderTop: '2px solid #333' }}>
          {user && (
            <div style={{ padding: '0.5rem', fontFamily: 'monospace', fontSize: '0.8rem', color: '#666' }}>
              {user.displayName || user.username}
              {isAdmin && <span className="pill active" style={{ marginLeft: '0.5rem', fontSize: '0.6rem' }}>{t('sidebar.admin')}</span>}
            </div>
          )}
          <div
            className={`nav-item ${currentPage === 'profile' ? 'active' : ''}`}
            onClick={() => onNavigate('profile')}
          >
            <span>👤</span>
            <span>{t('sidebar.profile')}</span>
          </div>
          <div
            className="nav-item"
            onClick={() => onNavigate('logout')}
            style={{ color: '#666' }}
          >
            <span>🚪</span>
            <span>{t('sidebar.logout')}</span>
          </div>
        </div>
      </nav>
    </aside>
  );
};

export default Sidebar;
