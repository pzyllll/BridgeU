<template>
  <div class="login-container">
    <!-- Firebase reCAPTCHA 容器（隐藏，用于 invisible 模式） -->
    <div id="recaptcha-container" style="position: fixed; top: -9999px; left: -9999px; width: 1px; height: 1px; overflow: hidden;"></div>
    <!-- Language Switcher -->
    <div class="lang-switcher">
      <button
        class="lang-btn"
        @click="handleLanguageSwitch(lang === 'en' ? 'zh' : 'en')"
      >
        <span :class="{ active: lang === 'en' }">EN</span>
        <span class="lang-separator">/</span>
        <span :class="{ active: lang === 'zh' }">中文</span>
      </button>
    </div>

    <div class="login-box">
      <!-- Header -->
      <div class="login-header">
        <div class="logo">🌐</div>
        <h1>BridgeU</h1>
        <p>{{ t('login.subtitle') }}</p>
      </div>

      <!-- Login Form -->
      <template v-if="!isRegister && !isForgotPassword">
        <form class="login-form" @submit.prevent="handleLogin">
          <div v-if="error" class="error-message">
          {{ error }}
        </div>
        <div class="form-group">
          <label>{{ t('login.username') }}</label>
          <input
            type="text"
            class="input"
              :placeholder="t('login.username')"
              v-model="loginForm.username"
              required
            />
          </div>
        <div class="form-group">
          <label>{{ t('login.password') }}</label>
          <input
            type="password"
            class="input"
              :placeholder="t('login.password')"
              v-model="loginForm.password"
            required
          />
        </div>
        <button 
          type="submit" 
          class="btn btn-primary" 
          style="width: 100%; margin-top: 1rem"
          :disabled="loading"
        >
            {{ loading ? t('login.pleaseWait') : t('login.login') }}
        </button>
          <div class="form-footer">
            <span class="link" @click="isRegister = true" style="cursor: pointer">
              {{ t('login.create') }}
            </span>
            <span class="link" @click="isForgotPassword = true" style="cursor: pointer">
              {{ t('login.forgotPassword') }}
            </span>
        </div>
      </form>
      </template>

      <!-- Registration Form -->
      <template v-else-if="isRegister">
        <!-- Registration: All fields in one page -->
        <div class="login-form">
          <div v-if="error" class="error-message">
          {{ error }}
        </div>
          <h3 style="margin-bottom: 1rem">{{ t('login.completeRegistration') || '完成注册' }}</h3>
          
          <!-- Email/Phone Input with Method Selector -->
        <div class="form-group">
            <label>{{ t('login.emailOrPhone') }}</label>
            <div class="input-group">
              <!-- Method Selector (left side, 类似 +86 选择器) -->
              <div class="method-selector-wrapper">
                <select
                  class="input method-selector"
                  v-model="registerMethod"
                  @change="codeSent = false; codeVerified = false; registerForm.identifier = '';"
                >
                  <option value="email">{{ t('login.email') }}</option>
                  <option value="phone">{{ t('login.phone') }}</option>
                </select>
              </div>
              <!-- Input Field (middle) -->
          <input
                :type="registerMethod === 'email' ? 'email' : 'tel'"
                class="input input-field"
                :placeholder="registerMethod === 'email' ? t('login.enterEmail') : t('login.enterPhone')"
                v-model="registerForm.identifier"
                @input="onIdentifierInput"
            required
          />
              <!-- Send Code Button (right side) -->
              <button 
                type="button" 
                class="btn btn-primary send-code-btn"
                @click="sendVerificationCode"
                :disabled="loading || !registerForm.identifier || codeSent"
              >
                {{ codeSent ? (lang === 'zh' ? '已发送' : 'Sent') : (loading ? t('login.pleaseWait') : t('login.sendCode')) }}
              </button>
            </div>
            <small v-if="codeSent" class="success-message">
              ✓ {{ t('login.codeSentTo') }} {{ maskedIdentifier }}
            </small>
        </div>
          
          <!-- Verification Code Section (shown after code sent) -->
          <div v-if="codeSent" class="form-group">
            <label>
              {{ t('login.verificationCode') }}
              <span v-if="codeVerified" class="verified-badge">
                ✓ {{ lang === 'zh' ? '已验证' : 'Verified' }}
              </span>
            </label>
            <div class="code-input-group">
          <input
                type="text"
                class="input code-input"
                :class="{ 'verified': codeVerified }"
                :placeholder="t('login.verificationCode')"
                v-model="registerForm.code"
                maxlength="6"
                @input="onCodeInput"
                :disabled="codeVerified"
            required
          />
              <button 
                type="button" 
                class="btn verify-btn"
                :class="{ 'btn-primary': !codeVerified, 'btn-success': codeVerified }"
                @click="verifyCode"
                :disabled="loading || codeVerified || !registerForm.code || registerForm.code.length !== 6"
              >
                {{ codeVerified ? (lang === 'zh' ? '已验证' : 'Verified') : (loading ? t('login.pleaseWait') : t('login.verify')) }}
              </button>
        </div>
            <small class="form-hint">
              {{ lang === 'zh' ? '输入6位验证码后点击验证' : 'Enter 6-digit code and click verify' }}
            </small>
        </div>

          <!-- Username Section -->
        <div class="form-group">
            <label>{{ t('login.username') }}</label>
          <input
            type="text"
            class="input"
              :placeholder="t('login.username')"
              v-model="registerForm.username"
              pattern="[A-Za-z0-9_]+"
            required
          />
            <small style="color: #666; font-size: 0.8rem">{{ t('login.usernameRequirements') }}</small>
        </div>
        <div class="form-group">
          <label>{{ t('login.password') }}</label>
          <input
            type="password"
            class="input"
              :placeholder="t('login.password')"
              v-model="registerForm.password"
            required
          />
            <small style="color: #666; font-size: 0.8rem">{{ t('login.passwordRequirements') }}</small>
        </div>
        <div class="form-group">
            <label>{{ t('login.confirmPassword') }}</label>
          <input
              type="password"
            class="input"
              :placeholder="t('login.confirmPassword')"
              v-model="registerForm.confirmPassword"
            required
          />
        </div>
        <div class="form-group">
            <label>{{ t('login.preferredLanguage') }}</label>
          <select
            class="input"
              v-model="registerForm.preferredLanguage"
            required
          >
              <option value="en">English</option>
            <option value="zh">中文</option>
          </select>
        </div>
        <button
            type="button" 
            class="btn btn-primary register-btn" 
            @click="completeRegistration"
            :disabled="loading || !codeSent || (registerMethod === 'email' && !codeVerified)"
          >
            {{ loading ? t('login.pleaseWait') : t('login.register') }}
          </button>
          
          <div class="form-footer">
            <span v-if="codeSent" class="link" @click="sendVerificationCode" style="cursor: pointer">
              {{ t('login.resendCode') }}
            </span>
            <span class="link" @click="backToLogin" style="cursor: pointer; margin-left: auto">
              {{ t('login.backToLogin') }}
            </span>
          </div>
        </div>
      </template>

      <!-- Forgot Password Form -->
      <template v-else-if="isForgotPassword">
        <!-- Forgot Password: All fields in one page (same as registration) -->
        <div class="login-form">
          <div v-if="error" class="error-message">
            {{ error }}
          </div>
          <h3 style="margin-bottom: 1rem">{{ t('login.resetPassword') }}</h3>
          
          <!-- Email/Phone Input with Method Selector -->
          <div class="form-group">
            <label>{{ t('login.emailOrPhone') }}</label>
            <div class="input-group">
              <!-- Method Selector (left side) -->
              <div class="method-selector-wrapper">
                <select
                  class="input method-selector"
                  v-model="forgotPasswordMethod"
                  @change="forgotPasswordCodeSent = false; forgotPasswordCodeVerified = false; forgotPasswordForm.identifier = '';"
                >
                  <option value="email">{{ t('login.email') }}</option>
                  <option value="phone">{{ t('login.phone') }}</option>
                </select>
              </div>
              <!-- Input Field (middle) -->
              <input
                :type="forgotPasswordMethod === 'email' ? 'email' : 'tel'"
                class="input input-field"
                :placeholder="forgotPasswordMethod === 'email' ? t('login.enterEmail') : t('login.enterPhone')"
                v-model="forgotPasswordForm.identifier"
                @input="onForgotPasswordIdentifierInput"
                required
              />
              <!-- Send Code Button (right side) -->
              <button 
                type="button" 
                class="btn btn-primary send-code-btn"
                @click="sendPasswordResetCode"
                :disabled="loading || !forgotPasswordForm.identifier || forgotPasswordCodeSent"
              >
                {{ forgotPasswordCodeSent ? (lang === 'zh' ? '已发送' : 'Sent') : (loading ? t('login.pleaseWait') : t('login.sendCode')) }}
              </button>
            </div>
            <small v-if="forgotPasswordCodeSent" class="success-message">
              ✓ {{ t('login.codeSentTo') }} {{ maskedForgotPasswordIdentifier }}
            </small>
          </div>
          
          <!-- Verification Code Section (shown after code sent) -->
          <div v-if="forgotPasswordCodeSent" class="form-group">
            <label>
              {{ t('login.verificationCode') }}
              <span v-if="forgotPasswordCodeVerified" class="verified-badge">
                ✓ {{ lang === 'zh' ? '已验证' : 'Verified' }}
              </span>
            </label>
            <div class="code-input-group">
              <input
                type="text"
                class="input code-input"
                :class="{ 'verified': forgotPasswordCodeVerified }"
                :placeholder="forgotPasswordMethod === 'phone' ? t('login.enterSmsCode') : t('login.verificationCode')"
                v-model="forgotPasswordForm.code"
                :maxlength="forgotPasswordMethod === 'phone' ? undefined : '6'"
                @input="onForgotPasswordCodeInput"
                :disabled="forgotPasswordCodeVerified"
                required
              />
              <button 
                type="button" 
                class="btn verify-btn"
                :class="{ 'btn-primary': !forgotPasswordCodeVerified, 'btn-success': forgotPasswordCodeVerified }"
                @click="verifyPasswordResetCode"
                :disabled="loading || forgotPasswordCodeVerified || !forgotPasswordForm.code || (forgotPasswordMethod === 'email' && forgotPasswordForm.code.length !== 6)"
        >
                {{ forgotPasswordCodeVerified ? (lang === 'zh' ? '已验证' : 'Verified') : (loading ? t('login.pleaseWait') : t('login.verify')) }}
              </button>
            </div>
            <small class="form-hint">
              {{ forgotPasswordMethod === 'email' ? (lang === 'zh' ? '输入6位验证码后点击验证' : 'Enter 6-digit code and click verify') : (lang === 'zh' ? '输入短信验证码后点击验证' : 'Enter SMS code and click verify') }}
            </small>
          </div>
          
          <!-- Password Section -->
          <div class="form-group">
            <label>{{ t('login.newPassword') }}</label>
            <input
              type="password"
              class="input"
              :placeholder="t('login.newPassword')"
              v-model="forgotPasswordForm.newPassword"
              required
            />
            <small style="color: #666; font-size: 0.8rem">{{ t('login.passwordRequirements') }}</small>
          </div>
          
          <div class="form-group">
            <label>{{ t('login.confirmNewPassword') }}</label>
            <input
              type="password"
              class="input"
              :placeholder="t('login.confirmNewPassword')"
              v-model="forgotPasswordForm.confirmPassword"
              required
            />
          </div>
          
          <button 
            type="button" 
            class="btn btn-primary register-btn" 
            @click="resetPassword"
            :disabled="loading || !forgotPasswordCodeSent || (forgotPasswordMethod === 'email' && !forgotPasswordCodeVerified)"
          >
            {{ loading ? t('login.pleaseWait') : t('login.resetPassword') }}
        </button>
          
          <div class="form-footer">
            <span v-if="forgotPasswordCodeSent" class="link" @click="sendPasswordResetCode" style="cursor: pointer">
              {{ t('login.resendCode') }}
            </span>
            <span class="link" @click="backToLogin" style="cursor: pointer; margin-left: auto">
              {{ t('login.backToLogin') }}
            </span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { t, setLanguage, getCurrentLanguage, initLanguage } from '../i18n';
