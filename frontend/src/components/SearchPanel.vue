<template>
  <div class="card" style="max-width: 800px; margin: 0 auto; box-shadow: 8px 8px 0px rgba(0,0,0,1)">
    <h2 class="section-title"></h2>
    <div class="flex" style="margin-bottom: 1rem">
      <input
        class="input"
        style="flex: 1"
        placeholder="输入关键词，如 烹饪、住宿、课程"
        v-model="query"
        @keydown.enter="handleSearch"
      />
      <button class="btn btn-primary" @click="handleSearch">
        搜索
      </button>
    </div>
    <p v-if="loading">搜索中...</p>
    <div v-if="result">
      <h4 style="border-bottom: 2px solid #333; padding-bottom: 0.5rem">📝 帖子匹配</h4>
      <p v-if="!result.posts || result.posts.length === 0" style="color: #666">无匹配</p>
      <div
        v-for="post in result.posts"
        :key="post.id"
        class="card"
        style="margin-bottom: 0.5rem"
      >
        <strong>{{ post.title }}</strong>
        <p style="color: #666; font-size: 0.875rem; font-family: Georgia, serif; margin: 0.5rem 0">
          {{ post.body }}
        </p>
        <small style="color: #7c3aed">得分：{{ post.score.toFixed(2) }}</small>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { searchAll } from '../api';

const query = ref('');
const result = ref(null);
const loading = ref(false);

const handleSearch = async () => {
  if (!query.value.trim()) return;
  loading.value = true;
  try {
    const data = await searchAll({ q: query.value });
    result.value = data;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* Styles are in styles.css */
</style>

