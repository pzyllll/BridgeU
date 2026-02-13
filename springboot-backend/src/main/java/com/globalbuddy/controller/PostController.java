package com.globalbuddy.controller;

import com.globalbuddy.dto.*;
import com.globalbuddy.model.*;
import com.globalbuddy.repository.*;
import com.globalbuddy.service.AiSummaryService;
import com.globalbuddy.service.ContentModerationService;
import com.globalbuddy.service.LanguageDetectionService;
import com.globalbuddy.service.SemanticService;
import com.globalbuddy.service.TranslationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final CommunityPostRepository postRepository;
    private final CommunityRepository communityRepository;
    private final AppUserRepository userRepository;
    private final SemanticService semanticService;
    private final LanguageDetectionService languageDetectionService;
    private final TranslationService translationService;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserFollowRepository userFollowRepository;
    private final ContentModerationService contentModerationService;
    private final AiSummaryService aiSummaryService;
    @Value("${file.upload.base-path:C:/Users/pzy/Documents/java/work/hh/pictures}")
    private String uploadBasePath;

    @GetMapping
    public List<PostListResponse> listPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "en") String lang,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        log.info("Fetching posts with language preference: {}, page: {}, size: {}", lang, page, size);
        List<CommunityPost> posts = postRepository.findAllByOrderByCreatedAtDesc();
        
        // 1) 只向前台展示已经通过审核的帖子
        // 2) 过滤掉被举报下架的帖子
        // 3) 过滤掉从新闻自动转换来的帖子（category = "News & Information" 或 tags 中包含 "News"）
        //    现在新闻只在每日简报里展示，不再出现在社区列表
        posts = posts.stream()
                .filter(post -> post.getStatus() == CommunityPost.Status.APPROVED) // 只显示已审核通过的帖子（自动过滤被举报下架的帖子）
                .filter(post -> {
                    // 过滤掉系统自动生成的新闻贴（旧数据安全兜底）
                    AppUser author = post.getAuthor();
                    if (author != null && author.getEmail() != null &&
                            "system@globalbuddy.com".equalsIgnoreCase(author.getEmail().trim())) {
                        log.debug("Filtering out system-generated news post {} by author email", post.getId());
                        return false;
                    }

                    String category = post.getCategory();
                    if (category != null && "News & Information".equalsIgnoreCase(category.trim())) {
                        log.debug("Filtering out news-based post {} by category", post.getId());
                        return false;
                    }
                    // tags 里包含 "News" 也视为新闻帖子
                    if (post.getTags() != null) {
                        boolean hasNewsTag = post.getTags().stream()
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .anyMatch(tag -> tag.equalsIgnoreCase("News"));
                        if (hasNewsTag) {
                            log.debug("Filtering out news-based post {} by tag", post.getId());
                            return false;
                        }
                    }
                    return true;
                })
                .filter(post -> {
                    // 不按原始语言过滤，翻译由 toPostResponse 处理
                    // 仅过滤掉完全没有内容的帖子
                    boolean hasContent = (post.getBody() != null && !post.getBody().trim().isEmpty()) ||
                                         (post.getContentZh() != null && !post.getContentZh().trim().isEmpty()) ||
                                         (post.getContentEn() != null && !post.getContentEn().trim().isEmpty());
                    if (!hasContent) {
                        log.debug("Filtering out post {} - no content available", post.getId());
                    }
                    return hasContent;
                })
                .collect(Collectors.toList());
        
        log.info("Total posts available: {} (requested language: {})", posts.size(), lang);
        List<PostListResponse> responses = new ArrayList<>();
        int zhTranslatedCount = 0;
        int enTranslatedCount = 0;
        for (CommunityPost post : posts) {
            PostResponse postResponse = toPostResponse(post, lang);
            
            // Get statistics
            long likeCount = postLikeRepository.countByPost(post);
            long commentCount = commentRepository.countByPost(post);
            
            // Get author name and avatar
            AppUser author = post.getAuthor();
            String authorName = author != null ? (author.getDisplayName() != null ? author.getDisplayName() : author.getUsername()) : "Unknown";
            String authorAvatar = author != null ? author.getAvatar() : null;
            
            PostListResponse response = PostListResponse.builder()
                    .id(postResponse.getId())
                    .communityId(postResponse.getCommunityId())
                    .authorId(postResponse.getAuthorId())
                    .authorName(authorName)
                    .authorAvatar(authorAvatar)
                    .title(postResponse.getTitle())
                    .body(postResponse.getBody())
                    .tags(postResponse.getTags())
                    .category(postResponse.getCategory())
                    .createdAt(postResponse.getCreatedAt())
                    .updatedAt(postResponse.getUpdatedAt())
                    .contentZh(postResponse.getContentZh())
                    .contentEn(postResponse.getContentEn())
                    .originalLanguage(postResponse.getOriginalLanguage())
                    .imageUrl(postResponse.getImageUrl())
                    .likeCount(likeCount)
                    .commentCount(commentCount)
                    .build();
            
            responses.add(response);
            
            // Count how many posts have translations
            if ("zh".equals(lang) && post.getContentZh() != null && !post.getContentZh().isEmpty()) {
                zhTranslatedCount++;
            } else if ("en".equals(lang) && post.getContentEn() != null && !post.getContentEn().isEmpty()) {
                enTranslatedCount++;
            }
        }
        if ("zh".equals(lang)) {
            log.info("Posts with Chinese translation: {}/{}", zhTranslatedCount, posts.size());
        } else if ("en".equals(lang)) {
            log.info("Posts with English translation: {}/{}", enTranslatedCount, posts.size());
        }
        
        // Apply search filter if query provided
        if (StringUtils.hasText(q)) {
            List<PostListResponse> filtered = new ArrayList<>();
            for (PostListResponse response : responses) {
                double score = semanticService.calculateScore(q, response.getTitle() + " " + response.getBody());
                if (score > 0) {
                    response.setScore(score);
                    filtered.add(response);
                }
            }
            filtered.sort((a, b) -> {
                if (a.getScore() != null && b.getScore() != null) {
                    return Double.compare(b.getScore(), a.getScore());
                }
                return 0;
            });
            responses = filtered;
        }
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, responses.size());
        if (start >= responses.size()) {
            return new ArrayList<>();
        }
        return responses.subList(start, end);
    }

    /**
     * 获取当前用户被拒绝的帖子列表（用于个人页面显示审核结果）
     */
    @GetMapping("/my/rejected")
    public ResponseEntity<List<Map<String, Object>>> getMyRejectedPosts() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<CommunityPost> posts = postRepository.findByAuthorIdAndStatusOrderByCreatedAtDesc(
                currentUser.getId(), CommunityPost.Status.REJECTED);

        List<Map<String, Object>> result = posts.stream().map(post -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", post.getId());
            map.put("title", post.getTitle());
            map.put("status", post.getStatus().name());
            map.put("reviewNote", post.getReviewNote());
            map.put("reviewedAt", post.getReviewedAt());
            map.put("createdAt", post.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDetailResponse> getPost(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        
        log.info("🔍 getPost - Request: postId={}, lang={}", id, lang);
        
        Optional<CommunityPost> postOpt = postRepository.findById(id);
        if (!postOpt.isPresent()) {
            log.warn("❌ getPost - Post not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        CommunityPost post = postOpt.get();
        AppUser currentUser = getCurrentUser();
        
        // Debug: Log post data from database
        log.info("🔍 getPost - Post from DB: id={}, title={}, titleZh={}, titleEn={}, body length={}, contentZh length={}, contentEn length={}, originalLang={}", 
                post.getId(), 
                post.getTitle() != null ? post.getTitle().substring(0, Math.min(50, post.getTitle().length())) : "null",
                post.getTitleZh() != null ? post.getTitleZh().substring(0, Math.min(50, post.getTitleZh().length())) : "null",
                post.getTitleEn() != null ? post.getTitleEn().substring(0, Math.min(50, post.getTitleEn().length())) : "null",
                post.getBody() != null ? post.getBody().length() : 0,
                post.getContentZh() != null ? post.getContentZh().length() : 0,
                post.getContentEn() != null ? post.getContentEn().length() : 0,
                post.getOriginalLanguage());
        
        // Check if titleZh is actually populated
        if (post.getTitleZh() != null && !post.getTitleZh().isEmpty()) {
            log.info("✅ getPost - titleZh is available (length={}): {}", 
                    post.getTitleZh().length(), 
                    post.getTitleZh().substring(0, Math.min(100, post.getTitleZh().length())));
        } else {
            log.warn("⚠️ getPost - titleZh is NULL or EMPTY for post: {}", post.getId());
        }
        
        // Get post response with language preference
        PostResponse postResponse = toPostResponse(post, lang);
        
        // Debug: Log post response - show full title
        log.info("📤 getPost - PostResponse: title={}, title length={}, body length={}, lang={}", 
                postResponse.getTitle() != null ? postResponse.getTitle().substring(0, Math.min(100, postResponse.getTitle().length())) : "null",
                postResponse.getTitle() != null ? postResponse.getTitle().length() : 0,
                postResponse.getBody() != null ? postResponse.getBody().length() : 0,
                lang);
        
        // Get author info
        AppUser author = post.getAuthor();
        String authorName = author != null ? author.getUsername() : "Unknown";
        String authorDisplayName = author != null ? author.getDisplayName() : "Unknown";
        String authorAvatar = author != null ? author.getAvatar() : null;
        
        // Get like count and check if current user liked
        long likeCount = postLikeRepository.countByPost(post);
        boolean isLiked = currentUser != null && postLikeRepository.existsByPostAndUser(post, currentUser);
        
        // Get comment count
        long commentCount = commentRepository.countByPost(post);
        
        // Check if current user is following the author
        boolean isFollowing = false;
        if (currentUser != null && author != null && !currentUser.getId().equals(author.getId())) {
            isFollowing = userFollowRepository.existsByFollowerAndFollowing(currentUser, author);
        }
        
        // Get comments with language preference (filter out reported/removed comments)
        List<Comment> comments = commentRepository.findByPostOrderByCreatedAtDesc(post);
        List<CommentResponse> commentResponses = comments.stream()
                .filter(comment -> comment.getStatus() == null || comment.getStatus() == Comment.Status.ACTIVE) // Filter out reported/removed comments
                .map(comment -> toCommentResponse(comment, lang))
                .collect(Collectors.toList());
        
        PostDetailResponse response = PostDetailResponse.builder()
                .post(postResponse)
                .authorName(authorName)
                .authorDisplayName(authorDisplayName)
                .authorId(author != null ? author.getId() : null)
                .authorAvatar(authorAvatar)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .commentCount(commentCount)
                .isFollowing(isFollowing)
                .comments(commentResponses)
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Add a comment to a post
     */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String postId,
            @Valid @RequestBody CommentRequest request,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<CommunityPost> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        CommunityPost post = postOpt.get();
        
        // Detect language
        String detectedLang = languageDetectionService.detectLanguage(request.getContent());
        
        Comment comment = Comment.builder()
                .id(UUID.randomUUID().toString())
                .post(post)
                .author(currentUser)
                .content(request.getContent())
                .originalLanguage(detectedLang)
                .build();
        
        // Auto-translate comment
        try {
            TranslationService.TranslationResult translationResult = 
                translationService.translateContent("", request.getContent(), detectedLang);
            
            if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                comment.setContentZh(translationResult.getBodyZh());
            } else if ("zh".equals(detectedLang)) {
                comment.setContentZh(request.getContent());
            }
            
            if (translationResult.getBodyEn() != null && !translationResult.getBodyEn().isEmpty()) {
                comment.setContentEn(translationResult.getBodyEn());
            } else if ("en".equals(detectedLang)) {
                comment.setContentEn(request.getContent());
            }
        } catch (Exception e) {
            log.error("Failed to translate comment: {}", e.getMessage());
        }
        
        Comment saved = commentRepository.save(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCommentResponse(saved, lang));
    }
    
    /**
     * Delete a comment
     * DELETE /api/posts/{postId}/comments/{commentId}
     */
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId) {
        
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (!commentOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Comment comment = commentOpt.get();
        
        // Check if the comment belongs to the current user
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "You can only delete your own comments");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        
        // Verify the comment belongs to the post
        if (!comment.getPost().getId().equals(postId)) {
            return ResponseEntity.badRequest().build();
        }
        
        commentRepository.delete(comment);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Comment deleted successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Generate AI summary of all comments for a post
     */
    @GetMapping("/{postId}/comments/summary")
    public ResponseEntity<Map<String, Object>> getCommentSummary(
            @PathVariable String postId,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        
        log.info("Generating comment summary for post: {}, lang: {}", postId, lang);
        
        Optional<CommunityPost> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        CommunityPost post = postOpt.get();
        
        // Get all comments for this post (filter out reported/removed comments)
        List<Comment> comments = commentRepository.findByPostOrderByCreatedAtDesc(post).stream()
                .filter(comment -> comment.getStatus() == null || comment.getStatus() == Comment.Status.ACTIVE)
                .collect(Collectors.toList());
        
        if (comments.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("summary", lang.equals("zh") ? "暂无评论可总结" : "No comments to summarize");
            response.put("commentCount", 0);
            return ResponseEntity.ok(response);
        }
        
        // Extract comment contents based on language preference
        List<String> commentContents = new ArrayList<>();
        for (Comment comment : comments) {
            String content = null;
            if ("zh".equals(lang)) {
                content = comment.getContentZh() != null && !comment.getContentZh().isEmpty() 
                    ? comment.getContentZh() 
                    : comment.getContent();
            } else {
                content = comment.getContentEn() != null && !comment.getContentEn().isEmpty() 
                    ? comment.getContentEn() 
                    : comment.getContent();
            }
            if (content != null && !content.trim().isEmpty()) {
                commentContents.add(content);
            }
        }
        
        if (commentContents.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("summary", lang.equals("zh") ? "暂无有效评论可总结" : "No valid comments to summarize");
            response.put("commentCount", 0);
            return ResponseEntity.ok(response);
        }
        
        try {
            // Generate summary using AI
            String summary = aiSummaryService.generateCommentSummary(commentContents, lang);
            
            Map<String, Object> response = new HashMap<>();
            response.put("summary", summary);
            response.put("commentCount", commentContents.size());
            
            log.info("Comment summary generated successfully for {} comments", commentContents.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to generate comment summary: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", lang.equals("zh") ? "生成评论总结失败" : "Failed to generate comment summary");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Like or unlike a post
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable String postId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<CommunityPost> postOpt = postRepository.findById(postId);
        if (!postOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        CommunityPost post = postOpt.get();
        Optional<PostLike> likeOpt = postLikeRepository.findByPostAndUser(post, currentUser);
        
        Map<String, Object> response = new HashMap<>();
        
        if (likeOpt.isPresent()) {
            // Unlike
            postLikeRepository.delete(likeOpt.get());
            response.put("liked", false);
            response.put("message", "Post unliked");
        } else {
            // Like
            PostLike like = PostLike.builder()
                    .id(UUID.randomUUID().toString())
                    .post(post)
                    .user(currentUser)
                    .build();
            postLikeRepository.save(like);
            response.put("liked", true);
            response.put("message", "Post liked");
        }
        
        long likeCount = postLikeRepository.countByPost(post);
        response.put("likeCount", likeCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Follow or unfollow a user (post author)
     * Note: This endpoint is under /api/posts/users/{userId}/follow for convenience
     * Alternative: Could be moved to a UserController
     */
    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<Map<String, Object>> toggleFollow(@PathVariable String userId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Optional<AppUser> targetUserOpt = userRepository.findById(userId);
        if (!targetUserOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        AppUser targetUser = targetUserOpt.get();
        
        if (currentUser.getId().equals(targetUser.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot follow yourself"));
        }
        
        Optional<UserFollow> followOpt = userFollowRepository.findByFollowerAndFollowing(currentUser, targetUser);
        
        Map<String, Object> response = new HashMap<>();
        
        if (followOpt.isPresent()) {
            // Unfollow
            userFollowRepository.delete(followOpt.get());
            response.put("following", false);
            response.put("message", "User unfollowed");
        } else {
            // Follow
            UserFollow follow = UserFollow.builder()
                    .id(UUID.randomUUID().toString())
                    .follower(currentUser)
                    .following(targetUser)
                    .build();
            userFollowRepository.save(follow);
            response.put("following", true);
            response.put("message", "User followed");
        }
        
        long followersCount = userFollowRepository.countByFollowing(targetUser);
        response.put("followersCount", followersCount);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get current authenticated user
     */
    private AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())
            && authentication.getPrincipal() instanceof AppUser) {
            return (AppUser) authentication.getPrincipal();
        }
        return null;
    }
    
    /**
     * Convert Comment to CommentResponse with language preference
     */
    private CommentResponse toCommentResponse(Comment comment, String lang) {
        String content = comment.getContent();
        
        if ("zh".equals(lang)) {
            if (comment.getContentZh() != null && !comment.getContentZh().isEmpty()) {
                content = comment.getContentZh();
            }
        } else if ("en".equals(lang)) {
            if (comment.getContentEn() != null && !comment.getContentEn().isEmpty()) {
                content = comment.getContentEn();
            }
        }
        
        AppUser author = comment.getAuthor();
        return CommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorId(author != null ? author.getId() : null)
                .authorName(author != null ? author.getUsername() : "Unknown")
                .authorDisplayName(author != null ? author.getDisplayName() : "Unknown")
                .authorAvatar(author != null ? author.getAvatar() : null)
                .content(content)
                .contentZh(comment.getContentZh())
                .contentEn(comment.getContentEn())
                .originalLanguage(comment.getOriginalLanguage())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        Optional<Community> communityOpt = communityRepository.findById(request.getCommunityId());
        // 如果社区不存在，允许为空社区，避免 400
        Optional<AppUser> authorOpt = userRepository.findById(request.getAuthorId());
        if (!authorOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Detect language of the post content
        String combinedText = (request.getTitle() != null ? request.getTitle() + " " : "") + 
                             (request.getBody() != null ? request.getBody() : "");
        String detectedLang = languageDetectionService.detectLanguage(combinedText);
        log.info("Detected language for post: {} (title: {})", detectedLang, request.getTitle());

        CommunityPost post = new CommunityPost();
        post.setId(UUID.randomUUID().toString());
        post.setCommunity(communityOpt.orElse(null));
        post.setAuthor(authorOpt.get());
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setTags(request.getTags());
        post.setCategory(request.getCategory());
        post.setImageUrl(request.getImageUrl());
        post.setOriginalLanguage(detectedLang);

        // Auto-translate to Chinese and English
        try {
            TranslationService.TranslationResult translationResult = 
                translationService.translateContent(request.getTitle(), request.getBody(), detectedLang);
            
            // Set Chinese title translation
            if (translationResult.getTitleZh() != null && !translationResult.getTitleZh().isEmpty()) {
                post.setTitleZh(translationResult.getTitleZh());
                log.info("✅ Chinese title translation set for post");
            }
            // 回退逻辑：如果翻译失败，检查原标题是否包含中文
            if (post.getTitleZh() == null || post.getTitleZh().isEmpty()) {
                if (languageDetectionService.containsChinese(request.getTitle())) {
                    post.setTitleZh(request.getTitle());
                    log.info("✅ Post title contains Chinese, using original as titleZh");
                } else if ("zh".equals(detectedLang)) {
                    post.setTitleZh(request.getTitle());
                    log.info("✅ Post detected as Chinese, using original as titleZh");
                }
            }
            
            // Set Chinese content translation
            if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                post.setContentZh(translationResult.getBodyZh());
                log.info("✅ Chinese content translation completed for post");
            } else if (languageDetectionService.containsChinese(request.getBody())) {
                post.setContentZh(request.getBody());
                log.info("✅ Post body contains Chinese, using original as contentZh");
            } else if ("zh".equals(detectedLang)) {
                post.setContentZh(request.getBody());
                log.info("✅ Post detected as Chinese, using original as contentZh");
            }

            // Set English title translation
            if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                post.setTitleEn(translationResult.getTitleEn());
                log.info("✅ English title translation set for post");
            }
            // 回退逻辑：如果翻译失败，检查原标题是否是英文
            if (post.getTitleEn() == null || post.getTitleEn().isEmpty()) {
                if ("en".equals(detectedLang)) {
                    post.setTitleEn(request.getTitle());
                    log.info("✅ Post detected as English, using original as titleEn");
                }
            }

            // Set English content translation
            if (translationResult.getBodyEn() != null && !translationResult.getBodyEn().isEmpty()) {
                post.setContentEn(translationResult.getBodyEn());
                log.info("✅ English content translation completed for post");
            } else if ("en".equals(detectedLang)) {
                post.setContentEn(request.getBody());
                log.info("✅ Post detected as English, using original as contentEn");
            }
            
            // 记录翻译结果
            log.info("📝 Post translation result: titleZh={}, titleEn={}, contentZh={}, contentEn={}",
                    post.getTitleZh() != null && !post.getTitleZh().isEmpty(),
                    post.getTitleEn() != null && !post.getTitleEn().isEmpty(),
                    post.getContentZh() != null && !post.getContentZh().isEmpty(),
                    post.getContentEn() != null && !post.getContentEn().isEmpty());
        } catch (Exception e) {
            log.error("❌ Translation failed for post: {} - {}", request.getTitle(), e.getMessage(), e);
            // Fallback: 检查原内容语言并设置相应字段
            if (languageDetectionService.containsChinese(request.getTitle())) {
                post.setTitleZh(request.getTitle());
            }
            if (languageDetectionService.containsChinese(request.getBody())) {
                post.setContentZh(request.getBody());
            }
            if ("en".equals(detectedLang)) {
                post.setTitleEn(request.getTitle());
                post.setContentEn(request.getBody());
            }
        }

        // AI 内容审核与状态决策
        try {
            ContentModerationService.ModerationResult moderation = contentModerationService.moderatePost(post);
            post.setAiResult(moderation.getAiResult());
            post.setAiConfidence(moderation.getConfidence());
            if (moderation.getStatus() != null) {
                post.setStatus(moderation.getStatus());
            }
        } catch (Exception e) {
            log.error("内容审核失败，标记为待审核: {}", post.getId(), e);
            post.setStatus(CommunityPost.Status.PENDING_REVIEW);
        }

        CommunityPost saved = postRepository.save(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPostResponse(saved, "en"));
    }

    /**
     * 检查并创建上传目录
     */
    @PostConstruct
    public void init() {
        try {
            Path uploadDir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                log.info("Creating upload directory: {}", uploadDir);
                Files.createDirectories(uploadDir);
                log.info("Upload directory created successfully");
            } else {
                log.info("Using existing upload directory: {}", uploadDir);
            }
            
            // 测试目录可写性
            Path testFile = uploadDir.resolve(".test-write-" + System.currentTimeMillis());
            Files.writeString(testFile, "test");
            Files.deleteIfExists(testFile);
            log.info("Upload directory is writable");
            
        } catch (Exception e) {
            log.error("Failed to initialize upload directory: " + uploadBasePath, e);
            throw new RuntimeException("Failed to initialize upload directory", e);
        }
    }

    /**
     * Upload post image
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@RequestPart("file") MultipartFile file) {
        log.info("Received file upload request: {}", file.getOriginalFilename());
        
        if (file == null || file.isEmpty()) {
            log.warn("Upload failed: File is empty");
            return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
        }
        
        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!Arrays.asList("jpg", "jpeg", "png", "gif").contains(fileExtension)) {
            log.warn("Upload failed: Invalid file extension: {}", fileExtension);
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的文件类型，仅支持 JPG, PNG, GIF"));
        }
        
        try {
            // 准备目录和文件名
            String sanitized = sanitizeFileName(originalFilename);
            String newName = UUID.randomUUID() + "_" + sanitized;
            Path dir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            
            // 确保目录存在
            if (!Files.exists(dir)) {
                log.warn("Upload directory does not exist, creating: {}", dir);
                Files.createDirectories(dir);
            }
            
            // 检查目录是否可写
            if (!Files.isWritable(dir)) {
                log.error("Upload directory is not writable: {}", dir);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "服务器存储目录不可写"));
            }
            
            // 保存文件
            Path target = dir.resolve(newName);
            log.info("Saving uploaded file to: {}", target);
            file.transferTo(target.toFile());
            
            // 验证文件是否保存成功
            if (!Files.exists(target) || Files.size(target) == 0) {
                log.error("Failed to save uploaded file: {}", target);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "文件保存失败"));
            }
            
            // 返回相对URL
            String url = "/pictures/" + newName;
            log.info("File uploaded successfully: {}", url);
            return ResponseEntity.ok(Map.of("url", url));
            
        } catch (IOException e) {
            log.error("Failed to upload file: " + originalFilename, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "文件上传失败: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "服务器内部错误: " + e.getMessage()));
        }
    }

    private String sanitizeFileName(String original) {
        if (!StringUtils.hasText(original)) {
            return "image";
        }
        return original.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }

    public PostResponse toPostResponse(CommunityPost post, String lang) {
        String title = post.getTitle();
        String body = post.getBody();
        
        // Check if original content contains Thai - we should never show Thai on the website
        boolean originalTitleIsThai = languageDetectionService.hasAnyThai(post.getTitle());
        boolean originalBodyIsThai = languageDetectionService.hasAnyThai(post.getBody());
        
        log.debug("🔄 toPostResponse - Input: postId={}, lang={}, titleZh={}, titleEn={}, originalTitleIsThai={}", 
                post.getId(), lang,
                post.getTitleZh() != null && !post.getTitleZh().isEmpty(),
                post.getTitleEn() != null && !post.getTitleEn().isEmpty(),
                originalTitleIsThai);

        // Return translated title and content based on language preference
        // IMPORTANT: Never show Thai content on the website
        if ("zh".equals(lang)) {
            // Use Chinese translation if available
            if (post.getTitleZh() != null && !post.getTitleZh().isEmpty()) {
                title = post.getTitleZh();
                log.info("✅ Using Chinese title translation for post {}", post.getId());
            } else if (originalTitleIsThai) {
                // If original is Thai and no Chinese translation, try English as fallback
                if (post.getTitleEn() != null && !post.getTitleEn().isEmpty()) {
                    title = post.getTitleEn();
                    log.warn("⚠️ Chinese title not available, using English fallback for Thai post: {}", post.getId());
                } else {
                    title = "[帖子标题翻译中...]";
                    log.error("❌ No translation available for Thai post title: {}", post.getId());
                }
            } else {
                log.debug("⚠️ Chinese title not available for post: {}, using original (non-Thai)", post.getId());
            }
            
            if (post.getContentZh() != null && !post.getContentZh().isEmpty()) {
                body = post.getContentZh();
                log.debug("Using Chinese content translation for post: {}", post.getId());
            } else if (originalBodyIsThai) {
                // If original is Thai and no Chinese translation, try English as fallback
                if (post.getContentEn() != null && !post.getContentEn().isEmpty()) {
                    body = post.getContentEn();
                    log.warn("⚠️ Chinese content not available, using English fallback for Thai post: {}", post.getId());
                } else {
                    body = "[帖子内容翻译中...]";
                    log.error("❌ No translation available for Thai post content: {}", post.getId());
                }
            } else {
                body = post.getBody();
                log.debug("Chinese content not available for post: {}, using original (non-Thai)", post.getId());
            }
        } else if ("en".equals(lang)) {
            // Use English translation if available
            if (post.getTitleEn() != null && !post.getTitleEn().isEmpty()) {
                title = post.getTitleEn();
                log.info("✅ Using English title translation for post {}", post.getId());
            } else if (originalTitleIsThai) {
                // If original is Thai and no English translation, try Chinese as fallback
                if (post.getTitleZh() != null && !post.getTitleZh().isEmpty()) {
                    title = post.getTitleZh();
                    log.warn("⚠️ English title not available, using Chinese fallback for Thai post: {}", post.getId());
                } else {
                    title = "[Post title translating...]";
                    log.error("❌ No translation available for Thai post title: {}", post.getId());
                }
            } else {
                log.debug("⚠️ English title not available for post: {}, using original (non-Thai)", post.getId());
            }
            
            if (post.getContentEn() != null && !post.getContentEn().isEmpty()) {
                body = post.getContentEn();
                log.debug("Using English content translation for post: {}", post.getId());
            } else if (originalBodyIsThai) {
                // If original is Thai and no English translation, try Chinese as fallback
                if (post.getContentZh() != null && !post.getContentZh().isEmpty()) {
                    body = post.getContentZh();
                    log.warn("⚠️ English content not available, using Chinese fallback for Thai post: {}", post.getId());
                } else {
                    body = "[Post content translating...]";
                    log.error("❌ No translation available for Thai post content: {}", post.getId());
                }
            } else {
                body = post.getBody();
                log.debug("English content not available for post: {}, using original (non-Thai)", post.getId());
            }
        } else {
            // Default to English for unknown language preference
            log.warn("⚠️ Unknown language preference: {}, defaulting to English for post: {}", lang, post.getId());
            if (post.getTitleEn() != null && !post.getTitleEn().isEmpty()) {
                title = post.getTitleEn();
            } else if (originalTitleIsThai && post.getTitleZh() != null && !post.getTitleZh().isEmpty()) {
                title = post.getTitleZh();
            }
            if (post.getContentEn() != null && !post.getContentEn().isEmpty()) {
                body = post.getContentEn();
            } else if (originalBodyIsThai && post.getContentZh() != null && !post.getContentZh().isEmpty()) {
                body = post.getContentZh();
            }
        }
        
        // Ensure body is not null or empty - provide fallback
        if (body == null || body.trim().isEmpty()) {
            body = title != null && !title.trim().isEmpty() 
                ? title 
                : "No content available";
            log.warn("⚠️ Post body is empty for post: {}, using title as fallback. Original body was: {}", 
                    post.getId(), post.getBody() != null ? "length=" + post.getBody().length() : "null");
        }
        
        log.info("✅ toPostResponse - Output: title='{}', title length={}, body length={}, lang={}", 
                title != null ? title.substring(0, Math.min(100, title.length())) : "null",
                title != null ? title.length() : 0,
                body != null ? body.length() : 0,
                lang);

        return new PostResponse(
                post.getId(),
                post.getCommunity() != null ? post.getCommunity().getId() : null,
                post.getAuthor() != null ? post.getAuthor().getId() : null,
                title,
                body,
                post.getTags(),
                post.getCategory(),
                post.getEmbedding(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getContentZh(),
                post.getContentEn(),
                post.getOriginalLanguage(),
                post.getImageUrl()
        );
    }

    private static class PostResponseWithScore {
        private final PostResponse response;
        private final double score;

        PostResponseWithScore(PostResponse response, double score) {
            this.response = response;
            this.score = score;
        }

        public double getScore() {
            return score;
        }

        public PostResponse toResponse() {
            return response;
        }
    }
}

