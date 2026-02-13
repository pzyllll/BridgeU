package com.globalbuddy.controller;

import com.globalbuddy.dto.*;
import com.globalbuddy.model.AppUser;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.security.JwtService;
import com.globalbuddy.service.VerificationCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationCodeService verificationCodeService;

    /**
     * 发送验证码（注册或重置密码）
     * POST /api/auth/send-verification-code
     */
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        try {
            log.info("收到发送验证码请求: type={}, identifier={}", request.getType(), request.getIdentifier());
            
            // 验证邮箱格式
            if ("email".equalsIgnoreCase(request.getType())) {
                if (!request.getIdentifier().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    log.warn("邮箱格式不正确: {}", request.getIdentifier());
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "邮箱格式不正确",
                        "field", "identifier"
                    ));
                }
            }
            
            // 检查是否已注册（注册时）
            if ("email".equalsIgnoreCase(request.getType())) {
                if (userRepository.existsByEmail(request.getIdentifier())) {
                    log.warn("邮箱已被注册: {}", request.getIdentifier());
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "该邮箱已被注册",
                        "field", "identifier"
                    ));
                }
            } else if ("phone".equalsIgnoreCase(request.getType())) {
                if (userRepository.existsByPhone(request.getIdentifier())) {
                    log.warn("手机号已被注册: {}", request.getIdentifier());
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "该手机号已被注册",
                        "field", "identifier"
                    ));
                }
            }

            // 发送验证码（默认用途为注册）
            log.debug("开始调用验证码服务发送验证码");
            boolean sent = verificationCodeService.sendVerificationCode(
                request.getIdentifier(),
                request.getType(),
                "REGISTER"
            );

            if (sent) {
                log.info("验证码发送成功: {}", request.getIdentifier());
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "验证码已发送"
                ));
            } else {
                log.error("验证码发送失败: {}", request.getIdentifier());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "发送验证码失败，请稍后重试"
                ));
            }
        } catch (RuntimeException e) {
            log.error("发送验证码时发生运行时异常: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "发送验证码失败",
                "details", e.getClass().getSimpleName()
            ));
        } catch (Exception e) {
            log.error("发送验证码时发生未知异常: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "发送验证码失败: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()),
                "details", e.getClass().getName()
            ));
        }
    }

    /**
     * 验证验证码
     * POST /api/auth/verify-code
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        try {
            String purpose = request.getPurpose() != null ? request.getPurpose() : "REGISTER";
            VerificationCodeService.VerificationResult result = verificationCodeService.verifyCodeWithDetails(
                request.getIdentifier(),
                request.getCode(),
                request.getType(),
                purpose
            );

            if (result.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "验证码验证成功"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", result.getErrorMessage() != null ? result.getErrorMessage() : "验证码错误或已过期"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "验证失败"
            ));
        }
    }

    /**
     * 用户注册（带验证码）
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterWithVerificationRequest request) {
        // 验证验证码（返回详细错误信息）
        VerificationCodeService.VerificationResult codeResult = verificationCodeService.verifyCodeWithDetails(
            request.getIdentifier(),
            request.getCode(),
            request.getType(),
            "REGISTER"
        );

        if (!codeResult.isSuccess()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", codeResult.getErrorMessage() != null ? codeResult.getErrorMessage() : "验证码错误或已过期",
                "field", "code"
            ));
        }
        
        // 验证码验证成功后，标记为已使用（防止重复使用）
        try {
            verificationCodeService.markCodeAsUsed(request.getIdentifier(), request.getType(), "REGISTER");
        } catch (Exception e) {
            log.warn("标记验证码为已使用时出错: {}", e.getMessage());
            // 不影响注册流程，继续执行
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "用户名已被使用",
                "field", "username"
            ));
        }

        // 检查邮箱或手机号是否已存在
        if ("email".equalsIgnoreCase(request.getType())) {
            if (userRepository.existsByEmail(request.getIdentifier())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "邮箱已被注册",
                    "field", "identifier"
                ));
            }
        } else if ("phone".equalsIgnoreCase(request.getType())) {
            if (userRepository.existsByPhone(request.getIdentifier())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "手机号已被注册",
                    "field", "identifier"
                ));
            }
        }

        // 创建新用户
        AppUser user;
        String email = "email".equalsIgnoreCase(request.getType()) ? request.getIdentifier() : null;
        String phone = "phone".equalsIgnoreCase(request.getType()) ? request.getIdentifier() : null;
        
        // 如果使用手机号注册，需要生成一个临时邮箱（或使用手机号作为邮箱）
        if (email == null) {
            email = phone + "@bridgeu.local"; // 临时邮箱
        }

        String displayName = request.getDisplayName() != null && !request.getDisplayName().isEmpty()
            ? request.getDisplayName()
            : request.getUsername();

        String preferredLanguage = request.getPreferredLanguage();
        if (preferredLanguage == null || 
            (!preferredLanguage.equals("zh") && !preferredLanguage.equals("en"))) {
            preferredLanguage = "en";
        }

        user = AppUser.create(
            request.getUsername(),
            email,
            passwordEncoder.encode(request.getPassword()),
            displayName,
            preferredLanguage
        );

        // 如果使用手机号注册，设置手机号
        if (phone != null) {
            user.setPhone(phone);
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
     * 手机注册（手机号已通过 Firebase 验证）
     * 不再依赖后端短信验证码
     * POST /api/auth/register/phone
     */
    @PostMapping("/register/phone")
    public ResponseEntity<?> registerPhone(@Valid @RequestBody PhoneRegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "用户名已被使用",
                "field", "username"
            ));
        }

        // 检查手机号是否已存在
        if (userRepository.existsByPhone(request.getPhone())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "手机号已被注册",
                "field", "phone"
            ));
        }

        // 使用手机号生成一个临时邮箱
        String phone = request.getPhone();
        String email = phone + "@bridgeu.local";

        String displayName = request.getDisplayName() != null && !request.getDisplayName().isEmpty()
            ? request.getDisplayName()
            : request.getUsername();

        String preferredLanguage = request.getPreferredLanguage();
        if (preferredLanguage == null ||
            (!preferredLanguage.equals("zh") && !preferredLanguage.equals("en"))) {
            preferredLanguage = "en";
        }

        AppUser user = AppUser.create(
            request.getUsername(),
            email,
            passwordEncoder.encode(request.getPassword()),
            displayName,
            preferredLanguage
        );
        user.setPhone(phone);

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
            .token(token)
            .expiresIn(jwtService.getExpirationTime())
            .user(UserDTO.fromEntity(user))
            .build());
    }

    /**
     * 旧版注册接口（保留兼容性）
     * POST /api/auth/register/old
     */
    @PostMapping("/register/old")
    public ResponseEntity<?> registerOld(@Valid @RequestBody RegisterRequest request) {
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

    /**
     * 发送密码重置验证码
     * POST /api/auth/forgot-password/send-code
     */
    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<?> sendPasswordResetCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        try {
            // 检查用户是否存在
            boolean userExists = false;
            if ("email".equalsIgnoreCase(request.getType())) {
                userExists = userRepository.existsByEmail(request.getIdentifier());
            } else if ("phone".equalsIgnoreCase(request.getType())) {
                userExists = userRepository.existsByPhone(request.getIdentifier());
            }

            if (!userExists) {
                // 为了安全，不透露用户是否存在
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "如果该邮箱/手机号已注册，验证码已发送"
                ));
            }

            // 发送验证码（用途为重置密码）
            boolean sent = verificationCodeService.sendVerificationCode(
                request.getIdentifier(),
                request.getType(),
                "RESET_PASSWORD"
            );

            if (sent) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "验证码已发送"
                ));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "发送验证码失败，请稍后重试"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "发送验证码失败"
            ));
        }
    }

    /**
     * 重置密码（邮箱验证码方式）
     * POST /api/auth/forgot-password/reset
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            // 验证验证码（返回详细错误信息）
            VerificationCodeService.VerificationResult codeResult = verificationCodeService.verifyCodeWithDetails(
                request.getIdentifier(),
                request.getCode(),
                request.getType(),
                "RESET_PASSWORD"
            );

            if (!codeResult.isSuccess()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", codeResult.getErrorMessage() != null ? codeResult.getErrorMessage() : "验证码错误或已过期",
                    "field", "code"
                ));
            }

            // 查找用户
            AppUser user = null;
            if ("email".equalsIgnoreCase(request.getType())) {
                user = userRepository.findByEmail(request.getIdentifier()).orElse(null);
            } else if ("phone".equalsIgnoreCase(request.getType())) {
                user = userRepository.findByPhone(request.getIdentifier()).orElse(null);
            }

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "用户不存在"
                ));
            }

            // 更新密码
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "密码重置成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "重置密码失败"
            ));
        }
    }

    /**
     * 重置密码（手机号方式，Firebase已验证）
     * POST /api/auth/forgot-password/reset/phone
     */
    @PostMapping("/forgot-password/reset/phone")
    public ResponseEntity<?> resetPasswordWithPhone(@Valid @RequestBody PhoneResetPasswordRequest request) {
        try {
            // 查找用户
            AppUser user = userRepository.findByPhone(request.getPhone()).orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "该手机号未注册"
                ));
            }

            // 更新密码（Firebase 已验证手机号，无需再验证验证码）
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "密码重置成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage() != null ? e.getMessage() : "重置密码失败"
            ));
        }
    }
}