import { 
  sendVerificationCode as apiSendVerificationCode,
  verifyCode as apiVerifyCode,
  registerWithVerification,
  registerWithPhone,
  sendPasswordResetCode as apiSendPasswordResetCode,
  resetPassword as apiResetPassword,
  resetPasswordWithPhone
} from '../api';
import { sendSmsCode } from '../firebase';

const API_BASE = import.meta.env.VITE_API_BASE ? `${import.meta.env.VITE_API_BASE}/api` : '/api';

const emit = defineEmits(['login']);

const lang = ref(getCurrentLanguage());
const isRegister = ref(false);
const isForgotPassword = ref(false);
const loading = ref(false);
const error = ref('');

// Login form
const loginForm = ref({ username: '', password: '' });

// Registration form (all in one page)
const registerStep = ref(1); // 保留用于兼容，实际不再使用步骤
const registerMethod = ref('email'); // 'email' or 'phone'
const codeSent = ref(false); // 验证码是否已发送
const codeVerified = ref(false); // 验证码是否已验证
const registerForm = ref({
  identifier: '',
  code: '',
  username: '',
  password: '',
  confirmPassword: '',
  preferredLanguage: 'en'
});

// Forgot password form (all in one page, same as registration)
const forgotPasswordStep = ref(1); // 保留用于兼容，实际不再使用步骤
const forgotPasswordMethod = ref('email');
const forgotPasswordCodeSent = ref(false); // 验证码是否已发送
const forgotPasswordCodeVerified = ref(false); // 忘记密码验证码是否已验证
const forgotPasswordForm = ref({
  identifier: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
});
const forgotPasswordPhoneConfirmation = ref(null); // Firebase confirmation result for phone reset

