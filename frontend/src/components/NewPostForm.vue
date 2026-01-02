<template>
  <div class="card" style="max-width: 600px; margin: 0 auto; box-shadow: 8px 8px 0px rgba(0,0,0,1)">
    <h2 class="section-title">{{ t('newPost.title') }}</h2>
    <div style="
      margin-bottom: 1rem;
      padding: 0.75rem;
      background: #e0f2fe;
      border: 2px solid #0ea5e9;
      border-radius: 4px;
      font-size: 0.875rem
    ">
      <strong>{{ t('newPost.autoTranslation') }}</strong> {{ t('newPost.autoTranslationDesc') }}
    </div>
    <div style="
      margin-bottom: 1rem;
      padding: 0.65rem;
      background: #fff7ed;
      border: 1px solid #fed7aa;
      border-radius: 4px;
      font-size: 0.8rem;
      color: #7c2d12
    ">
      <strong>内容安全提示：</strong>
      帖子文字和图片会先经过 AI 实时审核，高风险内容会被自动拦截；
      边界内容将进入人工审核队列，管理员会做最终判断。
      <br />
      <span style="font-size: 0.75rem; color: #92400e">
        Content safety notice: Your text and images will be screened by AI and human moderators. High-risk content is blocked automatically; borderline content will be marked as
        <em> pending review</em> before publication.
      </span>
    </div>
    
    <form @submit.prevent="handleSubmit">
      <!-- Tag Selection -->
      <div style="margin-bottom: 1rem">
        <label style="display: block; font-size: 10px; font-weight: bold; text-transform: uppercase; margin-bottom: 8px">
          {{ t('newPost.selectTag') }}
        </label>
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap">
          <button
            v-for="tag in TAG_OPTIONS"
            :key="tag.value"
            type="button"
            :class="['pill', { active: selectedTag === tag.value }]"
            @click="selectedTag = tag.value"
          >
            {{ tag.label }}
          </button>
        </div>
      </div>

      <!-- Title -->
      <div style="margin-bottom: 1rem">
        <label style="display: block; font-size: 10px; font-weight: bold; text-transform: uppercase; margin-bottom: 4px">
          {{ t('newPost.postTitle') }}
        </label>
        <input
          class="input"
          :placeholder="t('newPost.postTitlePlaceholder')"
          v-model="form.title"
        />
      </div>

      <!-- Content -->
      <div style="margin-bottom: 1rem">
        <label style="display: block; font-size: 10px; font-weight: bold; text-transform: uppercase; margin-bottom: 4px">
          {{ t('newPost.content') }}
        </label>
        <textarea
          class="input"
          style="min-height: 120px"
          :placeholder="t('newPost.contentPlaceholder')"
          v-model="form.body"
        />
      </div>

      <!-- Image Upload -->
      <div
        style="
          margin-bottom: 1rem;
          border: 2px dashed #333;
          background: #f9f9f9;
          min-height: 100px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          cursor: pointer;
          padding: 8px
        "
        @click="triggerFileInput"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleImageChange"
        />
        <span style="font-size: 24px; margin-bottom: 4px">🖼️</span>
        <span style="font-size: 10px; font-weight: bold; text-transform: uppercase; color: #666">
          {{ lang === 'zh' ? '点击上传图片（可选）' : 'Click to upload image (optional)' }}
        </span>
        <div v-if="imagePreview" style="position: relative; margin-top: 8px; width: 100%; display: flex; justify-content: center;">
          <img
            :src="imagePreview"
            alt="preview"
            style="max-width: 100%; max-height: 160px; border-radius: 6px"
          />
          <button
            type="button"
            @click.stop="removeImage"
            style="
              position: absolute;
              top: -8px;
              right: -8px;
              background: #dc2626;
              color: white;
              border: none;
              border-radius: 50%;
              width: 24px;
              height: 24px;
              cursor: pointer;
              font-size: 14px;
              display: flex;
              align-items: center;
              justify-content: center;
              box-shadow: 0 2px 4px rgba(0,0,0,0.2);
            "
          >
            ×
          </button>
        </div>
      </div>

      <!-- Notice -->
      <div
        v-if="notice"
        style="
          margin-bottom: 1rem;
          padding: 0.75rem;
          border: 2px solid #333;
          background: notice.includes('✅') ? '#dcfce7' : '#fef2f2';
          font-size: 0.875rem;
          font-weight: bold;
        "
      >
        {{ notice }}
      </div>

      <!-- Submit -->
      <button
        class="btn btn-primary"
        type="submit"
        style="width: 100%"
        :disabled="isSubmitting"
      >
        {{ isSubmitting ? t('newPost.publishing') : t('newPost.publish') }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { createPost, uploadPostImage } from '../api';
import { t, getCurrentLanguage } from '../i18n';

const props = defineProps({
  currentUserId: {
    type: Number,
    default: null
  }
});

const defaultForm = {
  communityId: '',
  title: '',
  body: '',
  tags: '',
  imageUrl: '',
};

const TAG_OPTIONS = [
  { label: '📚 Study', value: 'Study' },
  { label: '🏠 Housing', value: 'Housing' },
  { label: '✈️ Travel', value: 'Travel' },
  { label: '💼 Part-time Job', value: 'Part-time Job' },
  { label: '🛒 Life Services', value: 'Life Services' },
];

const form = ref({ ...defaultForm });
const notice = ref('');
const selectedTag = ref('');
const isSubmitting = ref(false);
const lang = ref(getCurrentLanguage());
const imageFile = ref(null);
const imagePreview = ref('');
const fileInputRef = ref(null);

const triggerFileInput = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click();
  }
};

