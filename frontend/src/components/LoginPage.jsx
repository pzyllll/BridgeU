import { useState, useEffect } from 'react';
import { t, setLanguage, getCurrentLanguage, initLanguage } from '../i18n';

// 与全局 axios 配置保持一致：优先使用环境变量，未设置则使用相对路径走 Vite 代理
const API_BASE = import.meta.env.VITE_API_BASE ? `${import.meta.env.VITE_API_BASE}/api` : '/api';

const LoginPage = ({ onLogin }) => {
  const [activeTab, setActiveTab] = useState('student');
  const [isRegister, setIsRegister] = useState(false);
  const [lang, setLangState] = useState(getCurrentLanguage());
  const [form, setForm] = useState({ username: '', email: '', password: '', businessName: '', contact: '', preferredLanguage: 'en' });
  const [merchantForm, setMerchantForm] = useState({ username: '', email: '', phone: '', password: '', businessName: '', idNumber: '', preferredLanguage: 'zh' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Initialize language and listen for changes
  useEffect(() => {
    initLanguage();
    setLangState(getCurrentLanguage());
    
    const handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        setLangState(e.detail.lang);
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

  const handleLanguageSwitch = (newLang) => {
    setLanguage(newLang);
    setLangState(newLang);
  };

  // 登录处理
  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: form.username,
          password: form.password
        })
      });
      
      const data = await res.json();
      
      if (res.ok) {
        console.log('LoginPage: Login successful, user data:', data.user);
        
        // 从用户信息中读取语言偏好并立即设置
        if (data.user) {
          const userLang = data.user.preferredLanguage;
          console.log('LoginPage: User preferredLanguage from server:', userLang);
          
          if (userLang && (userLang === 'zh' || userLang === 'en')) {
            console.log('LoginPage: Setting language to:', userLang);
            setLanguage(userLang);
            console.log('LoginPage: Language set, verifying...');
            console.log('  - getCurrentLanguage():', getCurrentLanguage());
            console.log('  - localStorage.getItem("userLanguage"):', localStorage.getItem('userLanguage'));
          } else {
            console.warn('LoginPage: Invalid or missing preferredLanguage:', userLang, 'using default: en');
            // 如果没有 preferredLanguage，使用默认值
            setLanguage('en');
          }
        } else {
          console.warn('LoginPage: No user data in response');
        }
        onLogin(data.user, data.token);
      } else {
        setError(data.error || 'Login failed');
      }
    } catch (err) {
      setError('Network error, please ensure backend service is running');
    }
    setLoading(false);
  };

  // 商家注册
  const handleMerchantRegister = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const payload = { ...merchantForm, preferredLanguage: merchantForm.preferredLanguage || lang };
      const res = await fetch(`${API_BASE}/auth/register/merchant`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json();

      if (res.ok) {
        if (data.user && data.user.preferredLanguage) {
          const userLang = data.user.preferredLanguage;
          if (userLang === 'zh' || userLang === 'en') {
            setLanguage(userLang);
          }
        }
        onLogin(data.user, data.token);
      } else {
        setError(data.error || 'Registration failed');
      }
    } catch (err) {
      setError('Network error, please ensure backend service is running');
    }
    setLoading(false);
  };

  // 注册处理
  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: form.username,
          email: form.email,
          password: form.password,
          displayName: form.username,
          preferredLanguage: form.preferredLanguage || lang // 保存用户选择的语言偏好
        })
      });
      
      const data = await res.json();
      
      if (res.ok) {
        // 从用户信息中读取语言偏好并立即设置
        if (data.user && data.user.preferredLanguage) {
          const userLang = data.user.preferredLanguage;
          if (userLang === 'zh' || userLang === 'en') {
            setLanguage(userLang);
          }
        }
        onLogin(data.user, data.token);
      } else {
        setError(data.error || 'Registration failed');
      }
    } catch (err) {
      setError('Network error, please ensure backend service is running');
    }
    setLoading(false);
  };

  return (
    <div className="login-container">
      {/* Language Switcher */}
      <div className="lang-switcher">
        <button
          className={`lang-btn ${lang === 'en' ? 'active' : ''}`}
          onClick={() => handleLanguageSwitch('en')}
        >
          EN
        </button>
        <button
          className={`lang-btn ${lang === 'zh' ? 'active' : ''}`}
          onClick={() => handleLanguageSwitch('zh')}
        >
          中文
        </button>
      </div>

      <div className="login-box">
        {/* Header */}
        <div className="login-header">
          <div className="logo">🌐</div>
          <h1>BridgeU</h1>
          <p>{t('login.subtitle')}</p>
        </div>

        {/* Tabs */}
        <div className="login-tabs">
          <button
            className={`login-tab ${activeTab === 'student' ? 'active' : ''}`}
            onClick={() => setActiveTab('student')}
          >
            {t('login.student')}
          </button>
          <button
            className={`login-tab ${activeTab === 'merchant' ? 'active' : ''}`}
            onClick={() => setActiveTab('merchant')}
          >
            {t('login.merchant')}
          </button>
        </div>

        {/* Student Form */}
        {activeTab === 'student' && (
          <form className="login-form" onSubmit={isRegister ? handleRegister : handleLogin}>
            {error && (
              <div style={{ padding: '0.5rem', background: '#f8d7da', border: '2px solid #dc3545', marginBottom: '1rem', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                {error}
              </div>
            )}
            <div className="form-group">
              <label>{t('login.username')}</label>
              <input
                type="text"
                className="input"
                placeholder="admin / lihua"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                required
              />
            </div>
            {isRegister && (
              <>
                <div className="form-group">
                  <label>{t('login.email')}</label>
                  <input
                    type="email"
                    className="input"
                    placeholder="your@email.com"
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>{t('login.preferredLanguage') || 'Preferred Language'}</label>
                  <select
                    className="input"
                    value={form.preferredLanguage}
                    onChange={(e) => setForm({ ...form, preferredLanguage: e.target.value })}
                    required
                  >
                    <option value="en">English</option>
                    <option value="zh">中文</option>
                  </select>
                </div>
              </>
            )}
            <div className="form-group">
              <label>{t('login.password')}</label>
              <input
                type="password"
                className="input"
                placeholder="******"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
              />
            </div>
            <button 
              type="submit" 
              className="btn btn-primary" 
              style={{ width: '100%', marginTop: '1rem' }}
              disabled={loading}
            >
              {loading ? t('login.pleaseWait') : (isRegister ? t('login.register') : t('login.login'))}
            </button>
            <div className="link" onClick={() => setIsRegister(!isRegister)} style={{ cursor: 'pointer' }}>
              {isRegister ? t('login.hasAccount') : t('login.create')}
            </div>
            <div style={{ marginTop: '1rem', padding: '0.5rem', background: '#e9ecef', fontFamily: 'monospace', fontSize: '0.7rem', textAlign: 'center' }}>
              <div>{t('login.testAdmin')}</div>
              <div>{t('login.testUser')}</div>
            </div>
          </form>
        )}

        {/* Merchant Form */}
        {activeTab === 'merchant' && (
          <form className="login-form" onSubmit={handleMerchantRegister}>
            {error && (
              <div style={{ padding: '0.5rem', background: '#f8d7da', border: '2px solid #dc3545', marginBottom: '1rem', fontFamily: 'monospace', fontSize: '0.8rem' }}>
                {error}
              </div>
            )}
            <div className="form-group">
              <label>用户名</label>
              <input
                type="text"
                className="input"
                placeholder="merchant_user"
                value={merchantForm.username}
                onChange={(e) => setMerchantForm({ ...merchantForm, username: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>邮箱</label>
              <input
                type="email"
                className="input"
                placeholder="merchant@example.com"
                value={merchantForm.email}
                onChange={(e) => setMerchantForm({ ...merchantForm, email: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>电话</label>
              <input
                type="text"
                className="input"
                placeholder="+66 8x xxxx xxx"
                value={merchantForm.phone}
                onChange={(e) => setMerchantForm({ ...merchantForm, phone: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>身份证号</label>
              <input
                type="text"
                className="input"
                placeholder="例如：1234567890123"
                value={merchantForm.idNumber}
                onChange={(e) => setMerchantForm({ ...merchantForm, idNumber: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>{t('login.password')}</label>
              <input
                type="password"
                className="input"
                placeholder="******"
                value={merchantForm.password}
                onChange={(e) => setMerchantForm({ ...merchantForm, password: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>{t('login.businessName')}</label>
              <input
                type="text"
                className="input"
                placeholder="e.g., Somchai Property"
                value={merchantForm.businessName}
                onChange={(e) => setMerchantForm({ ...merchantForm, businessName: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>{t('login.preferredLanguage') || 'Preferred Language'}</label>
              <select
                className="input"
                value={merchantForm.preferredLanguage}
                onChange={(e) => setMerchantForm({ ...merchantForm, preferredLanguage: e.target.value })}
                required
              >
                <option value="zh">中文</option>
                <option value="en">English</option>
              </select>
            </div>
            <button
              type="submit"
              className="btn"
              style={{ width: '100%', marginTop: '0.5rem', background: '#2563eb', color: '#fff', border: '2px solid #1d4ed8' }}
              disabled={loading}
            >
              {loading ? t('login.pleaseWait') : t('login.registerMerchant')}
            </button>
            <div className="link" onClick={() => setActiveTab('student')}>
              {t('login.back')}
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default LoginPage;
