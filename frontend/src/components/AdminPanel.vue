<template>
  <div>
    <h2 class="section-title">🔧 Admin Panel</h2>
    
    <!-- Tab Navigation -->
    <div style="display: flex; gap: 0.5rem; margin-bottom: 1rem">
      <button
        :class="['pill', { active: activeTab === 'dashboard' }]"
        @click="activeTab = 'dashboard'"
        style="cursor: pointer; border: 2px solid #333"
      >
        📊 Dashboard
      </button>
      <button
        :class="['pill', { active: activeTab === 'posts' }]"
        @click="activeTab = 'posts'"
        style="cursor: pointer; border: 2px solid #333"
      >
        📋 Post Review
      </button>
      <button
        :class="['pill', { active: activeTab === 'users' }]"
        @click="activeTab = 'users'"
        style="cursor: pointer; border: 2px solid #333"
      >
        👥 User Management
      </button>
    </div>

    <!-- Dashboard View -->
    <div v-if="activeTab === 'dashboard'">
      <h2 class="section-title">📊 Admin Dashboard</h2>
      <div v-if="stats" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem">
        <div class="card" style="text-align: center">
          <h3 style="margin: 0 0 0.5rem 0; font-family: monospace">👥 User Statistics</h3>
          <div style="font-size: 2rem; font-weight: bold">{{ stats.users?.total || 0 }}</div>
          <small style="font-family: monospace">
            Admins: {{ stats.users?.admins || 0 }} | Regular Users: {{ stats.users?.regularUsers || 0 }}
          </small>
        </div>
        <div class="card" style="text-align: center">
          <h3 style="margin: 0 0 0.5rem 0; font-family: monospace">📝 Total Posts</h3>
          <div style="font-size: 2rem; font-weight: bold">{{ stats.posts?.total || 0 }}</div>
        </div>
        <div class="card" style="text-align: center; background: #fff3cd">
          <h3 style="margin: 0 0 0.5rem 0; font-family: monospace">⏳ Pending</h3>
          <div style="font-size: 2rem; font-weight: bold; color: #856404">{{ stats.posts?.pending || 0 }}</div>
        </div>
        <div class="card" style="text-align: center; background: #d4edda">
          <h3 style="margin: 0 0 0.5rem 0; font-family: monospace">✅ Approved</h3>
          <div style="font-size: 2rem; font-weight: bold; color: #155724">{{ stats.posts?.approved || 0 }}</div>
        </div>
        <div class="card" style="text-align: center; background: #f8d7da">
          <h3 style="margin: 0 0 0.5rem 0; font-family: monospace">❌ Rejected</h3>
          <div style="font-size: 2rem; font-weight: bold; color: #721c24">{{ stats.posts?.rejected || 0 }}</div>
        </div>
      </div>
      <div v-else class="card" style="text-align: center">Loading...</div>
    </div>

    <!-- Posts Review View -->
    <div v-if="activeTab === 'posts'">
      <h2 class="section-title">📋 Post Review Queue</h2>
      <div v-if="loading" class="card" style="text-align: center">Loading...</div>
      <div v-else-if="pendingPosts.length === 0" class="card" style="text-align: center; color: #666">
        ✨ No pending posts
      </div>
      <div v-else>
        <div
          v-for="post in pendingPosts"
          :key="post.id"
          class="card"
          style="margin-bottom: 1rem"
        >
          <div style="display: flex; justify-content: space-between; align-items: flex-start">
            <div style="flex: 1">
              <h3 style="margin: 0 0 0.5rem 0">{{ post.title }}</h3>
              <p style="margin: 0 0 0.5rem 0; font-family: Georgia, serif">{{ post.body }}</p>
              <div style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.5rem">
                <span v-for="(tag, i) in post.tags" :key="i" class="pill">#{{ tag }}</span>
              </div>
              <small style="font-family: monospace; color: #666">
                Author: {{ post.author?.displayName || post.author?.username || 'Unknown' }} |
                AI Result: {{ post.aiResult || 'None' }} |
                Confidence: {{ post.aiConfidence ? (post.aiConfidence * 100).toFixed(0) + '%' : 'N/A' }}
              </small>
            </div>
            <div style="display: flex; flex-direction: column; gap: 0.5rem; margin-left: 1rem">
              <button
                class="btn btn-primary"
                @click="approvePost(post.id)"
                style="background: #28a745; border-color: #28a745"
              >
                ✓ Approve
              </button>
              <button
                class="btn btn-secondary"
                @click="handleRejectPost(post.id)"
                style="background: #dc3545; border-color: #dc3545; color: #fff"
              >
                ✗ Reject
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Users Management View -->
    <div v-if="activeTab === 'users'">
      <h2 class="section-title">👥 User Management</h2>
      <div v-if="loading" class="card" style="text-align: center">Loading...</div>
      <div v-else class="card">
        <table style="width: 100%; border-collapse: collapse; font-family: monospace">
          <thead>
            <tr style="border-bottom: 2px solid #333">
              <th style="padding: 0.5rem; text-align: left">Username</th>
              <th style="padding: 0.5rem; text-align: left">Email</th>
              <th style="padding: 0.5rem; text-align: left">Role</th>
              <th style="padding: 0.5rem; text-align: left">Status</th>
              <th style="padding: 0.5rem; text-align: left">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id" style="border-bottom: 1px dashed #ccc">
              <td style="padding: 0.5rem">{{ user.username || user.displayName }}</td>
              <td style="padding: 0.5rem">{{ user.email }}</td>
              <td style="padding: 0.5rem">
                <span :class="['pill', { active: user.role === 'ADMIN' }]">
                  {{ user.role === 'ADMIN' ? 'Admin' : 'User' }}
                </span>
              </td>
              <td style="padding: 0.5rem">
                <span :style="{ color: user.enabled ? '#28a745' : '#dc3545' }">
                  {{ user.enabled ? '✓ Active' : '✗ Disabled' }}
                </span>
              </td>
              <td style="padding: 0.5rem">
                <div style="display: flex; gap: 0.5rem">
                  <button
                    class="btn btn-secondary"
                    style="font-size: 0.75rem; padding: 0.25rem 0.5rem"
                    @click="updateUserRole(user.id, user.role === 'ADMIN' ? 'USER' : 'ADMIN')"
                  >
                    {{ user.role === 'ADMIN' ? 'Demote to User' : 'Promote to Admin' }}
                  </button>
                  <button
                    class="btn btn-secondary"
                    style="font-size: 0.75rem; padding: 0.25rem 0.5rem"
                    @click="toggleUserStatus(user.id, !user.enabled)"
                  >
                    {{ user.enabled ? 'Disable' : 'Enable' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';

const props = defineProps({
  token: {
    type: String,
    required: true
  }
});

const API_BASE = 'http://localhost:8080/api';

const activeTab = ref('dashboard');
const stats = ref(null);
const pendingPosts = ref([]);
const users = ref([]);
const loading = ref(false);

const headers = computed(() => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${props.token}`
}));

const fetchDashboard = async () => {
  try {
    const res = await fetch(`${API_BASE}/admin/dashboard`, { headers: headers.value });
    if (res.ok) {
      const data = await res.json();
      stats.value = data;
    }
  } catch (err) {
    console.error('Failed to fetch dashboard data:', err);
  }
};

const fetchPendingPosts = async () => {
  loading.value = true;
  try {
    const res = await fetch(`${API_BASE}/admin/posts/pending`, { headers: headers.value });
    if (res.ok) {
      const data = await res.json();
      pendingPosts.value = data.data || [];
    }
  } catch (err) {
    console.error('Failed to fetch pending posts:', err);
  }
  loading.value = false;
};

const fetchUsers = async () => {
  loading.value = true;
  try {
    const res = await fetch(`${API_BASE}/admin/users`, { headers: headers.value });
    if (res.ok) {
      const data = await res.json();
      users.value = data.data || [];
    }
  } catch (err) {
    console.error('Failed to fetch user list:', err);
  }
  loading.value = false;
};

const approvePost = async (postId) => {
  try {
    const res = await fetch(`${API_BASE}/admin/posts/${postId}/approve`, {
      method: 'POST',
      headers: headers.value,
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

const handleRejectPost = async (postId) => {
  const reason = prompt('Please enter rejection reason:');
  if (reason) {
    await rejectPost(postId, reason);
  }
};

const rejectPost = async (postId, reason) => {
  try {
    const res = await fetch(`${API_BASE}/admin/posts/${postId}/reject`, {
      method: 'POST',
      headers: headers.value,
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

const updateUserRole = async (userId, newRole) => {
  try {
    const res = await fetch(`${API_BASE}/admin/users/${userId}/role`, {
      method: 'PATCH',
      headers: headers.value,
      body: JSON.stringify({ role: newRole })
    });
    if (res.ok) {
      fetchUsers();
    }
  } catch (err) {
    console.error('Failed to update role:', err);
  }
};

const toggleUserStatus = async (userId, enabled) => {
  try {
    const res = await fetch(`${API_BASE}/admin/users/${userId}/status`, {
      method: 'PATCH',
      headers: headers.value,
      body: JSON.stringify({ enabled })
    });
    if (res.ok) {
      fetchUsers();
    }
  } catch (err) {
    console.error('Failed to update user status:', err);
  }
};

onMounted(() => {
  fetchDashboard();
});

watch(activeTab, (newTab) => {
  if (newTab === 'posts') fetchPendingPosts();
  if (newTab === 'users') fetchUsers();
});
</script>


<style scoped>
/* Styles are in styles.css */
</style>

