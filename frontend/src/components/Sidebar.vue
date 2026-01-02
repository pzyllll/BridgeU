<template>
  <aside class="sidebar">
    <!-- Logo -->
    <div class="sidebar-logo">
      <div class="logo-icon">🌐</div>
      <span>BridgeU</span>
    </div>

    <!-- Navigation -->
    <nav class="sidebar-nav">
      <div class="nav-section-title">{{ t('sidebar.platform') }}</div>
      <div
        v-for="item in navItems"
        :key="item.id"
        :class="['nav-item', { active: currentPage === item.id }]"
        @click="$emit('navigate', item.id)"
      >
        <span>{{ item.icon }}</span>
        <span>{{ item.label }}</span>
      </div>

      <!-- Admin Entry -->
      <template v-if="isAdmin">
        <div class="nav-section-title" style="margin-top: 1rem">{{ t('sidebar.admin') }}</div>
        <div
          :class="['nav-item', { active: currentPage === 'admin' }]"
          @click="$emit('navigate', 'admin')"
          :style="{ background: currentPage === 'admin' ? '#ffd700' : 'transparent' }"
        >
          <span>🔧</span>
          <span>{{ t('sidebar.adminPanel') }}</span>
        </div>
      </template>

      <div style="margin-top: auto; border-top: 2px solid #333">
        <!-- Language Switcher -->
        <div style="
          padding: 0.75rem;
          display: flex;
          justify-content: center;
          gap: 0.5rem;
          border-bottom: 1px solid #e0e0e0
        ">
          <button
            @click="switchLanguage('zh')"
            :style="{
              padding: '0.4rem 0.8rem',
              border: lang === 'zh' ? '2px solid #333' : '1px solid #ccc',
              borderRadius: '4px',
              background: lang === 'zh' ? '#333' : '#fff',
              color: lang === 'zh' ? '#fff' : '#333',
              cursor: 'pointer',
              fontWeight: lang === 'zh' ? 'bold' : 'normal',
              fontSize: '0.85rem',
              transition: 'all 0.2s ease'
            }"
          >
            🇨🇳 中文
          </button>
          <button
            @click="switchLanguage('en')"
            :style="{
              padding: '0.4rem 0.8rem',
              border: lang === 'en' ? '2px solid #333' : '1px solid #ccc',
              borderRadius: '4px',
              background: lang === 'en' ? '#333' : '#fff',
              color: lang === 'en' ? '#fff' : '#333',
              cursor: 'pointer',
              fontWeight: lang === 'en' ? 'bold' : 'normal',
              fontSize: '0.85rem',
              transition: 'all 0.2s ease'
            }"
          >
            🇺🇸 EN
          </button>
        </div>
        
        <div v-if="user" style="padding: 0.5rem; font-family: monospace; font-size: 0.8rem; color: #666">
          {{ user.displayName || user.username }}
          <span v-if="isAdmin" class="pill active" style="margin-left: 0.5rem; font-size: 0.6rem">{{ t('sidebar.admin') }}</span>
        </div>
        <div
          :class="['nav-item', { active: currentPage === 'profile' }]"
          @click="$emit('navigate', 'profile')"
        >
          <span>👤</span>
          <span>{{ t('sidebar.profile') }}</span>
        </div>
        <div
          class="nav-item"
          @click="$emit('navigate', 'logout')"
          style="color: #666"
        >
          <span>🚪</span>
          <span>{{ t('sidebar.logout') }}</span>
        </div>
      </div>
    </nav>
  </aside>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { t, getCurrentLanguage, setLanguage } from '../i18n';

const props = defineProps({
  currentPage: {
    type: String,
    required: true
  },
  isAdmin: {
    type: Boolean,
    default: false
  },
  user: {
    type: Object,
    default: null
  }
});

const emit = defineEmits(['navigate']);

const lang = ref(getCurrentLanguage());

// 让 navItems 依赖于 lang.value，这样当语言变化时会重新计算
const navItems = computed(() => {
  // 读取 lang.value 以确保响应式
  const _ = lang.value; // 让计算属性依赖于 lang.value
  return [
    { id: 'briefing', label: t('sidebar.briefing'), icon: '📰' },
    { id: 'community', label: t('sidebar.communityFeed'), icon: '🏠' },
    { id: 'post', label: t('sidebar.newPost'), icon: '➕' },
    { id: 'messages', label: t('sidebar.messages'), icon: '💬' },
    { id: 'assistant', label: t('sidebar.aiAssistant'), icon: '🤖' },
  ];
});

onMounted(() => {
  const handleLanguageChange = (e) => {
    if (e && e.detail && e.detail.lang) {
      lang.value = e.detail.lang;
    }
  };
  
  if (typeof window !== 'undefined') {
    window.addEventListener('languageChanged', handleLanguageChange);
  }
  
  return () => {
    if (typeof window !== 'undefined') {
      window.removeEventListener('languageChanged', handleLanguageChange);
    }
  };
});

const switchLanguage = (newLang) => {
  setLanguage(newLang);
  lang.value = newLang;
};
</script>

<style scoped>
/* Styles are in styles.css */
</style>

