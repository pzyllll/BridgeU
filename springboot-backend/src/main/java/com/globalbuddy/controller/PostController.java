package com.globalbuddy.controller;

import com.globalbuddy.dto.*;
import com.globalbuddy.model.*;
import com.globalbuddy.repository.*;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    @Value("${file.upload.base-path:C:/Users/pzy/Documents/java/work/hh/pictures}")
    private String uploadBasePath;

    @GetMapping
    public List<PostResponse> listPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        log.info("Fetching posts with language preference: {}", lang);
        List<CommunityPost> posts = postRepository.findAllByOrderByCreatedAtDesc();
        
        // Don't filter by original language - show all posts that have translations
        // The toPostResponse method will handle language selection based on lang parameter
        // Only filter out posts that have no content at all
        posts = posts.stream()
                .filter(post -> {
                    // Keep posts that have original content or translations
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
        List<PostResponse> responses = new ArrayList<>();
        int zhTranslatedCount = 0;
        int enTranslatedCount = 0;
        for (CommunityPost post : posts) {
            PostResponse response = toPostResponse(post, lang);
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
        if (!StringUtils.hasText(q)) {
            return responses;
        }
        List<PostResponseWithScore> filtered = new ArrayList<>();
        for (PostResponse response : responses) {
            double score = semanticService.calculateScore(q, response.getTitle() + " " + response.getBody());
            if (score > 0) {
                filtered.add(new PostResponseWithScore(response, score));
            }
        }
        filtered.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return filtered.stream().map(PostResponseWithScore::toResponse).collect(Collectors.toList());
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
        
        // Get comments with language preference
        List<Comment> comments = commentRepository.findByPostOrderByCreatedAtDesc(post);
        List<CommentResponse> commentResponses = comments.stream()
                .map(comment -> toCommentResponse(comment, lang))
                .collect(Collectors.toList());
        
        PostDetailResponse response = PostDetailResponse.builder()
                .post(postResponse)
                .authorName(authorName)
                .authorDisplayName(authorDisplayName)
                .authorId(author != null ? author.getId() : null)
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
            } else if ("zh".equals(detectedLang)) {
                post.setTitleZh(request.getTitle());
            }
            
            // Set Chinese content translation (with fallback to original if translation fails)
            if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                post.setContentZh(translationResult.getBodyZh());
                log.info("✅ Chinese translation completed for post: {}", request.getTitle());
            } else {
                // Fallback: if detected language is Chinese, use original; otherwise use original as fallback
                if ("zh".equals(detectedLang)) {
                    post.setContentZh(request.getBody());
                } else {
                    post.setContentZh(request.getBody()); // Fallback to original
                    log.warn("⚠️ Chinese translation failed, using original content as fallback");
                }
            }

            // Set English title translation
            if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                post.setTitleEn(translationResult.getTitleEn());
            } else if ("en".equals(detectedLang)) {
                post.setTitleEn(request.getTitle());
            }

            // Set English content translation (with fallback to original if translation fails)
            if (translationResult.getBodyEn() != null && !translationResult.getBodyEn().isEmpty()) {
                post.setContentEn(translationResult.getBodyEn());
                log.info("✅ English translation completed for post: {}", request.getTitle());
            } else {
                // Fallback: if detected language is English, use original; otherwise use original as fallback
                if ("en".equals(detectedLang)) {
                    post.setContentEn(request.getBody());
                } else {
                    post.setContentEn(request.getBody()); // Fallback to original
                    log.warn("⚠️ English translation failed, using original content as fallback");
                }
            }
        } catch (Exception e) {
            log.error("❌ Translation failed for post: {} - {}", request.getTitle(), e.getMessage(), e);
            // Fallback: use original content for both languages
            post.setContentZh(request.getBody());
            post.setContentEn(request.getBody());
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
     * Upload post image
     */
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "文件不能为空"));
        }
        try {
            String sanitized = sanitizeFileName(file.getOriginalFilename());
            String newName = UUID.randomUUID() + "_" + sanitized;
            Path dir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(newName);
            file.transferTo(target.toFile());
            // 返回可供前端使用的相对路径（需有静态映射）
            String url = "/pictures/" + newName;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("上传图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    private String sanitizeFileName(String original) {
        if (!StringUtils.hasText(original)) {
            return "image";
        }
        return original.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }

    private PostResponse toPostResponse(CommunityPost post, String lang) {
        String title = post.getTitle();
        String body = post.getBody();
        
        log.info("🔄 toPostResponse - Input: postId={}, lang={}, original title={}, titleZh={}, titleEn={}, original body length={}, contentZh length={}, contentEn length={}", 
                post.getId(), 
                lang,
                post.getTitle() != null ? post.getTitle().substring(0, Math.min(50, post.getTitle().length())) : "null",
                post.getTitleZh() != null ? (post.getTitleZh().isEmpty() ? "EMPTY" : post.getTitleZh().substring(0, Math.min(50, post.getTitleZh().length()))) : "NULL",
                post.getTitleEn() != null ? (post.getTitleEn().isEmpty() ? "EMPTY" : post.getTitleEn().substring(0, Math.min(50, post.getTitleEn().length()))) : "NULL",
                body != null ? body.length() : 0,
                post.getContentZh() != null ? post.getContentZh().length() : 0,
                post.getContentEn() != null ? post.getContentEn().length() : 0);

        // Return translated title and content based on language preference
        if ("zh".equals(lang)) {
            // Use Chinese translation if available
            if (post.getTitleZh() != null && !post.getTitleZh().isEmpty()) {
                String originalTitle = title;
                title = post.getTitleZh();
                log.info("✅ Using Chinese title translation for post {}: original='{}' -> translated='{}'", 
                        post.getId(), 
                        originalTitle != null ? originalTitle.substring(0, Math.min(100, originalTitle.length())) : "null",
                        title.substring(0, Math.min(100, title.length())));
            } else {
                log.warn("⚠️ Chinese title translation not available for post: {} (original: {}), titleZh is null or empty, using original", 
                        post.getId(), 
                        post.getTitle() != null ? post.getTitle().substring(0, Math.min(100, post.getTitle().length())) : "null");
            }
            if (post.getContentZh() != null && !post.getContentZh().isEmpty()) {
                body = post.getContentZh();
                log.debug("Using Chinese content translation for post: {} (length: {})", post.getId(), body.length());
            } else {
                // Fallback to original body if translation not available
                body = post.getBody();
                log.warn("Chinese content translation not available for post: {} (original length: {}), using original body", post.getId(), post.getBody() != null ? post.getBody().length() : 0);
            }
        } else if ("en".equals(lang)) {
            // Use English translation if available
            if (post.getTitleEn() != null && !post.getTitleEn().isEmpty()) {
                title = post.getTitleEn();
                log.info("✅ Using English title translation for post {}: {} -> {}", 
                        post.getId(),
                        post.getTitle() != null ? post.getTitle().substring(0, Math.min(50, post.getTitle().length())) : "null",
                        title.substring(0, Math.min(50, title.length())));
            } else {
                log.warn("⚠️ English title translation not available for post: {} (original: {}), using original", 
                        post.getId(), 
                        post.getTitle() != null ? post.getTitle().substring(0, Math.min(100, post.getTitle().length())) : "null");
            }
            if (post.getContentEn() != null && !post.getContentEn().isEmpty()) {
                body = post.getContentEn();
                log.debug("Using English content translation for post: {} (length: {})", post.getId(), body.length());
            } else {
                // Fallback to original body if translation not available
                body = post.getBody();
                log.warn("English content translation not available for post: {} (original length: {}), using original body", post.getId(), post.getBody() != null ? post.getBody().length() : 0);
            }
        } else {
            // Fallback to original content
            log.debug("Using original content for post: {} (lang: {})", post.getId(), lang);
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

