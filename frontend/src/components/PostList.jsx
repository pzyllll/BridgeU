import { useEffect, useState } from 'react';
import { fetchPosts } from '../api';
import { getLanguagePreference } from '../utils/language';
import { t, getCurrentLanguage, setLanguage } from '../i18n';

const PostList = ({ onPostClick }) => {
  const [posts, setPosts] = useState([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [lang, setLang] = useState(getCurrentLanguage());

  const loadPosts = async () => {
    setLoading(true);
    try {
      // Get current language from multiple sources to ensure accuracy
      const savedLang = localStorage.getItem('userLanguage');
      const currentLang = getCurrentLanguage();
      const langToUse = lang || savedLang || currentLang || 'en';
      
      console.log('PostList: Loading posts');
      console.log('  - lang state:', lang);
      console.log('  - localStorage:', savedLang);
      console.log('  - currentLang from i18n:', currentLang);
      console.log('  - final langToUse:', langToUse);
      
      // Explicitly pass lang parameter
      const result = await fetchPosts({ q: query || undefined, lang: langToUse });
      console.log('PostList: Posts loaded:', result?.length || 0, 'posts');
      
      // Debug: Check if posts have Chinese content
      if (langToUse === 'zh' && result && result.length > 0) {
        const firstPost = result[0];
        console.log('=== First Post Debug (Chinese) ===');
        console.log('Title:', firstPost.title);
        console.log('Body (first 100 chars):', firstPost.body?.substring(0, 100));
        console.log('Has contentZh:', !!firstPost.contentZh);
        console.log('Has contentEn:', !!firstPost.contentEn);
        console.log('Original language:', firstPost.originalLanguage);
      }
      
      setPosts(result || []);
    } catch (error) {
      console.error('Failed to load posts:', error);
      setPosts([]);
    } finally {
      setLoading(false);
    }
  };

  // Initialize language and load posts
  useEffect(() => {
    // Force re-initialize language from localStorage
    const savedLang = localStorage.getItem('userLanguage');
    console.log('PostList: Initial load, localStorage userLanguage:', savedLang);
    
    // Get current language from i18n (which reads from localStorage)
    const currentLang = getCurrentLanguage();
    console.log('PostList: Initial load, currentLang from i18n:', currentLang);
    
    // Use savedLang if available, otherwise use currentLang
    const langToSet = (savedLang === 'zh' || savedLang === 'en') ? savedLang : (currentLang || 'en');
    console.log('PostList: Setting lang state to:', langToSet);
    setLang(langToSet);
    
    // Also update i18n if needed
    if (savedLang && savedLang !== currentLang && (savedLang === 'zh' || savedLang === 'en')) {
      setLanguage(savedLang);
    }
  }, []);

  // Listen for language changes and reload posts
  useEffect(() => {
    const handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        const newLang = e.detail.lang;
        setLang(newLang);
        // Reload posts with new language
        setTimeout(() => {
          loadPosts();
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
  }, []);

  // Reload posts when language changes
  useEffect(() => {
    if (lang) {
      console.log('PostList: Language changed to:', lang, 'reloading posts...');
      loadPosts();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lang]);

  // Parse tags (could be array, JSON string, or comma-separated string)
  const parseTags = (tags) => {
    if (!tags) return [];
    if (Array.isArray(tags)) return tags;
    if (typeof tags === 'string') {
      // Try JSON parsing
      if (tags.startsWith('[')) {
        try {
          return JSON.parse(tags);
        } catch (e) {
          // Parsing failed, treat as comma-separated
        }
      }
      // Comma-separated string
      return tags.split(',').map(t => t.trim());
    }
    return [];
  };

  const getTagEmoji = (tags) => {
    const tagList = parseTags(tags);
    if (tagList.some(t => ['rent', 'rental', 'bangkok'].includes(t.toLowerCase()))) return '🏠';
    if (tagList.some(t => ['learning', 'study', 'course', 'class'].includes(t.toLowerCase()))) return '📚';
    if (tagList.some(t => ['market', 'secondhand', 'trading'].includes(t.toLowerCase()))) return '🛒';
    if (tagList.some(t => ['visa', 'studyabroad'].includes(t.toLowerCase()))) return '🛂';
    if (tagList.some(t => ['food', 'life', 'lifestyle'].includes(t.toLowerCase()))) return '🍜';
    return '📝';
  };

  return (
    <section style={{ marginBottom: '1.5rem' }}>
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <input
          className="input"
          style={{ flex: 1 }}
          placeholder={t('postList.searchPlaceholder')}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && loadPosts()}
        />
        <button className="btn btn-primary" onClick={loadPosts}>
          {t('postList.search')}
        </button>
      </div>

      {loading ? (
        <p>{t('postList.loading')}</p>
      ) : (
        <div className="grid grid-2">
          {posts.map((post) => (
            <article 
              key={post.id} 
              className="post-card"
              style={{ cursor: 'pointer' }}
              onClick={() => onPostClick && onPostClick(post.id)}
            >
              <div className="post-header">
                <div className="post-author">
                  <div className="post-avatar"></div>
                  <div>
                    <div className="post-author-name">{post.authorName || t('postList.anonymous')}</div>
                    <span className="post-author-badge">{t('postList.student')}</span>
                  </div>
                </div>
                <span className="post-tag">
                  {getTagEmoji(post.tags)} #{parseTags(post.tags)[0] || 'Post'}
                </span>
              </div>
              <h3 className="post-title">{post.title}</h3>
              {post.score != null && (
                <p style={{ color: '#7c3aed', fontSize: '0.75rem', marginBottom: '4px' }}>
                  {t('postList.semanticScore')}: {post.score.toFixed(2)}
                </p>
              )}
              <p className="post-body">{post.body}</p>
              <div className="post-footer">
                <span>2h ago</span>
                <div className="post-stats">
                  <span>❤️ 24</span>
                  <span>💬 5</span>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};

export default PostList;