// Firebase 短信验证码确认对象（注册用）
const registerPhoneConfirmation = ref(null);

// Computed properties
const maskedIdentifier = computed(() => {
  if (!registerForm.value.identifier) return '';
  const id = registerForm.value.identifier;
  if (registerMethod.value === 'email') {
    const [name, domain] = id.split('@');
    return `${name.substring(0, 2)}***@${domain}`;
  } else {
    return id.substring(0, 3) + '***' + id.substring(id.length - 2);
  }
});

const maskedForgotPasswordIdentifier = computed(() => {
  if (!forgotPasswordForm.value.identifier) return '';
  const id = forgotPasswordForm.value.identifier;
  if (forgotPasswordMethod.value === 'email') {
    const [name, domain] = id.split('@');
    return `${name.substring(0, 2)}***@${domain}`;
  } else {
    return id.substring(0, 3) + '***' + id.substring(id.length - 2);
  }
});

onMounted(() => {
  initLanguage();
  lang.value = getCurrentLanguage();
  
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

const handleLanguageSwitch = (newLang) => {
  setLanguage(newLang);
  lang.value = newLang;
};

const backToLogin = () => {
  isRegister.value = false;
  isForgotPassword.value = false;
  registerStep.value = 1;
  forgotPasswordStep.value = 1;
  codeSent.value = false; // 重置注册发送状态
  codeVerified.value = false; // 重置注册验证状态
  forgotPasswordCodeSent.value = false; // 重置忘记密码发送状态
  forgotPasswordCodeVerified.value = false; // 重置忘记密码验证状态
  error.value = '';
  // Reset forms
  registerForm.value = {
    identifier: '',
    code: '',
    username: '',
    password: '',
    confirmPassword: '',
    preferredLanguage: 'en'
  };
  forgotPasswordForm.value = {
    identifier: '',
    code: '',
    newPassword: '',
    confirmPassword: ''
  };
  forgotPasswordPhoneConfirmation.value = null;
};
  
// Login
const handleLogin = async () => {
  error.value = '';
  
  if (!loginForm.value.username || !loginForm.value.password) {
    error.value = lang.value === 'zh' ? '请填写所有必填项' : 'Please fill in all required fields';
    return;
  }
  
  loading.value = true;
  
  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: loginForm.value.username,
        password: loginForm.value.password
      })
    });
    
    const data = await res.json();
    
    if (res.ok) {
      if (data.user?.preferredLanguage) {
        const userLang = data.user.preferredLanguage;
        if (userLang === 'zh' || userLang === 'en') {
          setLanguage(userLang);
        }
      }
      emit('login', data.user, data.token);
    } else {
      error.value = data.error || data.message || (lang.value === 'zh' ? '登录失败' : 'Login failed');
    }
  } catch (err) {
    console.error('Login error:', err);
    error.value = lang.value === 'zh' 
      ? '网络错误，请确保后端服务正在运行'
      : 'Network error, please ensure backend service is running';
  } finally {
    loading.value = false;
  }
};

