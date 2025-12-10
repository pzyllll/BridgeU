import { useState } from 'react';
import LoginPage from './components/LoginPage';
import Sidebar from './components/Sidebar';
import PostList from './components/PostList';
import PostDetail from './components/PostDetail';
import SearchPanel from './components/SearchPanel';
import DailyBriefing from './components/DailyBriefing';
import NlpAssistant from './components/NlpAssistant';
import NewPostForm from './components/NewPostForm';
import AdminPanel from './components/AdminPanel';
import { setLanguagePreference } from './utils/language';
import { setLanguage } from './i18n';

const App = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentPage, setCurrentPage] = useState('home');
  const [selectedPostId, setSelectedPostId] = useState(null);
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);

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
      // 清除 localStorage 中的 token
      localStorage.removeItem('token');
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

  const renderMainContent = () => {
    switch (currentPage) {
      case 'home':
        return (
          <>
            <DailyBriefing />
            <div className="section-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>Community Feed</span>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <span className="pill active">All</span>
                <span className="pill">🏠 #Rent</span>
                <span className="pill">📚 #Learning</span>
                <span className="pill">🛒 #Market</span>
              </div>
            </div>
            <PostList onPostClick={(postId) => handleNavigate('postDetail', postId)} />
          </>
        );
      case 'post':
        return <NewPostForm currentUserId={user?.id} />;
      case 'search':
        return <SearchPanel />;
      case 'assistant':
        return <NlpAssistant />;
      case 'admin':
        return isAdmin ? <AdminPanel token={token} /> : <div className="card">Access Denied</div>;
      case 'postDetail':
        return selectedPostId ? (
          <PostDetail postId={selectedPostId} token={token} currentUserId={user?.id} onBack={() => handleNavigate('home')} />
        ) : (
          <div className="card">Post not found</div>
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
          </div>
        );
      default:
        return <PostList />;
    }
  };

  return (
    <div className="browser-window">
      {/* Browser Header */}
      <div className="browser-header">
        <div className="traffic-light"></div>
        <div className="traffic-light"></div>
        <div className="traffic-light"></div>
        <div className="url-bar">https://www.bridgeu.com/community</div>
      </div>

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
