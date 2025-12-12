import { useState } from 'react';
import { createPost, uploadPostImage } from '../api';
import { t, getCurrentLanguage } from '../i18n';

const defaultForm = {
  communityId: '',
  title: '',
  body: '',
  tags: '',
  imageUrl: '',
};

const TAG_OPTIONS = [
  { label: '🏠 #Rent', value: 'rent' },
  { label: '📚 #Learning', value: 'learning' },
  { label: '🛒 #Market', value: 'market' },
  { label: '🛂 #Visa', value: 'visa' },
];

const NewPostForm = ({ currentUserId }) => {
  const [form, setForm] = useState(defaultForm);
  const [notice, setNotice] = useState('');
  const [selectedTag, setSelectedTag] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [lang] = useState(getCurrentLanguage());
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    const storedUser = (() => {
      try {
        return JSON.parse(localStorage.getItem('user') || '{}');
      } catch (e) {
        return {};
      }
    })();
    const authorId = currentUserId || storedUser?.id;
    if (!authorId) {
      setNotice('请先登录，系统将自动使用当前用户ID');
      return;
    }
    if (!form.title.trim()) {
      setNotice('标题不能为空');
      return;
    }
    if (!form.body.trim()) {
      setNotice('内容不能为空');
      return;
    }
    const communityId = form.communityId?.trim() || 'default';
    setIsSubmitting(true);
    setNotice('');
    try {
      const token = localStorage.getItem('token');
      let imageUrl = form.imageUrl;

      // 上传图片（如果有选择）
      if (imageFile) {
        const uploadResp = await uploadPostImage(imageFile, token);
        imageUrl = uploadResp.url;
      }

      await createPost({
        communityId,
        authorId,
        title: form.title,
        body: form.body,
        tags: selectedTag ? [selectedTag] : form.tags
          .split(',')
          .map((tag) => tag.trim())
          .filter(Boolean),
        imageUrl,
      }, token);
      setNotice(t('newPost.success'));
      setForm((prev) => ({ ...defaultForm, communityId: prev.communityId }));
      setSelectedTag('');
      setImageFile(null);
      setImagePreview('');
    } catch (error) {
      console.error(error);
      setNotice(t('newPost.failed'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '600px', margin: '0 auto', boxShadow: '8px 8px 0px rgba(0,0,0,1)' }}>
      <h2 className="section-title">{t('newPost.title')}</h2>
      <div style={{ 
        marginBottom: '1rem', 
        padding: '0.75rem', 
        background: '#e0f2fe', 
        border: '2px solid #0ea5e9',
        borderRadius: '4px',
        fontSize: '0.875rem'
      }}>
        <strong>{t('newPost.autoTranslation')}</strong> {t('newPost.autoTranslationDesc')}
      </div>
      <div style={{
        marginBottom: '1rem',
        padding: '0.65rem',
        background: '#fff7ed',
        border: '1px solid #fed7aa',
        borderRadius: '4px',
        fontSize: '0.8rem',
        color: '#7c2d12'
      }}>
        <strong>内容安全提示：</strong>
        帖子文字和图片会先经过 AI 实时审核，高风险内容会被自动拦截；
        边界内容将进入人工审核队列，管理员会做最终判断。
        <br />
        <span style={{ fontSize: '0.75rem', color: '#92400e' }}>
          Content safety notice: Your text and images will be screened by AI and human moderators. High-risk content is blocked automatically; borderline content will be marked as
          <em> pending review</em> before publication.
        </span>
      </div>
      
      <form onSubmit={handleSubmit}>
        {/* Tag Selection */}
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', fontSize: '10px', fontWeight: 'bold', textTransform: 'uppercase', marginBottom: '8px' }}>
            {t('newPost.selectTag')}
          </label>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {TAG_OPTIONS.map((tag) => (
              <button
                key={tag.value}
                type="button"
                className={`pill ${selectedTag === tag.value ? 'active' : ''}`}
                onClick={() => setSelectedTag(tag.value)}
              >
                {tag.label}
              </button>
            ))}
          </div>
        </div>

        {/* Title */}
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', fontSize: '10px', fontWeight: 'bold', textTransform: 'uppercase', marginBottom: '4px' }}>
            {t('newPost.postTitle')}
          </label>
          <input
            className="input"
            placeholder={t('newPost.postTitlePlaceholder')}
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
          />
        </div>

        {/* Content */}
        <div style={{ marginBottom: '1rem' }}>
          <label style={{ display: 'block', fontSize: '10px', fontWeight: 'bold', textTransform: 'uppercase', marginBottom: '4px' }}>
            {t('newPost.content')}
          </label>
          <textarea
            className="input"
            style={{ minHeight: '120px' }}
            placeholder={t('newPost.contentPlaceholder')}
            value={form.body}
            onChange={(e) => setForm({ ...form, body: e.target.value })}
          />
        </div>

        {/* Image Upload Placeholder */}
        <div
          style={{
            marginBottom: '1rem',
            border: '2px dashed #333',
            background: '#f9f9f9',
            minHeight: '100px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
            padding: '8px'
          }}
          onClick={() => document.getElementById('postImageInput')?.click()}
        >
          <input
            id="postImageInput"
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (!file) return;
              setImageFile(file);
              const reader = new FileReader();
              reader.onload = (ev) => setImagePreview(ev.target?.result || '');
              reader.readAsDataURL(file);
            }}
          />
          <span style={{ fontSize: '24px', marginBottom: '4px' }}>🖼️</span>
          <span style={{ fontSize: '10px', fontWeight: 'bold', textTransform: 'uppercase', color: '#666' }}>
            点击上传图片
          </span>
          {imagePreview && (
            <img
              src={imagePreview}
              alt="preview"
              style={{ maxWidth: '100%', maxHeight: '160px', marginTop: '8px', borderRadius: '6px' }}
            />
          )}
        </div>

        {/* Notice */}
        {notice && (
          <div
            style={{
              marginBottom: '1rem',
              padding: '0.75rem',
              border: '2px solid #333',
              background: notice.includes('✅') ? '#dcfce7' : '#fef2f2',
              fontSize: '0.875rem',
              fontWeight: 'bold',
            }}
          >
            {notice}
          </div>
        )}

        {/* Submit */}
        <button
          className="btn btn-primary"
          type="submit"
          style={{ width: '100%' }}
          disabled={isSubmitting}
        >
          {isSubmitting ? t('newPost.publishing') : t('newPost.publish')}
        </button>
      </form>
    </div>
  );
};

export default NewPostForm;

