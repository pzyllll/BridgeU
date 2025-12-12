import { useState, useEffect } from 'react';
import LoginPage from './components/LoginPage';
import Sidebar from './components/Sidebar';
import PostList from './components/PostList';
import PostDetail from './components/PostDetail';
import SearchPanel from './components/SearchPanel';
import DailyBriefing from './components/DailyBriefing';
import NlpAssistant from './components/NlpAssistant';
import NewPostForm from './components/NewPostForm';
import AdminPanel from './components/AdminPanel';
import { fetchMyRejectedPosts } from './api';
import { setLanguagePreference, getLanguagePreference } from './utils/language';
import { setLanguage, getCurrentLanguage } from './i18n';

// 从 localStorage 恢复登录状态
const getInitialAuthState = () => {
  try {
    const savedToken = localStorage.getItem('token');
    const savedUser = localStorage.getItem('user');
    if (savedToken && savedUser) {
      return {
        isLoggedIn: true,
        token: savedToken,
        user: JSON.parse(savedUser)
      };
    }
  } catch (e) {
    console.error('Failed to restore auth state:', e);
  }
  return { isLoggedIn: false, token: null, user: null };
};

const App = () => {
  const initialAuth = getInitialAuthState();
  const [isLoggedIn, setIsLoggedIn] = useState(initialAuth.isLoggedIn);
  const [currentPage, setCurrentPage] = useState('home');
  const [selectedPostId, setSelectedPostId] = useState(null);
  const [user, setUser] = useState(initialAuth.user);
  const [token, setToken] = useState(initialAuth.token);
  const [lang, setLang] = useState(getCurrentLanguage());
  const [selectedTag, setSelectedTag] = useState('all');
  const [rejectedPosts, setRejectedPosts] = useState([]);

  // 监听语言变化事件，强制重新渲染整个应用
  useEffect(() => {
    const handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        console.log('App: Language changed to:', e.detail.lang);
        setLang(e.detail.lang);
      }
    };
    
    window.addEventListener('languageChanged', handleLanguageChange);
    return () => {
      window.removeEventListener('languageChanged', handleLanguageChange);
    };
  }, []);

  // 页面关闭时清除登录状态（可选：如果希望关闭浏览器标签页时登出）
  useEffect(() => {
    // 标记当前会话为活跃
    sessionStorage.setItem('sessionActive', 'true');
    
    // 监听页面关闭事件
    const handleBeforeUnload = () => {
      // 检查是否是刷新（sessionStorage 在刷新时保留）
      // 如果 sessionStorage 中没有 sessionActive，说明是新标签页
    };
    
    // 检查是否是从关闭的标签页恢复（新会话）
    const wasSessionActive = sessionStorage.getItem('sessionActive');
    if (!wasSessionActive && initialAuth.isLoggedIn) {
      // 这是一个新会话（浏览器重新打开），清除登录状态
      console.log('App: New session detected, clearing auth state');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      setIsLoggedIn(false);
      setUser(null);
      setToken(null);
    }
    
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, []);

  // 恢复登录后设置语言
  useEffect(() => {
    if (isLoggedIn && user?.preferredLanguage) {
      const userLang = user.preferredLanguage;
      if (userLang === 'zh' || userLang === 'en') {
        setLanguagePreference(userLang);
        setLanguage(userLang);
      }
    }
  }, [isLoggedIn, user]);

  const handleLogin = (userData, authToken) => {
    console.log('App: handleLogin called, userData:', userData);
    setIsLoggedIn(true);
    setUser(userData);
    setToken(authToken);
    // 保存 token 到 localStorage，方便其他工具使用
    if (authToken) {
      localStorage.setItem('token', authToken);
      console.log('App: Token saved to localStorage');
    }
    if (userData) {
      localStorage.setItem('user', JSON.stringify(userData));
    }
    setCurrentPage('home');
    
    // 从用户信息中读取语言偏好并设置（作为备用，LoginPage 应该已经设置了）
    if (userData && userData.preferredLanguage) {
      const userLang = userData.preferredLanguage;
      console.log('App: User preferredLanguage:', userLang);
      if (userLang === 'zh' || userLang === 'en') {
        console.log('App: Setting language to:', userLang);
        setLanguagePreference(userLang);
        setLanguage(userLang);
        console.log('App: Language set, localStorage:', localStorage.getItem('userLanguage'));
      }
    } else {
      console.warn('App: No preferredLanguage in userData, checking localStorage...');
      const savedLang = localStorage.getItem('userLanguage');
      if (savedLang && (savedLang === 'zh' || savedLang === 'en')) {
        console.log('App: Using saved language from localStorage:', savedLang);
        setLanguage(savedLang);
      }
    }
  };

  const handleNavigate = (page, postId = null) => {
    if (page === 'logout') {
      setIsLoggedIn(false);
      setUser(null);
      setToken(null);
      // 清除 localStorage 和 sessionStorage 中的登录数据
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      sessionStorage.removeItem('sessionActive');
      return;
    }
    if (page === 'postDetail' && postId) {
      setSelectedPostId(postId);
      setCurrentPage('postDetail');
    } else {
      setCurrentPage(page);
      setSelectedPostId(null);
    }
  };

  const isAdmin = user?.role === 'ADMIN';

  // 加载当前用户被拒绝的帖子（用于个人页面显示审核结果）
  useEffect(() => {
    const loadRejected = async () => {
      try {
        if (!token) return;
        const data = await fetchMyRejectedPosts();
        setRejectedPosts(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error('Failed to load rejected posts', e);
      }
    };
    loadRejected();
  }, [token]);

  const renderMainContent = () => {
    switch (currentPage) {
      case 'home':
        return (
          <>
            <DailyBriefing key={`briefing-${lang}`} />
            <div className="section-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>{lang === 'zh' ? '社区动态' : 'Community Feed'}</span>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <span
                  className={`pill ${selectedTag === 'all' ? 'active' : ''}`}
                  onClick={() => setSelectedTag('all')}
                  style={{ cursor: 'pointer' }}
                >
                  {lang === 'zh' ? '全部' : 'All'}
                </span>
                <span
                  className={`pill ${selectedTag === 'rent' ? 'active' : ''}`}
                  onClick={() => setSelectedTag('rent')}
                  style={{ cursor: 'pointer' }}
                >
                  🏠 #{lang === 'zh' ? '租房' : 'Rent'}
                </span>
                <span
                  className={`pill ${selectedTag === 'learning' ? 'active' : ''}`}
                  onClick={() => setSelectedTag('learning')}
                  style={{ cursor: 'pointer' }}
                >
                  📚 #{lang === 'zh' ? '学习' : 'Learning'}
                </span>
                <span
                  className={`pill ${selectedTag === 'market' ? 'active' : ''}`}
                  onClick={() => setSelectedTag('market')}
                  style={{ cursor: 'pointer' }}
                >
                  🛒 #{lang === 'zh' ? '市场' : 'Market'}
                </span>
                <span
                  className={`pill ${selectedTag === 'visa' ? 'active' : ''}`}
                  onClick={() => setSelectedTag('visa')}
                  style={{ cursor: 'pointer' }}
                >
                  🛂 #{lang === 'zh' ? '签证' : 'Visa'}
                </span>
                <span
                  className={`pill ${selectedTag === 'food' ? 'active' : ''}`}
                  onClick={() => setSelectedTag('food')}
                  style={{ cursor: 'pointer' }}
                >
                  🍜 #{lang === 'zh' ? '美食' : 'Food'}
                </span>
              </div>
            </div>
            <PostList
              key={`posts-${lang}`}
              onPostClick={(postId) => handleNavigate('postDetail', postId)}
              selectedTag={selectedTag}
            />
          </>
        );
      case 'post':
        return <NewPostForm key={`newpost-${lang}`} currentUserId={user?.id} />;
      case 'search':
        return <SearchPanel key={`search-${lang}`} />;
      case 'assistant':
        return <NlpAssistant key={`assistant-${lang}`} />;
      case 'admin':
        return isAdmin ? <AdminPanel key={`admin-${lang}`} token={token} /> : <div className="card">{lang === 'zh' ? '无权访问' : 'Access Denied'}</div>;
      case 'postDetail':
        return selectedPostId ? (
          <PostDetail key={`detail-${lang}-${selectedPostId}`} postId={selectedPostId} token={token} currentUserId={user?.id} onBack={() => handleNavigate('home')} />
        ) : (
          <div className="card">{lang === 'zh' ? '帖子未找到' : 'Post not found'}</div>
        );
      case 'profile':
        return (
          <div className="card">
            <h2 className="section-title">Profile</h2>
            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1rem' }}>
              <div style={{
                width: '80px',
                height: '80px',
                borderRadius: '50%',
                border: '2px solid #333',
                background: '#e0e0e0',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '2rem'
              }}>👤</div>
              <div>
                <h3 style={{ margin: 0 }}>Chen</h3>
                <p style={{ margin: '4px 0', color: '#666', fontFamily: 'monospace' }}>@chen_student</p>
                <span className="pill">Chinese Student</span>
              </div>
            </div>
            <p style={{ fontFamily: 'Georgia, serif' }}>Freshman at CMU. Looking for a quiet condo near Nimman. Love spicy food! 🌶️</p>
            <div style={{ display: 'flex', gap: '2rem', padding: '1rem 0', borderTop: '2px dashed #333', borderBottom: '2px dashed #333', margin: '1rem 0' }}>
              <div style={{ textAlign: 'center' }}><strong style={{ fontSize: '1.25rem' }}>12</strong><br /><small>Posts</small></div>
              <div style={{ textAlign: 'center' }}><strong style={{ fontSize: '1.25rem' }}>24</strong><br /><small>Following</small></div>
              <div style={{ textAlign: 'center' }}><strong style={{ fontSize: '1.25rem' }}>8</strong><br /><small>Followers</small></div>
            </div>
            {/* 审核结果区域：展示被拒绝的帖子及理由 */}
            <div style={{ marginTop: '1rem' }}>
              <h3 className="section-title" style={{ fontSize: '1rem' }}>
                {lang === 'zh' ? '被拒绝的帖子与审核理由' : 'Rejected Posts & Review Notes'}
              </h3>
              {(!rejectedPosts || rejectedPosts.length === 0) ? (
                <p style={{ color: '#666', fontSize: '0.85rem' }}>
                  {lang === 'zh' ? '目前没有被拒绝的帖子。' : 'You have no rejected posts at the moment.'}
                </p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {rejectedPosts.map((p) => (
                    <div key={p.id} className="card" style={{ border: '1px solid #fecaca', background: '#fef2f2' }}>
                      <div style={{ fontWeight: 'bold', marginBottom: '0.25rem' }}>{p.title || '(no title)'}</div>
                      <div style={{ fontSize: '0.8rem', color: '#991b1b', marginBottom: '0.25rem' }}>
                        {lang === 'zh' ? '状态：已拒绝' : 'Status: REJECTED'}
                      </div>
                      {p.reviewNote && (
                        <div style={{ fontSize: '0.8rem', color: '#7f1d1d' }}>
                          {lang === 'zh' ? '审核理由：' : 'Review note: '}{p.reviewNote}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        );
      default:
        return <PostList />;
    }
  };

  return (
    <div className="browser-window">
      {/* App Container */}
      <div className="app-container">
        {!isLoggedIn ? (
          <LoginPage onLogin={handleLogin} />
        ) : (
          <>
            <Sidebar 
              currentPage={currentPage} 
              onNavigate={handleNavigate}
              isAdmin={isAdmin}
              user={user}
            />
            <main className="main-content">
              {renderMainContent()}
            </main>
          </>
        )}
      </div>
    </div>
  );
};

export default App;
