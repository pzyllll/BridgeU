import { useEffect, useState } from 'react';
import { fetchPostDetail, addComment, toggleLike, toggleFollow } from '../api';
import { getCurrentLanguage, t } from '../i18n';

// Filter out browser extension errors
if (typeof window !== 'undefined') {
  const originalError = console.error;
  console.error = (...args) => {
    const errorMsg = args.join(' ');
    // Filter out browser extension errors
    if (errorMsg.includes('content-all.js') || 
        errorMsg.includes('chrome-extension') ||
        errorMsg.includes('Could not establish connection') ||
        errorMsg.includes('Receiving end does not exist')) {
      return; // Ignore these errors
    }
    originalError.apply(console, args);
  };
}

const PostDetail = ({ postId, token, currentUserId, onBack }) => {
  const [postDetail, setPostDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [commentContent, setCommentContent] = useState('');
  const [submittingComment, setSubmittingComment] = useState(false);
  const [lang, setLang] = useState(getCurrentLanguage());

  useEffect(() => {
    const savedLang = localStorage.getItem('userLanguage');
    const currentLang = getCurrentLanguage();
    const langToSet = (savedLang === 'zh' || savedLang === 'en') ? savedLang : (currentLang || 'en');
    setLang(langToSet);
  }, []);

  // Listen for language changes
  useEffect(() => {
    const handleLanguageChange = (e) => {
      if (e && e.detail && e.detail.lang) {
        console.log('PostDetail: Language changed to:', e.detail.lang);
        setLang(e.detail.lang);
      }
    };
    
    if (typeof window !== 'undefined') {
      window.addEventListener('languageChanged', handleLanguageChange);
      return () => {
        window.removeEventListener('languageChanged', handleLanguageChange);
      };
    }
  }, []);

  useEffect(() => {
    if (postId) {
      loadPostDetail();
    }
  }, [postId, lang]);

  const loadPostDetail = async () => {
    if (!postId) return;
    
    setLoading(true);
    setError(null);
    try {
      const savedLang = localStorage.getItem('userLanguage');
      const currentLang = getCurrentLanguage();
      const langToUse = lang || savedLang || currentLang || 'en';
      
      console.log('🔍 PostDetail: Loading post detail', { 
        postId, 
        langToUse,
        lang,
        savedLang,
        currentLang,
        'localStorage.userLanguage': localStorage.getItem('userLanguage')
      });
      const data = await fetchPostDetail(postId, langToUse);
      console.log('📦 PostDetail: Received data:', {
        hasPost: !!data.post,
        postTitle: data.post?.title,
        postTitleLength: data.post?.title?.length || 0,
        postBody: data.post?.body ? `${data.post.body.substring(0, 100)}...` : 'EMPTY',
        postBodyLength: data.post?.body?.length || 0,
        hasContentZh: !!data.post?.contentZh,
        hasContentEn: !!data.post?.contentEn,
        'requestedLang': langToUse,
        fullData: data
      });
      setPostDetail(data);
    } catch (err) {
      console.error('Failed to load post detail:', err);
      setError('Failed to load post');
    } finally {
      setLoading(false);
    }
  };

  const handleAddComment = async () => {
    if (!commentContent.trim() || !token) {
      return;
    }

    setSubmittingComment(true);
    try {
      const savedLang = localStorage.getItem('userLanguage');
      const currentLang = getCurrentLanguage();
      const langToUse = lang || savedLang || currentLang || 'en';
      
      await addComment(postId, commentContent, langToUse, token);
      setCommentContent('');
      // Reload post detail to get updated comments
      await loadPostDetail();
    } catch (err) {
      console.error('Failed to add comment:', err);
      alert(t('postDetail.commentFailed'));
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleToggleLike = async () => {
    if (!token) {
      alert(t('postDetail.loginRequired'));
      return;
    }

    try {
      const result = await toggleLike(postId, token);
      // Update local state
      setPostDetail(prev => ({
        ...prev,
        isLiked: result.liked,
        likeCount: result.likeCount
      }));
    } catch (err) {
      console.error('Failed to toggle like:', err);
    }
  };

  const handleToggleFollow = async () => {
    if (!token) {
      alert(t('postDetail.loginRequired'));
      return;
    }

    if (!postDetail || !postDetail.authorId) {
      return;
    }

    try {
      const result = await toggleFollow(postDetail.authorId, token);
      // Update local state
      setPostDetail(prev => ({
        ...prev,
        isFollowing: result.following
      }));
    } catch (err) {
      console.error('Failed to toggle follow:', err);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return t('postDetail.justNow');
    if (diffMins < 60) return `${diffMins}${t('postDetail.minutesAgo')}`;
    if (diffHours < 24) return `${diffHours}${t('postDetail.hoursAgo')}`;
    if (diffDays < 7) return `${diffDays}${t('postDetail.daysAgo')}`;
    return date.toLocaleDateString();
  };

  if (loading) {
    return (
      <section className="card">
        <p>{t('postDetail.loading')}</p>
      </section>
    );
  }

  if (error || !postDetail) {
    return (
      <section className="card">
        <p style={{ color: '#dc2626' }}>{error || t('postDetail.notFound')}</p>
        {onBack && (
          <button className="btn" onClick={onBack}>
            {t('postDetail.back')}
          </button>
        )}
      </section>
    );
  }

  const post = postDetail.post;

  return (
    <section className="card">
      {onBack && (
        <div style={{ marginBottom: '1rem' }}>
          <button className="btn" onClick={onBack} style={{ marginBottom: '1rem' }}>
            ← {t('postDetail.back')}
          </button>
        </div>
      )}

      {/* Post Header */}
      <div className="post-header" style={{ marginBottom: '1rem', paddingBottom: '1rem', borderBottom: '1px solid #e5e7eb' }}>
        <div className="post-author" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div className="post-avatar"></div>
          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <div className="post-author-name">{postDetail.authorDisplayName || postDetail.authorName || t('postList.anonymous')}</div>
              <span className="post-author-badge">{t('postList.student')}</span>
            </div>
            <div style={{ fontSize: '0.875rem', color: '#666', marginTop: '0.25rem' }}>
              @{postDetail.authorName}
            </div>
          </div>
          {currentUserId && postDetail.authorId && currentUserId !== postDetail.authorId && (
            <button
              className={`btn ${postDetail.isFollowing ? '' : 'btn-primary'}`}
              onClick={handleToggleFollow}
              style={{ padding: '0.5rem 1rem', fontSize: '0.875rem' }}
            >
              {postDetail.isFollowing ? t('postDetail.following') : t('postDetail.follow')}
            </button>
          )}
        </div>
      </div>

      {/* Post Content */}
      <h1 className="post-title" style={{ 
        fontSize: '1.5rem', 
        marginBottom: '1.5rem',
        fontWeight: 'bold',
        lineHeight: '1.4',
        wordBreak: 'break-word'
      }}>
        {post.title || t('postDetail.noTitle')}
      </h1>
      {post.imageUrl && (
        <div style={{ marginBottom: '1.5rem' }}>
          <img
            src={post.imageUrl}
            alt={post.title || 'post image'}
            style={{ width: '100%', maxHeight: '360px', objectFit: 'cover', borderRadius: '12px', border: '1px solid #e5e7eb' }}
          />
        </div>
      )}
      <div className="post-body" style={{ 
        marginBottom: '0',
        paddingBottom: '1rem',
        lineHeight: '1.8',
        wordBreak: 'break-word',
        overflow: 'visible',
        fontSize: '0.9375rem',
        color: '#374151',
        position: 'relative',
        zIndex: 0,
        display: 'block',
        width: '100%',
        clear: 'both'
      }}>
        {(() => {
          console.log('🔍 PostDetail render - post.body:', {
            exists: !!post.body,
            length: post.body?.length || 0,
            preview: post.body ? post.body.substring(0, 200) : 'EMPTY',
            trimmed: post.body?.trim() || ''
          });
          
          if (post.body && post.body.trim()) {
            // 简单的 Markdown 格式化函数
            const formatContent = (text) => {
              if (!text) return '';
              
              // 分割成段落
              const paragraphs = text.split(/\n\s*\n/).filter(p => p.trim());
              
              return paragraphs.map((para, idx) => {
                let formatted = para.trim();
                
                // 处理分隔线
                if (formatted.match(/^---+$/)) {
                  return <hr key={idx} style={{ 
                    border: 'none', 
                    borderTop: '2px dashed #d1d5db', 
                    margin: '1.5rem 0',
                    background: 'none'
                  }} />;
                }
                
                // 处理标题行（**Title** 格式）
                const titleMatch = formatted.match(/^(\*\*)(.+?)(\*\*)$/);
                if (titleMatch) {
                  return (
                    <h3 key={idx} style={{
                      fontSize: '1rem',
                      fontWeight: 'bold',
                      marginTop: idx > 0 ? '1.5rem' : '0',
                      marginBottom: '0.75rem',
                      color: '#111827'
                    }}>
                      {titleMatch[2]}
                    </h3>
                  );
                }
                
                // 处理行内粗体 **text**
                formatted = formatted.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
                
                // 处理链接 🔗 **Read Original**: URL 或 🔗 **Source**: text
                const linkMatch = formatted.match(/(🔗)\s*\*\*(.+?)\*\*:\s*(.+)/);
                if (linkMatch) {
                  const linkText = linkMatch[2];
                  const linkUrl = linkMatch[3].trim();
                  const isUrl = linkUrl.match(/^https?:\/\//);
                  
                  return (
                    <div key={idx} style={{
                      marginTop: idx > 0 ? '1.5rem' : '0',
                      marginBottom: '2rem',
                      padding: '0.75rem 1rem',
                      background: '#f9fafb',
                      borderRadius: '6px',
                      borderLeft: '3px solid #3b82f6',
                      position: 'relative',
                      zIndex: 1,
                      display: 'block',
                      width: '100%',
                      boxSizing: 'border-box',
                      clear: 'both'
                    }}>
                      <div style={{ 
                        fontWeight: 'bold', 
                        marginBottom: '0.5rem',
                        fontSize: '0.875rem',
                        color: '#374151',
                        display: 'block',
                        width: '100%'
                      }}>
                        🔗 {linkText}:
                      </div>
                      {isUrl ? (
                        <a 
                          href={linkUrl} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          style={{
                            color: '#2563eb',
                            textDecoration: 'underline',
                            wordBreak: 'break-all',
                            fontSize: '0.875rem',
                            display: 'inline-block',
                            width: '100%',
                            maxWidth: '100%',
                            overflowWrap: 'break-word'
                          }}
                        >
                          {linkUrl}
                        </a>
                      ) : (
                        <span style={{ 
                          fontSize: '0.875rem', 
                          color: '#6b7280',
                          display: 'block',
                          wordBreak: 'break-word'
                        }}>
                          {linkUrl}
                        </span>
                      )}
                    </div>
                  );
                }
                
                // 普通段落
                return (
                  <p 
                    key={idx} 
                    style={{
                      marginTop: idx > 0 ? '1rem' : '0',
                      marginBottom: '0.75rem',
                      lineHeight: '1.8',
                      position: 'relative',
                      zIndex: 0,
                      display: 'block',
                      width: '100%',
                      clear: 'both'
                    }}
                    dangerouslySetInnerHTML={{ __html: formatted }}
                  />
                );
              });
            };
            
            return formatContent(post.body);
          } else {
            console.warn('⚠️ PostDetail: post.body is empty or null', {
              postId: post.id,
              title: post.title,
              body: post.body,
              contentZh: post.contentZh,
              contentEn: post.contentEn
            });
            return (
              <div style={{ 
                color: '#666', 
                fontStyle: 'italic', 
                padding: '1.5rem', 
                background: '#f9fafb', 
                borderRadius: '8px',
                textAlign: 'center',
                border: '2px dashed #d1d5db'
              }}>
                {t('postDetail.noContent')}
              </div>
            );
          }
        })()}
      </div>

      {/* Post Actions */}
      <div style={{ 
        display: 'flex', 
        gap: '1rem', 
        padding: '1rem 0', 
        borderTop: '2px dashed #e5e7eb', 
        borderBottom: '2px dashed #e5e7eb', 
        marginTop: '2rem',
        marginBottom: '1.5rem',
        flexWrap: 'wrap',
        alignItems: 'center',
        position: 'relative',
        zIndex: 2,
        clear: 'both',
        width: '100%',
        boxSizing: 'border-box'
      }}>
        <button
          className="btn"
          onClick={handleToggleLike}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            padding: '0.5rem 1rem',
            background: postDetail.isLiked ? '#fee2e2' : 'transparent',
            color: postDetail.isLiked ? '#dc2626' : 'inherit'
          }}
        >
          <span>{postDetail.isLiked ? '❤️' : '🤍'}</span>
          <span>{postDetail.likeCount || 0}</span>
        </button>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.5rem 1rem' }}>
          <span>💬</span>
          <span>{postDetail.commentCount || 0}</span>
        </div>
        <div style={{ 
          display: 'flex', 
          alignItems: 'center', 
          gap: '0.5rem', 
          padding: '0.5rem 1rem', 
          color: '#666', 
          fontSize: '0.875rem',
          marginLeft: 'auto'
        }}>
          <span>{formatDate(post.createdAt)}</span>
        </div>
      </div>

      {/* Comment Form */}
      {token && (
        <div style={{ 
          marginTop: '1.5rem',
          marginBottom: '2rem',
          padding: '1.5rem',
          background: '#f9fafb',
          borderRadius: '8px',
          border: '2px solid #e5e7eb',
          position: 'relative',
          zIndex: 2,
          clear: 'both'
        }}>
          <h3 style={{ 
            marginBottom: '1rem',
            fontSize: '1rem',
            fontWeight: 'bold'
          }}>
            {t('postDetail.addComment')}
          </h3>
          <textarea
            className="input"
            value={commentContent}
            onChange={(e) => setCommentContent(e.target.value)}
            placeholder={t('postDetail.commentPlaceholder')}
            rows={4}
            style={{ 
              width: '100%', 
              marginBottom: '0.75rem',
              minHeight: '100px',
              resize: 'vertical'
            }}
          />
          <button
            className="btn btn-primary"
            onClick={handleAddComment}
            disabled={submittingComment || !commentContent.trim()}
            style={{ minWidth: '100px' }}
          >
            {submittingComment ? t('postDetail.submitting') : t('postDetail.submitComment')}
          </button>
        </div>
      )}

      {/* Comments List */}
      <div>
        <h3 style={{ 
          marginBottom: '1rem',
          fontSize: '1.125rem',
          fontWeight: 'bold',
          paddingBottom: '0.5rem',
          borderBottom: '2px solid #e5e7eb'
        }}>
          {t('postDetail.comments')} ({postDetail.comments?.length || 0})
        </h3>
        {postDetail.comments && postDetail.comments.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {postDetail.comments.map((comment) => (
              <div key={comment.id} style={{ 
                padding: '1rem', 
                background: '#fff', 
                borderRadius: '8px',
                border: '2px solid #e5e7eb',
                transition: 'all 0.2s'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                  <div className="post-avatar" style={{ width: '32px', height: '32px' }}></div>
                  <div>
                    <div className="post-author-name" style={{ fontSize: '0.875rem' }}>
                      {comment.authorDisplayName || comment.authorName || t('postList.anonymous')}
                    </div>
                    <div style={{ fontSize: '0.75rem', color: '#666' }}>
                      {formatDate(comment.createdAt)}
                    </div>
                  </div>
                </div>
                <div style={{ marginLeft: '2.5rem', lineHeight: '1.6' }}>
                  {comment.content}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: '#666', fontStyle: 'italic' }}>{t('postDetail.noComments')}</p>
        )}
      </div>
    </section>
  );
};

export default PostDetail;

