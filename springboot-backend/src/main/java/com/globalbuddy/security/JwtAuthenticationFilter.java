package com.globalbuddy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 从请求头中提取 JWT Token 并进行验证
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 从请求头中获取 Authorization
        final String authHeader = request.getHeader("Authorization");
        
        // 如果没有 Token 或不是 Bearer Token，直接放行
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 提取 Token（去掉 "Bearer " 前缀）
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            // 如果用户名不为空且当前没有认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 加载用户信息
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 验证 Token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 设置到 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token 无效或过期，不设置认证信息，让请求继续
            logger.debug("JWT Token validation failed: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