// 标识符输入时重置发送状态
const onIdentifierInput = () => {
  if (codeSent.value) {
    codeSent.value = false;
    codeVerified.value = false;
    registerForm.value.code = '';
  }
};

// Registration: Send verification code (点击确认按钮时检查并发送)
const sendVerificationCode = async () => {
  error.value = '';
  
  if (!registerForm.value.identifier) {
    error.value = lang.value === 'zh' ? '请输入邮箱或手机号' : 'Please enter email or phone number';
    return;
  }
  
  // 验证邮箱格式
  if (registerMethod.value === 'email') {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(registerForm.value.identifier.trim())) {
      error.value = lang.value === 'zh' ? '请输入有效的邮箱地址' : 'Please enter a valid email address';
      return;
  }
  }
  
  // 验证手机号格式（需带国家区号）
  if (registerMethod.value === 'phone') {
    const phone = registerForm.value.identifier.trim();
    if (!phone.startsWith('+')) {
      error.value = lang.value === 'zh'
        ? '请输入带国家区号的手机号，例如 +66...'
        : 'Please enter phone with country code, e.g. +66...';
      return;
    }
  }
  
  loading.value = true;
  
  try {
    if (registerMethod.value === 'phone') {
      // 使用 Firebase 发送短信验证码（需带国家区号）
      const phone = registerForm.value.identifier.trim();
      registerPhoneConfirmation.value = await sendSmsCode(phone);
      codeSent.value = true;
      codeVerified.value = false; // 重置验证状态
      error.value = '';
    } else {
      // 邮箱仍然使用后端验证码
      await apiSendVerificationCode(registerForm.value.identifier, registerMethod.value);
      codeSent.value = true;
      codeVerified.value = false; // 重置验证状态
      error.value = '';
    }
  } catch (err) {
    console.error('Send verification code error:', err);
    // 处理超时错误
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      error.value = lang.value === 'zh' 
        ? '请求超时，请检查后端服务是否运行（http://localhost:8080）或网络连接'
        : 'Request timeout. Please check if backend service is running (http://localhost:8080) or network connection';
    } else if (err.response?.data?.error) {
      error.value = err.response.data.error;
    } else if (err.response?.data?.message) {
      error.value = err.response.data.message;
    } else {
      error.value = err.message || 
        (lang.value === 'zh' ? '发送验证码失败，请检查后端服务是否运行' : 'Failed to send verification code. Please check if backend service is running');
    }
  } finally {
    loading.value = false;
  }
};

