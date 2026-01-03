import { initializeApp } from 'firebase/app';
import { getAuth, RecaptchaVerifier, signInWithPhoneNumber } from 'firebase/auth';

// Firebase 配置（已由你提供）
const firebaseConfig = {
  apiKey: '',
  authDomain: 'bridgeu-87fbe.firebaseapp.com',
  projectId: 'bridgeu-87fbe',
  storageBucket: 'bridgeu-87fbe.firebasestorage.app',
  messagingSenderId: '942751213810',
  appId: '1:942751213810:web:aa1f42ad58465fd80cc86b',
  measurementId: 'G-Z0S4F6MH7N',
};

// 初始化 Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

// 获取或创建全局 reCAPTCHA（使用 invisible 模式）
export async function getRecaptcha(containerId = 'recaptcha-container') {
  if (typeof window === 'undefined') return null;
  
  // 等待 DOM 完全加载
  if (document.readyState === 'loading') {
    await new Promise(resolve => {
      if (document.readyState === 'complete') {
        resolve();
      } else {
        document.addEventListener('DOMContentLoaded', resolve);
      }
    });
  }
  
  // 如果已存在 reCAPTCHA 实例，先清理
  if (window._bridgeuRecaptcha) {
    try {
      // 检查容器是否仍然存在
      const existingContainer = document.getElementById(containerId);
      if (!existingContainer) {
        // 容器被移除，清理实例
        console.warn('reCAPTCHA container was removed, clearing instance');
        try {
          window._bridgeuRecaptcha.clear();
        } catch (clearError) {
          console.warn('Failed to clear existing reCAPTCHA:', clearError);
        }
        window._bridgeuRecaptcha = null;
      } else {
        // 容器存在，检查实例是否仍然有效
        return window._bridgeuRecaptcha;
      }
    } catch (e) {
      // 如果检查失败，清除并重新创建
      console.warn('reCAPTCHA instance check failed, recreating:', e);
      try {
        window._bridgeuRecaptcha.clear();
      } catch (clearError) {
        console.warn('Failed to clear existing reCAPTCHA:', clearError);
      }
      window._bridgeuRecaptcha = null;
    }
  }
  
  // 确保容器存在且稳定
  let container = document.getElementById(containerId);
  if (!container) {
    container = document.createElement('div');
    container.id = containerId;
    container.style.position = 'fixed';
    container.style.top = '0';
    container.style.left = '0';
    container.style.width = '1px';
    container.style.height = '1px';
    container.style.opacity = '0';
    container.style.pointerEvents = 'none';
    container.style.zIndex = '-1';
    document.body.appendChild(container);
    // 等待容器完全添加到 DOM
    await new Promise(resolve => setTimeout(resolve, 200));
  } else {
    // 容器已存在，确保它仍然在 DOM 中
    if (!document.body.contains(container)) {
      // 容器被移除了，重新添加到 body
      document.body.appendChild(container);
      await new Promise(resolve => setTimeout(resolve, 200));
    }
  }
  
  // 确保容器可见（虽然透明度为0，但必须在 DOM 中）
  container.style.display = 'block';
  container.style.visibility = 'visible';
  
  // 创建新的 reCAPTCHA verifier（使用 invisible 模式）
  try {
    window._bridgeuRecaptcha = new RecaptchaVerifier(auth, container, {
      size: 'invisible',
      callback: () => {
        console.log('reCAPTCHA verified successfully');
      },
      'expired-callback': () => {
        console.warn('reCAPTCHA expired, will recreate on next use');
        window._bridgeuRecaptcha = null;
      },
      'error-callback': (error) => {
        console.error('reCAPTCHA error callback:', error);
        window._bridgeuRecaptcha = null;
      }
    });
    
    // RecaptchaVerifier 在创建时会自动渲染，不需要手动调用 render()
    console.log('reCAPTCHA initialized successfully (invisible mode)');
  } catch (error) {
    console.error('Failed to create reCAPTCHA verifier (invisible mode):', error);
    
    // 如果 invisible 模式失败，尝试 normal 模式作为回退
    try {
      console.log('Trying normal reCAPTCHA mode as fallback...');
      window._bridgeuRecaptcha = new RecaptchaVerifier(auth, container, {
        size: 'normal',
        callback: () => {
          console.log('reCAPTCHA (normal mode) verified');
        },
        'error-callback': (error) => {
          console.error('reCAPTCHA (normal mode) error:', error);
          window._bridgeuRecaptcha = null;
        }
      });
      console.log('reCAPTCHA initialized successfully (normal mode)');
    } catch (fallbackError) {
      console.error('Fallback reCAPTCHA also failed:', fallbackError);
      window._bridgeuRecaptcha = null;
      throw new Error('Failed to initialize reCAPTCHA. Please check:\n1. Phone Authentication is enabled in Firebase Console\n2. Your domain (localhost:5175) is authorized in Firebase Console > Authentication > Settings > Authorized domains\n3. Network connection is stable\n4. reCAPTCHA site key is configured correctly');
    }
  }
  
  return window._bridgeuRecaptcha;
}

// 发送短信验证码
export async function sendSmsCode(phoneNumber) {
  try {
    // 确保 reCAPTCHA 已初始化（异步）
    const verifier = await getRecaptcha();
    if (!verifier) {
      throw new Error('reCAPTCHA not initialized. Please check Firebase configuration.');
    }
    
    // 发送验证码（Firebase 会自动处理 reCAPTCHA）
    const confirmationResult = await signInWithPhoneNumber(auth, phoneNumber, verifier);
    console.log('SMS code sent successfully');
    return confirmationResult;
  } catch (error) {
    console.error('Firebase sendSmsCode error:', error);
    
    // 提供更友好的错误信息
    if (error.code === 'auth/internal-error' || error.message?.includes('internal-error')) {
      throw new Error('Firebase configuration error. Please ensure:\n1. Phone Authentication is enabled in Firebase Console\n2. Your domain (localhost:5175) is authorized in Firebase Console > Authentication > Settings > Authorized domains\n3. reCAPTCHA site key is configured correctly\n4. Network connection is stable');
    } else if (error.code === 'auth/invalid-phone-number') {
      throw new Error('Invalid phone number format. Please use international format with country code (e.g., +66xxxxxxxxx)');
    } else if (error.message?.includes('ERR_CERT') || error.message?.includes('ERR_CONNECTION')) {
      throw new Error('Network error. Please check:\n1. Your internet connection\n2. Firebase services are accessible\n3. No firewall blocking reCAPTCHA requests');
    } else if (error.message?.includes('Failed to initialize reCAPTCHA')) {
      throw error; // 传递原始错误信息
    }
    
    throw error;
  }
}

export { auth };


