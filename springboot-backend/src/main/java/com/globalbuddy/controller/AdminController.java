package com.globalbuddy.controller;

import com.globalbuddy.dto.UserDTO;
import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.CommentRepository;
import com.globalbuddy.repository.PostLikeRepository;
import com.globalbuddy.service.LanguageDetectionService;
import com.globalbuddy.service.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员控制器
 * 提供管理员后台功能接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AppUserRepository userRepository;
    private final CommunityPostRepository postRepository;
    private final TranslationService translationService;
    private final LanguageDetectionService languageDetectionService;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;

    // ============ 用户管理 ============

    /**
     * 获取所有用户列表
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AppUser> users = userRepository.findAll(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Map<String, Object> response = new HashMap<>();
        response.put("data", users.getContent().stream()
            .map(UserDTO::fromEntity)
            .collect(Collectors.toList()));
        response.put("pagination", Map.of(
            "page", users.getNumber(),
            "size", users.getSize(),
            "totalPages", users.getTotalPages(),
            "totalElements", users.getTotalElements()
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户详情
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        return userRepository.findById(userId)
            .map(user -> ResponseEntity.ok(UserDTO.fromEntity(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新用户角色
     * PATCH /api/admin/users/{userId}/role
     */
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> request
    ) {
        String newRole = request.get("role");
        if (newRole == null || (!newRole.equals("USER") && !newRole.equals("ADMIN"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "无效的角色"));
        }

        return userRepository.findById(userId)
            .map(user -> {
                user.setRole(AppUser.Role.valueOf(newRole));
                userRepository.save(user);
                return ResponseEntity.ok(Map.of(
                    "message", "用户角色已更新",
                    "user", UserDTO.fromEntity(user)
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 禁用/启用用户
     * PATCH /api/admin/users/{userId}/status
     */
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Boolean> request
    ) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供 enabled 参数"));
        }

        return userRepository.findById(userId)
            .map(user -> {
                user.setEnabled(enabled);
                userRepository.save(user);
                return ResponseEntity.ok(Map.of(
                    "message", enabled ? "用户已启用" : "用户已禁用",
                    "user", UserDTO.fromEntity(user)
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ============ 帖子审核 ============

    /**
     * 获取待审核帖子列表
     * GET /api/admin/posts/pending
     */
    @GetMapping("/posts/pending")
    public ResponseEntity<?> getPendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommunityPost> posts = postRepository.findByStatus(
            CommunityPost.Status.PENDING_REVIEW,
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))
        );

        return buildPostListResponse(posts);
    }

    /**
     * 获取所有帖子（按状态筛选）
     * GET /api/admin/posts
     */
    @GetMapping("/posts")
    public ResponseEntity<?> getAllPosts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<CommunityPost> posts;
        
        if (status != null && !status.isEmpty()) {
            try {
                CommunityPost.Status postStatus = CommunityPost.Status.valueOf(status.toUpperCase());
                posts = postRepository.findByStatus(
                    postStatus,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                );
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的状态值"));
            }
        } else {
            posts = postRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
        }

        return buildPostListResponse(posts);
    }

    /**
     * 审核通过帖子
     * POST /api/admin/posts/{postId}/approve
     */
    @PostMapping("/posts/{postId}/approve")
    public ResponseEntity<?> approvePost(
            @PathVariable String postId,
            @RequestBody(required = false) Map<String, String> request,
            @AuthenticationPrincipal AppUser admin
    ) {
        String note = request != null ? request.get("note") : null;

        return postRepository.findById(postId)
            .map(post -> {
                post.approve(admin.getId(), note);
                postRepository.save(post);
                return ResponseEntity.ok(Map.of(
                    "message", "帖子已通过审核",
                    "post", buildPostDTO(post)
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 拒绝帖子
     * POST /api/admin/posts/{postId}/reject
     */
    @PostMapping("/posts/{postId}/reject")
    public ResponseEntity<?> rejectPost(
            @PathVariable String postId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal AppUser admin
    ) {
        String note = request.get("note");
        if (note == null || note.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供拒绝原因"));
        }

        return postRepository.findById(postId)
            .map(post -> {
                post.reject(admin.getId(), note);
                postRepository.save(post);
                return ResponseEntity.ok(Map.of(
                    "message", "帖子已被拒绝",
                    "post", buildPostDTO(post)
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // ============ 统计数据 ============

    /**
     * 获取仪表盘统计数据
     * GET /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        long totalUsers = userRepository.count();
        long adminCount = userRepository.findByRole(AppUser.Role.ADMIN).size();
        long totalPosts = postRepository.count();
        long pendingPosts = postRepository.countByStatus(CommunityPost.Status.PENDING_REVIEW);
        long approvedPosts = postRepository.countByStatus(CommunityPost.Status.APPROVED);
        long rejectedPosts = postRepository.countByStatus(CommunityPost.Status.REJECTED);

        return ResponseEntity.ok(Map.of(
            "users", Map.of(
                "total", totalUsers,
                "admins", adminCount,
                "regularUsers", totalUsers - adminCount
            ),
            "posts", Map.of(
                "total", totalPosts,
                "pending", pendingPosts,
                "approved", approvedPosts,
                "rejected", rejectedPosts
            )
        ));
    }

    // ============ 辅助方法 ============

    private ResponseEntity<?> buildPostListResponse(Page<CommunityPost> posts) {
        List<Map<String, Object>> postDTOs = posts.getContent().stream()
            .map(this::buildPostDTO)
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("data", postDTOs);
        response.put("pagination", Map.of(
            "page", posts.getNumber(),
            "size", posts.getSize(),
            "totalPages", posts.getTotalPages(),
            "totalElements", posts.getTotalElements()
        ));

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> buildPostDTO(CommunityPost post) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", post.getId());
        dto.put("title", post.getTitle());
        dto.put("body", post.getBody());
        dto.put("tags", post.getTags());
        dto.put("status", post.getStatus().name());
        dto.put("aiResult", post.getAiResult());
        dto.put("aiConfidence", post.getAiConfidence());
        dto.put("reviewNote", post.getReviewNote());
        dto.put("reviewedBy", post.getReviewedBy());
        dto.put("reviewedAt", post.getReviewedAt());
        dto.put("createdAt", post.getCreatedAt());
        dto.put("updatedAt", post.getUpdatedAt());
        
        if (post.getAuthor() != null) {
            dto.put("author", Map.of(
                "id", post.getAuthor().getId(),
                "username", post.getAuthor().getUsername(),
                "displayName", post.getAuthor().getDisplayName()
            ));
        }
        
        return dto;
    }
    
    // ============ 帖子删除 ============
    
    /**
     * 删除指定帖子及其关联数据
     * DELETE /api/admin/posts/{postId}
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/posts/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable String postId,
            @AuthenticationPrincipal AppUser admin
    ) {
        return postRepository.findById(postId)
            .map(post -> {
                try {
                    // 删除关联的评论
                    commentRepository.findByPostId(postId).forEach(commentRepository::delete);
                    log.info("Deleted comments for post: {}", postId);
                    
                    // 删除关联的点赞
                    postLikeRepository.findByPostId(postId).forEach(postLikeRepository::delete);
                    log.info("Deleted likes for post: {}", postId);
                    
                    // 删除帖子本身
                    postRepository.delete(post);
                    log.info("Deleted post: {} by admin: {}", postId, admin.getUsername());
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "帖子已删除",
                        "postId", postId,
                        "deletedBy", admin.getUsername()
                    ));
                } catch (Exception e) {
                    log.error("Error deleting post: {}", postId, e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "删除帖子时发生错误: " + e.getMessage()));
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 批量删除未翻译好的泰语帖子
     * DELETE /api/admin/posts/delete-untranslated
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/posts/delete-untranslated")
    public ResponseEntity<?> deleteUntranslatedPosts(
            @AuthenticationPrincipal AppUser admin
    ) {
        try {
            // 查找要删除的帖子
            List<CommunityPost> postsToDelete = postRepository.findAll().stream()
                .filter(post -> {
                    String title = post.getTitle();
                    if (title == null) return false;
                    
                    // 检查是否包含泰语字符
                    boolean hasThaiChars = title.matches(".*[ก-๙].*");
                    if (!hasThaiChars) return false;
                    
                    // 检查标题是否仍然是泰语（说明翻译失败）
                    String titleZh = post.getTitleZh();
                    boolean titleStillThai = titleZh == null || titleZh.isEmpty() || titleZh.matches(".*[ก-๙].*");
                    
                    // 检查是否匹配特定的关键词
                    boolean matchesKeywords = title.contains("ไชยยงค์") ||
                                            title.contains("เถ้าแก่น้อย") ||
                                            title.contains("สสส.") ||
                                            title.contains("ICONIC Run Fest") ||
                                            title.contains("โอ-ออ") ||
                                            title.contains("นัทปง");
                    
                    return matchesKeywords && titleStillThai;
                })
                .collect(java.util.stream.Collectors.toList());
            
            if (postsToDelete.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "没有找到需要删除的帖子",
                    "deletedCount", 0
                ));
            }
            
            int deletedCount = 0;
            List<String> deletedPostIds = new ArrayList<>();
            
            for (CommunityPost post : postsToDelete) {
                String postId = post.getId();
                try {
                    // 删除关联的评论
                    commentRepository.findByPostId(postId).forEach(commentRepository::delete);
                    
                    // 删除关联的点赞
                    postLikeRepository.findByPostId(postId).forEach(postLikeRepository::delete);
                    
                    // 删除帖子本身
                    postRepository.delete(post);
                    
                    deletedPostIds.add(postId);
                    deletedCount++;
                    log.info("Deleted post: {} - {}", postId, post.getTitle());
                } catch (Exception e) {
                    log.error("Error deleting post: {}", postId, e);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "批量删除完成",
                "deletedCount", deletedCount,
                "deletedPostIds", deletedPostIds,
                "deletedBy", admin.getUsername()
            ));
        } catch (Exception e) {
            log.error("Error in batch delete", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "批量删除时发生错误: " + e.getMessage()));
        }
    }
    
    // ============ 帖子翻译 ============
    
    /**
     * 重新翻译指定帖子
     * POST /api/admin/posts/{postId}/translate
     */
    @PostMapping("/posts/{postId}/translate")
    public ResponseEntity<?> translatePost(@PathVariable String postId) {
        return postRepository.findById(postId)
            .map(post -> {
                try {
                    String originalLang = post.getOriginalLanguage();
                    if (originalLang == null || originalLang.isEmpty()) {
                        // 检测语言
                        String detectedLang = languageDetectionService.detectLanguage(
                            (post.getTitle() != null ? post.getTitle() : "") + " " + 
                            (post.getBody() != null ? post.getBody() : "")
                        );
                        post.setOriginalLanguage(detectedLang);
                        originalLang = detectedLang;
                        log.info("Detected language for post {}: {}", postId, detectedLang);
                    }
                    
                    String title = post.getTitle();
                    String body = post.getBody();
                    
                    if (title == null || body == null) {
                        return ResponseEntity.badRequest().body(Map.of("error", "帖子标题或内容为空"));
                    }
                    
                    // 检查标题的实际语言：如果标题是泰语但 originalLang 是中文，需要修正
                    boolean titleIsThai = languageDetectionService.containsThai(title) && 
                                          !languageDetectionService.containsChinese(title);
                    boolean titleIsChinese = languageDetectionService.containsChinese(title) && 
                                             !languageDetectionService.containsThai(title);
                    
                    // 如果标题是泰语，但 originalLang 不是泰语，需要强制翻译
                    String langForTranslation = originalLang;
                    if (titleIsThai && !"th".equals(originalLang)) {
                        log.warn("⚠️ Title is Thai but originalLang is {}, forcing translation from Thai", originalLang);
                        langForTranslation = "th";
                    } else if (titleIsChinese && !"zh".equals(originalLang)) {
                        log.warn("⚠️ Title is Chinese but originalLang is {}, forcing translation from Chinese", originalLang);
                        langForTranslation = "zh";
                    }
                    
                    // 提取实际内容（去除格式标记）
                    String actualContent = extractActualContent(body);
                    String actualTitle = title;
                    
                    // 翻译（使用修正后的语言）
                    TranslationService.TranslationResult translationResult = 
                        translationService.translateContent(actualTitle, actualContent, langForTranslation);
                    
                    // 重建翻译内容（保留格式标记）
                    String translatedBodyZh = rebuildTranslatedContent(body, translationResult.getBodyZh(), originalLang, "zh");
                    String translatedBodyEn = rebuildTranslatedContent(body, translationResult.getBodyEn(), originalLang, "en");
                    
                    // 设置中文翻译 - 放宽验证
                    if (translationResult.getTitleZh() != null && !translationResult.getTitleZh().isEmpty()) {
                        // 只要翻译结果不是泰语就接受
                        if (!languageDetectionService.containsThai(translationResult.getTitleZh())) {
                            post.setTitleZh(translationResult.getTitleZh());
                            log.info("✅ Set Chinese title translation for post: {} -> {}", postId, 
                                    translationResult.getTitleZh().substring(0, Math.min(50, translationResult.getTitleZh().length())));
                        } else {
                            log.warn("⚠️ Translation result contains Thai characters, rejecting: {} for post: {}", 
                                    translationResult.getTitleZh().substring(0, Math.min(50, translationResult.getTitleZh().length())), postId);
                        }
                    }
                    
                    // 如果 titleZh 仍然为空，检查原始标题是否包含中文
                    if (post.getTitleZh() == null || post.getTitleZh().isEmpty()) {
                        // 只要标题包含任何中文字符，就使用原标题作为 titleZh
                        if (languageDetectionService.containsChinese(title)) {
                            post.setTitleZh(title);
                            log.info("✅ Post title contains Chinese, using original as titleZh");
                        } else if ("zh".equals(originalLang)) {
                            post.setTitleZh(title);
                            log.info("✅ Post detected as Chinese, using original as titleZh");
                        } else {
                            log.warn("⚠️ TitleZh is null for post: {} (originalLang: {}, containsChinese: {})", 
                                    postId, originalLang, languageDetectionService.containsChinese(title));
                        }
                    }
                    
                    if (translatedBodyZh != null && !translatedBodyZh.isEmpty()) {
                        // 即使和原文相同也保存（可能是翻译服务返回了相同内容，但至少确保字段有值）
                        post.setContentZh(translatedBodyZh);
                        log.info("✅ Set Chinese content translation for post: {} (length: {})", postId, translatedBodyZh.length());
                    } else if ("zh".equals(originalLang)) {
                        post.setContentZh(body);
                        log.info("✅ Post is already in Chinese, using original content");
                    } else {
                        // 如果翻译失败，尝试使用翻译服务的原始结果
                        if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                            post.setContentZh(translationResult.getBodyZh());
                            log.info("✅ Set Chinese content translation from raw result for post: {} (length: {})", postId, translationResult.getBodyZh().length());
                        } else {
                            log.warn("⚠️ Chinese content translation is empty for post: {} (translatedBodyZh: null, bodyZh from service: {})", 
                                    postId, 
                                    translationResult.getBodyZh() != null ? translationResult.getBodyZh().length() : 0);
                        }
                    }
                    
                    // 设置英文翻译
                    if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                        post.setTitleEn(translationResult.getTitleEn());
                    } else if ("en".equals(originalLang)) {
                        post.setTitleEn(title);
                    }
                    
                    if (translatedBodyEn != null && !translatedBodyEn.isEmpty() && !translatedBodyEn.equals(body)) {
                        post.setContentEn(translatedBodyEn);
                    } else if ("en".equals(originalLang)) {
                        post.setContentEn(body);
                    }
                    
                    postRepository.save(post);
                    
                    log.info("✅ Successfully translated post: {}", postId);
                    
                    // 验证翻译结果
                    boolean titleZhOk = post.getTitleZh() != null && !post.getTitleZh().isEmpty() && !post.getTitleZh().equals(title);
                    boolean titleEnOk = post.getTitleEn() != null && !post.getTitleEn().isEmpty() && !post.getTitleEn().equals(title);
                    boolean contentZhOk = post.getContentZh() != null && !post.getContentZh().isEmpty() && !post.getContentZh().equals(body);
                    boolean contentEnOk = post.getContentEn() != null && !post.getContentEn().isEmpty() && !post.getContentEn().equals(body);
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "翻译成功",
                        "postId", postId,
                        "titleZh", titleZhOk ? "已翻译" : "未翻译",
                        "titleEn", titleEnOk ? "已翻译" : "未翻译",
                        "contentZh", contentZhOk ? "已翻译" : "未翻译",
                        "contentEn", contentEnOk ? "已翻译" : "未翻译",
                        "titleZhLength", post.getTitleZh() != null ? post.getTitleZh().length() : 0,
                        "contentZhLength", post.getContentZh() != null ? post.getContentZh().length() : 0
                    ));
                } catch (Exception e) {
                    log.error("❌ Failed to translate post {}: {}", postId, e.getMessage(), e);
                    return ResponseEntity.status(500).body(Map.of("error", "翻译失败: " + e.getMessage()));
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 提取实际内容（去除格式标记）
     */
    private String extractActualContent(String postBody) {
        if (postBody == null || postBody.isEmpty()) {
            return "";
        }
        
        String content = postBody;
        content = content.replaceAll("📝\\s*\\*\\*AI Summary\\*\\*\\s*\n+", "");
        content = content.replaceAll("📝\\s*\\*\\*News Summary\\*\\*\\s*\n+", "");
        content = content.replaceAll("📄\\s*\\*\\*Detailed Content\\*\\*\\s*\n+", "");
        content = content.replaceAll("🔗\\s*\\*\\*Read Original\\*\\*:\\s*", "");
        content = content.replaceAll("🔗\\s*\\*\\*Source\\*\\*:\\s*", "");
        content = content.replaceAll("---+\\s*\n*", "");
        content = content.replaceAll("https?://[^\\s]+", "");
        content = content.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        content = content.replaceAll("\n{3,}", "\n\n");
        return content.trim();
    }
    
    /**
     * 重建翻译内容（保留格式标记）
     * 参考 NewsToPostService 的实现
     */
    private String rebuildTranslatedContent(String originalBody, String translatedContent, String sourceLang, String targetLang) {
        if (translatedContent == null || translatedContent.isEmpty()) {
            return null;
        }
        
        // If source language matches target, return original
        if (sourceLang != null && sourceLang.equals(targetLang)) {
            return originalBody;
        }
        
        if (originalBody == null || originalBody.isEmpty()) {
            return translatedContent;
        }
        
        // Extract the structure from original body
        StringBuilder result = new StringBuilder();
        
        // Check for AI Summary section
        if (originalBody.contains("📝 **AI Summary**")) {
            result.append("📝 **AI Summary**\n\n");
            // Find the content after AI Summary header
            int summaryStart = originalBody.indexOf("📝 **AI Summary**");
            int contentStart = originalBody.indexOf("\n\n", summaryStart) + 2;
            if (contentStart > 1) {
                int nextSection = originalBody.indexOf("📄", contentStart);
                int nextLink = originalBody.indexOf("🔗", contentStart);
                int endPos = originalBody.length();
                if (nextSection > 0 && nextSection < endPos) endPos = nextSection;
                if (nextLink > 0 && nextLink < endPos) endPos = nextLink;
                // Use translated content for the summary part
                result.append(translatedContent);
                // Add remaining sections
                if (endPos < originalBody.length()) {
                    result.append("\n\n");
                    result.append(originalBody.substring(endPos));
                }
            } else {
                result.append(translatedContent);
            }
        } 
        // Check for News Summary section
        else if (originalBody.contains("📝 **News Summary**")) {
            result.append("📝 **News Summary**\n\n");
            int summaryEnd = originalBody.indexOf("📝 **News Summary**") + "📝 **News Summary**".length();
            int nextSection = originalBody.indexOf("📄", summaryEnd);
            int nextLink = originalBody.indexOf("🔗", summaryEnd);
            int endPos = originalBody.length();
            if (nextSection > 0 && nextSection < endPos) endPos = nextSection;
            if (nextLink > 0 && nextLink < endPos) endPos = nextLink;
            result.append(translatedContent);
            if (endPos < originalBody.length()) {
                result.append("\n\n");
                result.append(originalBody.substring(endPos));
            }
        }
        // Check for Detailed Content section
        else if (originalBody.contains("📄 **Detailed Content**")) {
            int detailedStart = originalBody.indexOf("📄 **Detailed Content**");
            result.append(originalBody.substring(0, detailedStart));
            result.append("📄 **Detailed Content**\n\n");
            int nextLink = originalBody.indexOf("🔗", detailedStart);
            if (nextLink > 0) {
                result.append(translatedContent);
                result.append("\n\n");
                result.append(originalBody.substring(nextLink));
            } else {
                result.append(translatedContent);
            }
        }
        // No specific structure, just replace the main content
        else {
            // Try to preserve link section if exists
            if (originalBody.contains("🔗")) {
                int linkIndex = originalBody.indexOf("🔗");
                String beforeLink = originalBody.substring(0, linkIndex).trim();
                String linkSection = originalBody.substring(linkIndex);
                
                // If the content before link is similar to translated content, use translated
                if (beforeLink.length() > 0) {
                    result.append(translatedContent);
                    result.append("\n\n");
                    result.append(linkSection);
                } else {
                    result.append(translatedContent);
                }
            } else {
                result.append(translatedContent);
            }
        }
        
        return result.toString();
    }
}