const handleImageChange = (e) => {
  const file = e.target.files?.[0];
  if (!file) return;
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    notice.value = lang.value === 'zh' ? '请选择图片文件' : 'Please select an image file';
    return;
  }
  
  // 验证文件大小（例如：最大5MB）
  const maxSize = 5 * 1024 * 1024; // 5MB
  if (file.size > maxSize) {
    notice.value = lang.value === 'zh' ? '图片大小不能超过5MB' : 'Image size cannot exceed 5MB';
    return;
  }
  
  imageFile.value = file;
  const reader = new FileReader();
  reader.onload = (ev) => {
    imagePreview.value = ev.target?.result || '';
  };
  reader.readAsDataURL(file);
  notice.value = ''; // 清除之前的错误信息
};

const removeImage = () => {
  imageFile.value = null;
  imagePreview.value = '';
  // 清除文件输入
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const handleSubmit = async () => {
  const storedUser = (() => {
    try {
      return JSON.parse(localStorage.getItem('user') || '{}');
    } catch (e) {
      return {};
    }
  })();
  const authorId = props.currentUserId || storedUser?.id;
  if (!authorId) {
    notice.value = '请先登录，系统将自动使用当前用户ID';
    return;
  }
  if (!selectedTag.value) {
    notice.value = lang.value === 'zh' ? '请选择一个标签' : 'Please select a tag';
    return;
  }
  if (!form.value.title.trim()) {
    notice.value = lang.value === 'zh' ? '标题不能为空' : 'Title cannot be empty';
    return;
  }
  if (!form.value.body.trim()) {
    notice.value = lang.value === 'zh' ? '内容不能为空' : 'Content cannot be empty';
    return;
  }
  const communityId = form.value.communityId?.trim() || 'default';
  isSubmitting.value = true;
  notice.value = '';
  try {
    const token = localStorage.getItem('token');
    let imageUrl = '';

    // 如果有图片，先上传图片
    if (imageFile.value) {
      try {
        const uploadResp = await uploadPostImage(imageFile.value, token);
        imageUrl = uploadResp.url;
      } catch (uploadError) {
        console.error('Image upload failed:', uploadError);
        notice.value = lang.value === 'zh' 
          ? '图片上传失败，请重试' 
          : 'Image upload failed, please try again';
        isSubmitting.value = false;
        return;
      }
    }

    await createPost({
      communityId,
      authorId,
      title: form.value.title,
      body: form.value.body,
      tags: [selectedTag.value], // 使用选中的标签（已验证不为空）
      imageUrl: imageUrl,
    }, token);
    notice.value = t('newPost.success');
    form.value = { ...defaultForm, communityId: form.value.communityId };
    selectedTag.value = '';
    imageFile.value = null;
    imagePreview.value = '';
  } catch (error) {
    console.error(error);
    notice.value = t('newPost.failed');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
/* Styles are in styles.css */
</style>

