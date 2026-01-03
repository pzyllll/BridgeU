<template>
  <div class="chat-window" :key="lang">
    <!-- Chat Header -->
    <div class="chat-header">
      <div class="chat-user-info" @click="handleViewUserProfile" style="cursor: pointer;" :title="t('messages.viewProfile')">
        <div class="chat-avatar">
          {{ (otherUser?.displayName || otherUser?.username || 'U')[0].toUpperCase() }}
        </div>
        <div class="chat-user-details">
          <h3 class="chat-user-name">
            {{ otherUser?.displayName || otherUser?.username || t('messages.anonymous') }}
          </h3>
          <p class="chat-user-status">Online</p>
        </div>
      </div>
      <button
        v-if="onBack"
        class="btn-icon"
        @click="onBack"
        :title="t('common.back')"
      >
        ←
      </button>
    </div>

    <!-- Safety Warning -->
    <div class="safety-warning" v-if="!isMutualFollow">
      <div class="warning-icon">⚠️</div>
      <div class="warning-content">
        <p class="warning-title">{{ t('messages.safetyWarning') }}</p>
        <p class="warning-text">{{ t('messages.safetyWarningText') }}</p>
        <p v-if="!canSendMore" class="warning-limit">{{ t('messages.singleMessageLimit') }}</p>
      </div>
    </div>

    <!-- Messages Container -->
    <div class="messages-container" ref="messagesContainer">
      <div v-if="loading" class="loading-messages">
        <p>{{ t('messages.loading') }}</p>
      </div>
      <div v-else-if="error" class="error-messages">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="loadMessages">
          {{ t('common.retry') }}
        </button>
      </div>
      <div v-else-if="messages.length === 0" class="empty-messages">
        <p>{{ t('messages.emptyConversation') }}</p>
      </div>
      <div v-else class="messages-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ 'message-sent': msg.senderId === currentUserId, 'message-received': msg.senderId !== currentUserId }"
        >
          <div class="message-content">
            <p class="message-text">{{ msg.content }}</p>
            <span class="message-time">{{ formatTime(msg.createdAt) }}</span>
            <span v-if="msg.senderId === currentUserId && msg.isRead" class="message-read">✓✓</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Message Input -->
    <div class="message-input-container">
      <input
        v-model="messageText"
        :placeholder="canSendMore ? t('messages.messagePlaceholder') : t('messages.waitForFollowBack')"
        class="message-input"
        @keyup.enter="handleSendMessage"
        :disabled="sending || !canSendMore"
      />
      <button
        class="btn btn-primary send-button"
        @click="handleSendMessage"
        :disabled="!messageText.trim() || sending || !canSendMore"
      >
        {{ sending ? t('messages.sending') : t('messages.sendMessage') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue';
import { getConversationMessages, sendMessage, markConversationAsRead } from '../api';
import { getCurrentLanguage, t } from '../i18n';

const props = defineProps({
  conversationId: {
    type: String,
    required: true
  },
  token: {
    type: String,
    required: true
  },
  currentUserId: {
    type: String,
    required: true
  },
  otherUser: {
    type: Object,
    default: null
  },
  onBack: {
    type: Function,
    default: null
  },
  onMessageSent: {
    type: Function,
    default: null
  },
  onViewUserProfile: {
    type: Function,
    default: null
  }
});

const loading = ref(false);
const error = ref(null);
const messages = ref([]);
const messageText = ref('');
const sending = ref(false);
const messagesContainer = ref(null);
// Initialize to false to avoid showing wrong state before data loads
const isMutualFollow = ref(false);
const canSendMore = ref(false);

// Add reactive language state to trigger re-renders
const lang = ref(getCurrentLanguage());

const loadMessages = async () => {
  if (!props.conversationId || !props.token) {
    return;
  }

  loading.value = true;
  error.value = null;

  try {
    const response = await getConversationMessages(props.conversationId, props.token);
    if (response.success) {
      messages.value = response.messages || [];
      
      // Debug: Log the raw response from backend
      console.log('ChatWindow: Raw response from backend', {
        isMutualFollow: response.isMutualFollow,
        isMutualFollowType: typeof response.isMutualFollow,
        canSendMore: response.canSendMore,
        canSendMoreType: typeof response.canSendMore,
        allKeys: Object.keys(response),
        fullResponse: JSON.stringify(response, null, 2)
      });
      
      // Update follow status - handle boolean values properly
      // Convert to boolean explicitly to handle any type issues
      if (response.isMutualFollow !== undefined && response.isMutualFollow !== null) {
        isMutualFollow.value = Boolean(response.isMutualFollow);
      } else {
        // If not provided, default to false for safety
        isMutualFollow.value = false;
      }
      
      if (response.canSendMore !== undefined && response.canSendMore !== null) {
        canSendMore.value = Boolean(response.canSendMore);
      } else {
        // If canSendMore is not provided, calculate it based on isMutualFollow
        // If mutual follow, can always send; if not, check message count
        const messagesSentByCurrentUser = messages.value.filter(m => m.senderId === props.currentUserId).length;
        canSendMore.value = isMutualFollow.value || messagesSentByCurrentUser < 1;
      }
      
      // Debug log to help diagnose issues
      console.log('ChatWindow: Updated status', {
        isMutualFollow: isMutualFollow.value,
        canSendMore: canSendMore.value,
        messagesCount: messages.value.length,
        messagesSentByCurrentUser: messages.value.filter(m => m.senderId === props.currentUserId).length,
        responseIsMutualFollow: response.isMutualFollow,
        responseCanSendMore: response.canSendMore
      });
      // Mark conversation as read
      await markConversationAsRead(props.conversationId, props.token);
      // Scroll to bottom
      await nextTick();
      scrollToBottom();
    } else {
      error.value = response.message || t('messages.loadFailed');
    }
  } catch (err) {
    console.error('Failed to load messages:', err);
    error.value = err.response?.data?.message || t('messages.loadFailed');
  } finally {
    loading.value = false;
  }
};

const handleSendMessage = async () => {
  if (!messageText.value.trim() || sending.value) {
    return;
  }

  const content = messageText.value.trim();
  messageText.value = '';
  sending.value = true;

  try {
    const response = await sendMessage(props.conversationId, content, props.token);
    if (response.success) {
      // Update follow status
      if (response.isMutualFollow !== undefined) {
        isMutualFollow.value = response.isMutualFollow === true;
      }
      // Reload messages to get the new one and update canSendMore
      await loadMessages();
      // Notify parent
      if (props.onMessageSent) {
        props.onMessageSent();
      }
    } else {
      const errorMsg = response.message || t('messages.sendFailed');
      alert(errorMsg);
      // If single message limit reached, update canSendMore
      if (response.code === 'SINGLE_MESSAGE_LIMIT') {
        canSendMore.value = false;
      }
      messageText.value = content; // Restore message on error
    }
  } catch (err) {
    console.error('Failed to send message:', err);
    const errorMsg = err.response?.data?.message || t('messages.sendFailed');
    alert(errorMsg);
    // If single message limit reached, update canSendMore
    if (err.response?.data?.code === 'SINGLE_MESSAGE_LIMIT') {
      canSendMore.value = false;
    }
    messageText.value = content; // Restore message on error
  } finally {
    sending.value = false;
  }
};

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

const handleViewUserProfile = () => {
  if (props.otherUser?.id && props.onViewUserProfile) {
    props.onViewUserProfile(props.otherUser.id);
  }
};

const formatTime = (timestamp) => {
  if (!timestamp) return '';
  
  // Parse timestamp and validate
  let time;
  try {
    time = new Date(timestamp);
    // Check if date is invalid or before 1971 (to catch 1970 dates)
    if (isNaN(time.getTime()) || time.getTime() < 0 || time.getFullYear() < 1971) {
      time = new Date(); // Use current time as fallback
    }
  } catch (e) {
    time = new Date(); // Use current time as fallback
  }
  
  // Calculate time difference (timezone-independent)
  const now = new Date();
  const diffMs = now.getTime() - time.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMs / 3600000);
  const diffDays = Math.floor(diffMs / 86400000);

  // Show relative time for recent messages
  if (diffMins < 1) {
    return t('messages.justNow');
  } else if (diffMins < 60) {
    return `${diffMins}${t('messages.minutesAgo')}`;
  } else if (diffHours < 24) {
    return `${diffHours}${t('messages.hoursAgo')}`;
  } else if (diffDays < 7) {
    return `${diffDays}${t('messages.daysAgo')}`;
  } else {
    // For older messages, format with Thailand timezone
    const formatter = new Intl.DateTimeFormat(lang.value === 'zh' ? 'zh-CN' : 'en-US', {
      timeZone: 'Asia/Bangkok',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
    return formatter.format(time);
  }
};

// Watch for conversation changes
watch(() => props.conversationId, (newId) => {
  if (newId) {
    loadMessages();
  } else {
    messages.value = [];
  }
});

// Listen for language changes to update UI text
const handleLanguageChange = (e) => {
  if (e && e.detail && e.detail.lang) {
    lang.value = e.detail.lang;
    // Force re-render by updating a reactive value
    // The t() function will automatically use the new language
  }
};

onMounted(() => {
  // Add language change listener
  if (typeof window !== 'undefined') {
    window.addEventListener('languageChanged', handleLanguageChange);
  }
});

onUnmounted(() => {
  // Clean up language change listener
  if (typeof window !== 'undefined') {
    window.removeEventListener('languageChanged', handleLanguageChange);
  }
});

onMounted(() => {
  if (props.conversationId) {
    loadMessages();
  }
});

// Expose refresh method
defineExpose({
  refresh: loadMessages
});
</script>

<style scoped>
.chat-window {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 800px;
  margin: 0 auto;
  border: 2px solid #ddd;
  border-radius: 8px;
  background: white;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  border-bottom: 2px solid #ddd;
  background: #f9f9f9;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #333;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 1rem;
}

.chat-user-details {
  flex: 1;
}

.chat-user-name {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}

.chat-user-status {
  font-size: 0.75rem;
  color: #666;
  margin: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  background: #f5f5f5;
}

.loading-messages,
.error-messages,
.empty-messages {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  text-align: center;
  color: #666;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message-item {
  display: flex;
  width: 100%;
}

.message-sent {
  justify-content: flex-end;
}

.message-received {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  position: relative;
}

.message-sent .message-content {
  background: #333;
  color: white;
  border-bottom-right-radius: 4px;
}

.message-received .message-content {
  background: white;
  color: #333;
  border: 2px solid #ddd;
  border-bottom-left-radius: 4px;
}

.message-text {
  margin: 0 0 0.25rem 0;
  word-wrap: break-word;
}

.message-time {
  font-size: 0.75rem;
  opacity: 0.7;
  display: block;
}

.message-read {
  font-size: 0.75rem;
  opacity: 0.7;
  margin-left: 0.5rem;
}

.message-input-container {
  display: flex;
  gap: 0.5rem;
  padding: 1rem;
  border-top: 2px solid #ddd;
  background: #f9f9f9;
}

.message-input {
  flex: 1;
  padding: 0.75rem;
  border: 2px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  font-family: inherit;
}

.message-input:focus {
  outline: none;
  border-color: #333;
}

.send-button {
  padding: 0.75rem 1.5rem;
  white-space: nowrap;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1.2rem;
  padding: 0.5rem;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.btn-icon:hover {
  opacity: 1;
}

.btn {
  padding: 0.5rem 1rem;
  border: 2px solid #333;
  background: white;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s;
}

.btn:hover:not(:disabled) {
  background: #333;
  color: white;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #333;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #555;
}

.safety-warning {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  background: #fff3cd;
  border-bottom: 2px solid #ffc107;
  border-left: 4px solid #ffc107;
}

.warning-icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
}

.warning-title {
  font-weight: 600;
  margin: 0 0 0.25rem 0;
  color: #856404;
  font-size: 0.9rem;
}

.warning-text {
  margin: 0 0 0.25rem 0;
  color: #856404;
  font-size: 0.85rem;
  line-height: 1.4;
}

.warning-limit {
  margin: 0.5rem 0 0 0;
  color: #d32f2f;
  font-size: 0.85rem;
  font-weight: 600;
}
</style>

