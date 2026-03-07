/**
 * Language preference utility
 * Manages user language preference using localStorage
 */

const LANGUAGE_KEY = 'userLanguage';
const DEFAULT_LANGUAGE = 'en';
const SUPPORTED_LANGUAGES = ['en', 'zh'];

/**
 * Get user's language preference
 * @returns {string} Language code (en/zh)
 */
export const getLanguagePreference = () => {
  // Check if we're in a browser environment
  if (typeof window === 'undefined' || !window.localStorage) {
    return DEFAULT_LANGUAGE;
  }
  try {
    const lang = localStorage.getItem(LANGUAGE_KEY);
    return lang && SUPPORTED_LANGUAGES.includes(lang) ? lang : DEFAULT_LANGUAGE;
  } catch (e) {
    console.warn('Failed to read language preference from localStorage:', e);
    return DEFAULT_LANGUAGE;
  }
};

/**
 * Set user's language preference
 * @param {string} lang Language code (en/zh)
 */
export const setLanguagePreference = (lang) => {
  if (!SUPPORTED_LANGUAGES.includes(lang)) {
    console.warn(`Unsupported language: ${lang}`);
    return;
  }
  
  // Check if we're in a browser environment
  if (typeof window === 'undefined' || !window.localStorage) {
    console.warn('localStorage is not available');
    return;
  }
  
  try {
    localStorage.setItem(LANGUAGE_KEY, lang);
    // Trigger language change event
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('languageChanged', { detail: { lang } }));
    }
  } catch (e) {
    console.error('Failed to save language preference to localStorage:', e);
  }
};

/**
 * Get language name
 * @param {string} lang Language code
 * @returns {string} Language name
 */
export const getLanguageName = (lang) => {
  const names = {
    en: 'English',
    zh: '中文',
    th: 'ไทย',
  };
  return names[lang] || lang;
};