// 验证码输入时重置验证状态
const onCodeInput = () => {
  if (codeVerified.value) {
    codeVerified.value = false;
  }
};

// Registration - Step 2: Verify code (实时验证)
const verifyCode = async () => {
  error.value = '';
  
  if (!registerForm.value.code) {
    error.value = lang.value === 'zh' ? '请输入验证码' : 'Please enter verification code';
    return;
  }
  
  // 清理验证码：移除空格，只保留数字
  const cleanCode = registerForm.value.code.replace(/\s/g, '').replace(/\D/g, '');
  
  if (cleanCode.length !== 6) {
    error.value = lang.value === 'zh' ? '验证码必须是6位数字' : 'Verification code must be 6 digits';
    return;
  }
  
  if (!registerForm.value.identifier) {
    error.value = lang.value === 'zh' ? '标识符不能为空' : 'Identifier cannot be empty';
    return;
  }
  
  loading.value = true;
  
  try {
    if (registerMethod.value === 'phone') {
      // 使用 Firebase 校验短信验证码
      if (!registerPhoneConfirmation.value) {
        throw new Error('No SMS confirmation session');
      }
      await registerPhoneConfirmation.value.confirm(cleanCode);
      codeVerified.value = true; // 标记为已验证
      error.value = '';
    } else {
      // 邮箱验证码仍然由后端校验
      console.log('Verifying code:', {
        identifier: registerForm.value.identifier,
        code: cleanCode,
        type: registerMethod.value
      });
      await apiVerifyCode(registerForm.value.identifier, cleanCode, registerMethod.value);
      codeVerified.value = true; // 标记为已验证
      error.value = '';
    }
  } catch (err) {
    console.error('Verify code error:', err);
    console.error('Error response:', err.response?.data);
    
    // 处理 400 错误（验证失败或格式错误）
    if (err.response?.status === 400) {
      if (err.response?.data?.error) {
        error.value = err.response.data.error;
      } else if (err.response?.data?.message) {
        error.value = err.response.data.message;
      } else {
        // 检查是否是验证错误（字段验证失败）
        const validationErrors = err.response?.data;
        if (validationErrors && typeof validationErrors === 'object') {
          const errorMessages = Object.values(validationErrors).flat();
          error.value = errorMessages.length > 0 
            ? errorMessages[0] 
            : (lang.value === 'zh' ? '验证码格式错误' : 'Invalid verification code format');
        } else {
          error.value = lang.value === 'zh' ? '验证码错误或已过期' : 'Invalid or expired verification code';
        }
      }
    } else {
      error.value = err.response?.data?.error || err.response?.data?.message || err.message || 
        (lang.value === 'zh' ? '验证码验证失败' : 'Failed to verify code');
    }
  } finally {
    loading.value = false;
  }
};

