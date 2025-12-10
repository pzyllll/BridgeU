import { useState } from 'react';
import { searchAll } from '../api';

const SearchPanel = () => {
  const [query, setQuery] = useState('');
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const data = await searchAll({ q: query });
      setResult(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '800px', margin: '0 auto', boxShadow: '8px 8px 0px rgba(0,0,0,1)' }}>
      <h2 className="section-title">    </h2>
      <div className="flex" style={{ marginBottom: '1rem' }}>
        <input
          className="input"
          style={{ flex: 1 }}
          placeholder="输入关键词，如 烹饪、住宿、课程"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
        />
        <button className="btn btn-primary" onClick={handleSearch}>
          搜索
        </button>
      </div>
      {loading && <p>搜索中...</p>}
      {result && (
        <div>
          <h4 style={{ borderBottom: '2px solid #333', paddingBottom: '0.5rem' }}>📝 帖子匹配</h4>
          {result.posts && result.posts.length === 0 && <p style={{ color: '#666' }}>无匹配</p>}
          {result.posts && result.posts.map((post) => (
            <div key={post.id} className="card" style={{ marginBottom: '0.5rem' }}>
              <strong>{post.title}</strong>
              <p style={{ color: '#666', fontSize: '0.875rem', fontFamily: 'Georgia, serif', margin: '0.5rem 0' }}>
                {post.body}
              </p>
              <small style={{ color: '#7c3aed' }}>得分：{post.score.toFixed(2)}</small>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SearchPanel;
