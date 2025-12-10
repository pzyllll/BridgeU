package com.globalbuddy.controller;

import com.globalbuddy.dto.AuthRequest;
import com.globalbuddy.dto.AuthResponse;
import com.globalbuddy.dto.RegisterRequest;
import com.globalbuddy.dto.MerchantRegisterRequest;
import com.globalbuddy.dto.UserDTO;
import com.globalbuddy.model.AppUser;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 处理用户注册、登录等认证相关操作
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "用户名已被使用",
                "field", "username"
            ));
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "邮箱已被注册",
                "field", "email"
            ));
        }

        // 创建新用户
        AppUser user;
        if (request.getPreferredLanguage() != null && 
            (request.getPreferredLanguage().equals("zh") || request.getPreferredLanguage().equals("en"))) {
            user = AppUser.create(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getDisplayName() != null ? request.getDisplayName() : request.getUsername(),
                request.getPreferredLanguage()
            );
        } else {
            user = AppUser.create(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getDisplayName() != null ? request.getDisplayName() : request.getUsername()
            );
        }

        // 保存用户
        userRepository.save(user);

        // 生成 JWT Token
        String token = jwtService.generateToken(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
            .token(token)
            .expiresIn(jwtService.getExpirationTime())
            .user(UserDTO.fromEntity(user))
            .build());
    }

    /**
     * 商家注册（仅需身份证号，不再上传证件照）
     * POST /api/auth/register/merchant
     */
    @PostMapping("/register/merchant")
    public ResponseEntity<?> registerMerchant(@Valid @RequestBody MerchantRegisterRequest request) {
        // 基本校验
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "用户名已被使用",
                "field", "username"
            ));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "邮箱已被注册",
                "field", "email"
            ));
        }

        try {
            // 创建商家用户
            AppUser user = AppUser.createMerchant(
                request.getUsername(),
                request.getEmail(),
                request.getPhone(),
                passwordEncoder.encode(request.getPassword()),
                request.getUsername(),
                request.getBusinessName(),
                request.getIdNumber(),
                request.getPreferredLanguage()
            );
            user.setMerchantDocType(null);
            user.setMerchantDocUrl(null);

            userRepository.save(user);

            // 生成 JWT Token
            String token = jwtService.generateToken(user);

            return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpirationTime())
                .user(UserDTO.fromEntity(user))
                .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "注册失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            // 验证用户凭据
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );

            // 获取用户信息
            AppUser user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new BadCredentialsException("用户不存在"));

            // 生成 JWT Token
            String token = jwtService.generateToken(user);

            return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .expiresIn(jwtService.getExpirationTime())
                .user(UserDTO.fromEntity(user))
                .build());

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "用户名或密码错误"
            ));
        }
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "未登录"
            ));
        }

        AppUser user = (AppUser) authentication.getPrincipal();
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * 验证 Token 是否有效
     * POST /api/auth/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "valid", false,
                "error", "无效的 Token 格式"
            ));
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            
            AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (jwtService.isTokenValid(token, user)) {
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "user", UserDTO.fromEntity(user)
                ));
            }
        } catch (Exception e) {
            // Token 无效
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
            "valid", false,
            "error", "Token 已过期或无效"
        ));
    }
}