// Registration: Complete registration (验证码和密码在同一页面)
const completeRegistration = async () => {
  error.value = '';
  
  // 检查验证码是否已发送
  if (!codeSent.value) {
    error.value = lang.value === 'zh' ? '请先发送验证码' : 'Please send verification code first';
    return;
  }
  
  // 检查验证码是否已验证（邮箱注册需要）
  if (registerMethod.value === 'email' && !codeVerified.value) {
    error.value = lang.value === 'zh' ? '请先验证验证码' : 'Please verify the code first';
    return;
  }
  
  // Validate username (English only)
  if (!/^[A-Za-z0-9_]+$/.test(registerForm.value.username)) {
    error.value = t('login.usernameInvalid');
    return;
  }
  
  // Validate password (must contain uppercase and lowercase)
  if (!/(?=.*[a-z])(?=.*[A-Z])/.test(registerForm.value.password)) {
    error.value = t('login.passwordWeak');
    return;
  }
  
  // Check password match
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    error.value = t('login.passwordsNotMatch');
    return;
  }
  
  loading.value = true;

  try {
    let data;
    if (registerMethod.value === 'phone') {
      // 手机注册：Firebase 已验证手机号，这里仅创建账号
      const payload = {
        phone: registerForm.value.identifier.trim(),
        username: registerForm.value.username.trim(),
        password: registerForm.value.password,
        displayName: registerForm.value.username.trim(),
        preferredLanguage: registerForm.value.preferredLanguage || lang.value || 'en'
      };
      data = await registerWithPhone(payload);
    } else {
      // 邮箱注册：仍使用后端验证码注册
      // 清理验证码：移除空格，只保留数字
      const cleanCode = registerForm.value.code.replace(/\s/g, '').replace(/\D/g, '');
      
    const payload = {
        identifier: registerForm.value.identifier.trim(),
        code: cleanCode,
        type: registerMethod.value,
        username: registerForm.value.username.trim(),
        password: registerForm.value.password,
        displayName: registerForm.value.username.trim(),
        preferredLanguage: registerForm.value.preferredLanguage || lang.value || 'en'
    };
    
      console.log('Register payload:', {
        ...payload,
        password: '***' // 不打印密码
      });
      
      data = await registerWithVerification(payload);
    }
    
    if (data.user?.preferredLanguage) {
        const userLang = data.user.preferredLanguage;
        if (userLang === 'zh' || userLang === 'en') {
          setLanguage(userLang);
        }
      }
      emit('login', data.user, data.token);
  } catch (err) {
    console.error('Registration error:', err);
    console.error('Error response:', err.response?.data);
    
    // 处理 400 错误（验证失败或格式错误）
    if (err.response?.status === 400) {
      if (err.response?.data?.error) {
        error.value = err.response.data.error;
      } else if (err.response?.data?.message) {
        error.value = err.response.data.message;
      } else {
        // 检查是否是验证错误（字段验证失败）
        const validationErrors = err.response?.data;
        if (validationErrors && typeof validationErrors === 'object') {
          // 尝试提取第一个错误消息
          const errorMessages = Object.values(validationErrors).flat();
          if (errorMessages.length > 0) {
            error.value = Array.isArray(errorMessages[0]) 
              ? errorMessages[0][0] 
              : errorMessages[0];
          } else {
            error.value = lang.value === 'zh' ? '注册信息格式错误' : 'Invalid registration format';
          }
        } else {
          error.value = lang.value === 'zh' ? '注册失败，请检查输入信息' : 'Registration failed. Please check your input';
        }
      }
    } else {
      error.value = err.response?.data?.error || err.response?.data?.message || err.message || 
        (lang.value === 'zh' ? '注册失败' : 'Registration failed');
    }
  } finally {
    loading.value = false;
  }
};

