import { useState, useEffect } from 'react';

const API_BASE = 'http://localhost:8080/api';

const AdminPanel = ({ token }) => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [stats, setStats] = useState(null);
  const [pendingPosts, setPendingPosts] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // Fetch dashboard data
  const fetchDashboard = async () => {
    try {
      const res = await fetch(`${API_BASE}/admin/dashboard`, { headers });
      if (res.ok) {
        const data = await res.json();
        setStats(data);
      }
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
    }
  };

  // Fetch pending posts
  const fetchPendingPosts = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/admin/posts/pending`, { headers });
      if (res.ok) {
        const data = await res.json();
        setPendingPosts(data.data || []);
      }
    } catch (err) {
      console.error('Failed to fetch pending posts:', err);
    }
    setLoading(false);
  };

  // Fetch user list
  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/admin/users`, { headers });
      if (res.ok) {
        const data = await res.json();
        setUsers(data.data || []);
      }
    } catch (err) {
      console.error('Failed to fetch user list:', err);
    }
    setLoading(false);
  };

  // Approve post
  const approvePost = async (postId) => {
    try {
      const res = await fetch(`${API_BASE}/admin/posts/${postId}/approve`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ note: 'Content approved' })
      });
      if (res.ok) {
        fetchPendingPosts();
        fetchDashboard();
      }
    } catch (err) {
      console.error('Failed to approve post:', err);
    }
  };

  // Reject post
  const rejectPost = async (postId, reason) => {
    try {
      const res = await fetch(`${API_BASE}/admin/posts/${postId}/reject`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ note: reason || 'Content does not meet community guidelines' })
      });
      if (res.ok) {
        fetchPendingPosts();
        fetchDashboard();
      }
    } catch (err) {
      console.error('Failed to reject post:', err);
    }
  };

  // Update user role
  const updateUserRole = async (userId, newRole) => {
    try {
      const res = await fetch(`${API_BASE}/admin/users/${userId}/role`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify({ role: newRole })
      });
      if (res.ok) {
        fetchUsers();
      }
    } catch (err) {
      console.error('Failed to update role:', err);
    }
  };

  // Disable/Enable user
  const toggleUserStatus = async (userId, enabled) => {
    try {
      const res = await fetch(`${API_BASE}/admin/users/${userId}/status`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify({ enabled })
      });
      if (res.ok) {
        fetchUsers();
      }
    } catch (err) {
      console.error('Failed to update user status:', err);
    }
  };


  useEffect(() => {
    fetchDashboard();
  }, []);

  useEffect(() => {
    if (activeTab === 'posts') fetchPendingPosts();
    if (activeTab === 'users') fetchUsers();
  }, [activeTab]);

  // Dashboard panel
  const DashboardView = () => (
    <div>
      <h2 className="section-title">📊 Admin Dashboard</h2>
      {stats ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          <div className="card" style={{ textAlign: 'center' }}>
            <h3 style={{ margin: '0 0 0.5rem 0', fontFamily: 'monospace' }}>👥 User Statistics</h3>
            <div style={{ fontSize: '2rem', fontWeight: 'bold' }}>{stats.users?.total || 0}</div>
            <small style={{ fontFamily: 'monospace' }}>
              Admins: {stats.users?.admins || 0} | Regular Users: {stats.users?.regularUsers || 0}
            </small>
          </div>
          <div className="card" style={{ textAlign: 'center' }}>
            <h3 style={{ margin: '0 0 0.5rem 0', fontFamily: 'monospace' }}>📝 Total Posts</h3>
            <div style={{ fontSize: '2rem', fontWeight: 'bold' }}>{stats.posts?.total || 0}</div>
          </div>
          <div className="card" style={{ textAlign: 'center', background: '#fff3cd' }}>
            <h3 style={{ margin: '0 0 0.5rem 0', fontFamily: 'monospace' }}>⏳ Pending</h3>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#856404' }}>{stats.posts?.pending || 0}</div>
          </div>
          <div className="card" style={{ textAlign: 'center', background: '#d4edda' }}>
            <h3 style={{ margin: '0 0 0.5rem 0', fontFamily: 'monospace' }}>✅ Approved</h3>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#155724' }}>{stats.posts?.approved || 0}</div>
          </div>
          <div className="card" style={{ textAlign: 'center', background: '#f8d7da' }}>
            <h3 style={{ margin: '0 0 0.5rem 0', fontFamily: 'monospace' }}>❌ Rejected</h3>
            <div style={{ fontSize: '2rem', fontWeight: 'bold', color: '#721c24' }}>{stats.posts?.rejected || 0}</div>
          </div>
        </div>
      ) : (
        <div className="card" style={{ textAlign: 'center' }}>Loading...</div>
      )}
    </div>
  );

  // Post review panel
  const PostsReviewView = () => (
    <div>
      <h2 className="section-title">📋 Post Review Queue</h2>
      {loading ? (
        <div className="card" style={{ textAlign: 'center' }}>Loading...</div>
      ) : pendingPosts.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', color: '#666' }}>
          ✨ No pending posts
        </div>
      ) : (
        pendingPosts.map(post => (
          <div key={post.id} className="card" style={{ marginBottom: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
              <div style={{ flex: 1 }}>
                <h3 style={{ margin: '0 0 0.5rem 0' }}>{post.title}</h3>
                <p style={{ margin: '0 0 0.5rem 0', fontFamily: 'Georgia, serif' }}>{post.body}</p>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '0.5rem' }}>
                  {post.tags?.map((tag, i) => (
                    <span key={i} className="pill">#{tag}</span>
                  ))}
                </div>
                <small style={{ fontFamily: 'monospace', color: '#666' }}>
                  Author: {post.author?.displayName || post.author?.username || 'Unknown'} | 
                  AI Result: {post.aiResult || 'None'} | 
                  Confidence: {post.aiConfidence ? (post.aiConfidence * 100).toFixed(0) + '%' : 'N/A'}
                </small>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginLeft: '1rem' }}>
                <button 
                  className="btn btn-primary"
                  onClick={() => approvePost(post.id)}
                  style={{ background: '#28a745', borderColor: '#28a745' }}
                >
                  ✓ Approve
                </button>
                <button 
                  className="btn btn-secondary"
                  onClick={() => {
                    const reason = prompt('Please enter rejection reason:');
                    if (reason) rejectPost(post.id, reason);
                  }}
                  style={{ background: '#dc3545', borderColor: '#dc3545', color: '#fff' }}
                >
                  ✗ Reject
                </button>
              </div>
            </div>
          </div>
        ))
      )}
    </div>
  );

  // User management panel
  const UsersManageView = () => (
    <div>
      <h2 className="section-title">👥 User Management</h2>
      {loading ? (
        <div className="card" style={{ textAlign: 'center' }}>Loading...</div>
      ) : (
        <div className="card">
          <table style={{ width: '100%', borderCollapse: 'collapse', fontFamily: 'monospace' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #333' }}>
                <th style={{ padding: '0.5rem', textAlign: 'left' }}>Username</th>
                <th style={{ padding: '0.5rem', textAlign: 'left' }}>Email</th>
                <th style={{ padding: '0.5rem', textAlign: 'left' }}>Role</th>
                <th style={{ padding: '0.5rem', textAlign: 'left' }}>Status</th>
                <th style={{ padding: '0.5rem', textAlign: 'left' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id} style={{ borderBottom: '1px dashed #ccc' }}>
                  <td style={{ padding: '0.5rem' }}>{user.username || user.displayName}</td>
                  <td style={{ padding: '0.5rem' }}>{user.email}</td>
                  <td style={{ padding: '0.5rem' }}>
                    <span className={`pill ${user.role === 'ADMIN' ? 'active' : ''}`}>
                      {user.role === 'ADMIN' ? 'Admin' : 'User'}
                    </span>
                  </td>
                  <td style={{ padding: '0.5rem' }}>
                    <span style={{ color: user.enabled ? '#28a745' : '#dc3545' }}>
                      {user.enabled ? '✓ Active' : '✗ Disabled'}
                    </span>
                  </td>
                  <td style={{ padding: '0.5rem' }}>
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button 
                        className="btn btn-secondary"
                        style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                        onClick={() => updateUserRole(user.id, user.role === 'ADMIN' ? 'USER' : 'ADMIN')}
                      >
                        {user.role === 'ADMIN' ? 'Demote to User' : 'Promote to Admin'}
                      </button>
                      <button 
                        className="btn btn-secondary"
                        style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}
                        onClick={() => toggleUserStatus(user.id, !user.enabled)}
                      >
                        {user.enabled ? 'Disable' : 'Enable'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );

  return (
    <div>
      <h2 className="section-title">🔧 Admin Panel</h2>
      
      {/* Tab Navigation */}
      <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
        <button 
          className={`pill ${activeTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => setActiveTab('dashboard')}
          style={{ cursor: 'pointer', border: '2px solid #333' }}
        >
          📊 Dashboard
        </button>
        <button 
          className={`pill ${activeTab === 'posts' ? 'active' : ''}`}
          onClick={() => setActiveTab('posts')}
          style={{ cursor: 'pointer', border: '2px solid #333' }}
        >
          📋 Post Review
        </button>
        <button 
          className={`pill ${activeTab === 'users' ? 'active' : ''}`}
          onClick={() => setActiveTab('users')}
          style={{ cursor: 'pointer', border: '2px solid #333' }}
        >
          👥 User Management
        </button>
      </div>

      {/* Content Area */}
      {activeTab === 'dashboard' && <DashboardView />}
      {activeTab === 'posts' && <PostsReviewView />}
      {activeTab === 'users' && <UsersManageView />}
    </div>
  );
};

export default AdminPanel;
