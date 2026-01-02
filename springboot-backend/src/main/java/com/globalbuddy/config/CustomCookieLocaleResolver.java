package com.globalbuddy.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.util.Locale;

/**
 * 自定义的 CookieLocaleResolver：
 * - 优先使用用户上次选择并保存在 Cookie 里的语言
 * - 如果没有 Cookie（第一次访问），根据浏览器 Accept-Language 自动推断默认语言
 *   - 以 zh 开头：简体中文
 *   - 其他情况：英文
 */
public class CustomCookieLocaleResolver extends CookieLocaleResolver {

    @Override
    protected Locale determineDefaultLocale(HttpServletRequest request) {
        // 优先使用父类的默认逻辑（可能已经有全局 defaultLocale）
        Locale defaultLocale = super.determineDefaultLocale(request);
        if (defaultLocale != null) {
            return defaultLocale;
        }

        // 没有在配置里显式指定 defaultLocale 时，根据浏览器语言自动判断
        String languageHeader = request.getHeader("Accept-Language");
        if (languageHeader != null && !languageHeader.isEmpty()) {
            // 直接使用 request.getLocale() 解析 Accept-Language 的首选语言
            Locale requestLocale = request.getLocale();
            if (requestLocale != null) {
                String lang = requestLocale.getLanguage();
                if (lang != null) {
                    if (lang.startsWith("zh")) {
                        // 浏览器语言是中文，默认使用简体中文
                        return Locale.SIMPLIFIED_CHINESE;
                    } else {
                        // 其他情况默认英文
                        return Locale.ENGLISH;
                    }
                }
            }
        }

        // 没有头信息时兜底为英文
        return Locale.ENGLISH;
    }
}