// Forgot Password - Step 1: Send code
const sendPasswordResetCode = async () => {
  error.value = '';
  
  if (!forgotPasswordForm.value.identifier) {
    error.value = lang.value === 'zh' ? '请输入邮箱或手机号' : 'Please enter email or phone number';
    return;
  }
  
  loading.value = true;
  
  try {
    if (forgotPasswordMethod.value === 'phone') {
      // 手机号找回密码：使用 Firebase 发送短信验证码
      const phone = forgotPasswordForm.value.identifier.trim();
      if (!phone.startsWith('+')) {
        error.value = lang.value === 'zh'
          ? '请输入带国家区号的手机号，如 +66...'
          : 'Please enter phone with country code, e.g. +66...';
        loading.value = false;
        return;
      }
      
      forgotPasswordPhoneConfirmation.value = await sendSmsCode(phone);
      forgotPasswordCodeSent.value = true;
      forgotPasswordCodeVerified.value = false; // 重置验证状态
      error.value = '';
    } else {
      // 邮箱找回密码：使用后端发送邮件验证码
      await apiSendPasswordResetCode(forgotPasswordForm.value.identifier, forgotPasswordMethod.value);
      forgotPasswordCodeSent.value = true;
      forgotPasswordCodeVerified.value = false; // 重置验证状态
      error.value = '';
    }
  } catch (err) {
    console.error('Send password reset code error:', err);
    error.value = err.response?.data?.message || err.message || 
      (lang.value === 'zh' ? '发送验证码失败' : 'Failed to send verification code');
  } finally {
    loading.value = false;
  }
};

// 忘记密码标识符输入时重置发送状态
const onForgotPasswordIdentifierInput = () => {
  if (forgotPasswordCodeSent.value) {
    forgotPasswordCodeSent.value = false;
    forgotPasswordCodeVerified.value = false;
    forgotPasswordForm.value.code = '';
  }
};

// 忘记密码验证码输入时重置验证状态
const onForgotPasswordCodeInput = () => {
  if (forgotPasswordCodeVerified.value) {
    forgotPasswordCodeVerified.value = false;
  }
};
  
// Forgot Password - Step 2: Verify code (实时验证)
const verifyPasswordResetCode = async () => {
  error.value = '';
  
  if (!forgotPasswordForm.value.code) {
    error.value = lang.value === 'zh' ? '请输入验证码' : 'Please enter verification code';
    return;
  }
  
  // 清理验证码：移除空格，只保留数字
  const cleanCode = forgotPasswordForm.value.code.replace(/\s/g, '').replace(/\D/g, '');
  
  if (forgotPasswordMethod.value === 'email' && cleanCode.length !== 6) {
    error.value = lang.value === 'zh' ? '验证码必须是6位数字' : 'Verification code must be 6 digits';
    return;
  }
  
  if (!forgotPasswordForm.value.identifier) {
    error.value = lang.value === 'zh' ? '标识符不能为空' : 'Identifier cannot be empty';
    return;
  }
  
  loading.value = true;
  
  try {
    if (forgotPasswordMethod.value === 'phone') {
      // 手机号找回密码：使用 Firebase 验证短信验证码
      if (!forgotPasswordPhoneConfirmation.value) {
        throw new Error('No confirmation result');
      }
      await forgotPasswordPhoneConfirmation.value.confirm(cleanCode);
      forgotPasswordCodeVerified.value = true; // 标记为已验证
      error.value = '';
    } else {
      // 邮箱找回密码：使用后端验证邮件验证码
      console.log('Verifying password reset code:', {
        identifier: forgotPasswordForm.value.identifier,
        code: cleanCode,
        type: forgotPasswordMethod.value,
        purpose: 'RESET_PASSWORD'
      });
      await apiVerifyCode(forgotPasswordForm.value.identifier, cleanCode, forgotPasswordMethod.value, 'RESET_PASSWORD');
      forgotPasswordCodeVerified.value = true; // 标记为已验证
      error.value = '';
    }
  } catch (err) {
    console.error('Verify password reset code error:', err);
    console.error('Error response:', err.response?.data);
    
    // 处理 400 错误（验证失败或格式错误）
    if (err.response?.status === 400) {
      if (err.response?.data?.error) {
        error.value = err.response.data.error;
      } else if (err.response?.data?.message) {
        error.value = err.response.data.message;
      } else {
        // 检查是否是验证错误（字段验证失败）
        const validationErrors = err.response?.data;
        if (validationErrors && typeof validationErrors === 'object') {
          const errorMessages = Object.values(validationErrors).flat();
          error.value = errorMessages.length > 0 
            ? errorMessages[0] 
            : (lang.value === 'zh' ? '验证码格式错误' : 'Invalid verification code format');
        } else {
          error.value = lang.value === 'zh' ? '验证码错误或已过期' : 'Invalid or expired verification code';
        }
      }
    } else {
      error.value = err.response?.data?.error || err.response?.data?.message || err.message || 
        (lang.value === 'zh' ? '验证码验证失败' : 'Failed to verify code');
    }
  } finally {
    loading.value = false;
  }
};

