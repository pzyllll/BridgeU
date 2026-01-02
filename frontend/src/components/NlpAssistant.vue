<template>
  <div class="card" style="max-width: 800px; margin: 0 auto; box-shadow: 8px 8px 0px rgba(0,0,0,1)">
    <h2 class="section-title">🤖 AI Assistant</h2>
    <div class="flex" style="margin-bottom: 1rem; align-items: flex-start">
      <textarea
        class="input"
        style="flex: 1; min-height: 80px"
        placeholder="Ask a question, e.g. Where to eat cheap in Bangkok? How to find Thai friends in Shanghai?"
        v-model="question"
      />
      <button class="btn btn-primary" @click="handleAsk" style="height: 80px">
        {{ loading ? 'Generating...' : 'Ask' }}
      </button>
    </div>
    <div v-if="answer" style="background: #f9f9f9; border: 2px solid #333; padding: 1rem">
      <div style="border-left: 4px solid #2563eb; padding-left: 1rem; margin-bottom: 1rem">
        <p style="white-space: pre-line; font-family: Georgia, serif; line-height: 1.7">
          {{ answer.answer }}
        </p>
      </div>
      <div style="border-top: 2px dashed #333; padding-top: 1rem">
        <strong style="font-size: 0.875rem">📚 Reference Posts:</strong>
        <ul style="margin: 0.5rem 0; padding-left: 1.5rem">
          <li
            v-for="ref in answer.references"
            :key="ref.id"
            style="font-size: 0.875rem; margin-bottom: 4px"
          >
            {{ ref.title }}
            <span style="color: #7c3aed; margin-left: 0.5rem">(Score {{ ref.score.toFixed(2) }})</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { askQuestion } from '../api';

const question = ref('');
const answer = ref(null);
const loading = ref(false);

const handleAsk = async () => {
  if (!question.value.trim()) return;
  loading.value = true;
  try {
    const data = await askQuestion({ question: question.value });
    answer.value = data;
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