// Forgot Password: Reset password (验证码和密码在同一页面)
const resetPassword = async () => {
  error.value = '';
  
  // 检查验证码是否已发送
  if (!forgotPasswordCodeSent.value) {
    error.value = lang.value === 'zh' ? '请先发送验证码' : 'Please send verification code first';
    return;
  }
  
  // 检查验证码是否已验证（邮箱重置密码需要）
  if (forgotPasswordMethod.value === 'email' && !forgotPasswordCodeVerified.value) {
    error.value = lang.value === 'zh' ? '请先验证验证码' : 'Please verify the code first';
    return;
  }
  
  // Validate password (must contain uppercase and lowercase)
  if (!/(?=.*[a-z])(?=.*[A-Z])/.test(forgotPasswordForm.value.newPassword)) {
    error.value = t('login.passwordWeak');
    return;
  }
  
  // Check password match
  if (forgotPasswordForm.value.newPassword !== forgotPasswordForm.value.confirmPassword) {
    error.value = t('login.passwordsNotMatch');
    return;
      }
  
  loading.value = true;
  
  try {
    if (forgotPasswordMethod.value === 'phone') {
      // 手机号找回密码：Firebase 已验证手机号，直接重置密码
      await resetPasswordWithPhone(
        forgotPasswordForm.value.identifier.trim(),
        forgotPasswordForm.value.newPassword
      );
    } else {
      // 邮箱找回密码：使用后端验证码重置密码
      // 清理验证码：移除空格，只保留数字
      const cleanCode = forgotPasswordForm.value.code.replace(/\s/g, '').replace(/\D/g, '');
      await apiResetPassword(
        forgotPasswordForm.value.identifier.trim(),
        cleanCode,
        forgotPasswordForm.value.newPassword,
        forgotPasswordMethod.value
      );
    }
    
    // Success - go back to login
    error.value = '';
    alert(t('login.passwordResetSuccess'));
    backToLogin();
  } catch (err) {
    console.error('Reset password error:', err);
    error.value = err.response?.data?.message || err.message || 
      (lang.value === 'zh' ? '重置密码失败' : 'Failed to reset password');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: linear-gradient(135deg, 
    rgba(157, 186, 213, 0.4) 0%, 
    rgba(200, 180, 220, 0.5) 50%,
    rgba(157, 186, 213, 0.4) 100%
  );
}

.lang-switcher {
  position: absolute;
  top: 1rem;
  right: 1rem;
  display: flex;
  gap: 0.5rem;
}

.lang-btn {
  padding: 0.5rem 1rem;
  border: 2px solid white;
  background: transparent;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
}

.lang-btn.active {
  background: white;
  color: #667eea;
}

.login-box {
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 480px;
  padding: 0;
  margin: 0 auto;
}

.login-header {
  text-align: center;
  margin-bottom: 0;
  padding: 2rem 2rem;
  background: linear-gradient(135deg, rgba(255,159,137,0.18), rgba(157,186,213,0.14));
}

.logo {
  font-size: 3rem;
  margin-bottom: 0.5rem;
}

.login-header h1 {
  margin: 0 0 0.5rem 0;
  color: #333;
}

.login-header p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  padding: 3.5rem 5rem;
}

.form-group {
  margin-bottom: 2rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
}

.input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.3s;
  box-sizing: border-box;
}

.input:focus {
  outline: none;
  border-color: #667eea;
}

.error-message {
  padding: 0.75rem;
  background: #fee;
  border: 2px solid #fcc;
  border-radius: 6px;
  color: #c33;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 1rem;
  font-size: 0.9rem;
}

.link {
  color: #667eea;
  cursor: pointer;
  text-decoration: underline;
}

.link:hover {
  color: #764ba2;
}

.method-selector {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.method-btn {
  flex: 1;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
}

.method-btn.active {
  background: #667eea;
  color: white;
  border-color: #667eea;
}

.method-btn:hover {
  border-color: #667eea;
}
</style>
