package com.globalbuddy.controller;

import com.globalbuddy.dto.PostListResponse;
import com.globalbuddy.dto.UserDTO;
import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.repository.CommentRepository;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.PostLikeRepository;
import com.globalbuddy.repository.UserFollowRepository;
import com.globalbuddy.model.UserFollow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * User Controller
 * Handles user profile and user-related operations
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository userRepository;
    private final CommunityPostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final PostController postController; // For converting posts to PostListResponse
    private final UserFollowRepository userFollowRepository;

    /**
     * Get current authenticated user
     */
    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return (AppUser) auth.getPrincipal();
    }

    /**
     * Get user profile by ID
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        
        try {
            Optional<AppUser> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            AppUser user = userOpt.get();
            AppUser currentUser = getCurrentUser();
            
            // Get user's posts (only approved posts)
            List<CommunityPost> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
            posts = posts.stream()
                    .filter(post -> post.getStatus() == CommunityPost.Status.APPROVED)
                    .collect(Collectors.toList());

            // Convert posts to PostListResponse
            List<PostListResponse> postResponses = posts.stream().map(post -> {
                long likeCount = postLikeRepository.countByPost(post);
                long commentCount = commentRepository.countByPost(post);
                
                // Use PostController's toPostResponse method via reflection or create a helper
                // For now, we'll create PostListResponse directly
                com.globalbuddy.dto.PostResponse postResponse = postController.toPostResponse(post, lang);
                
                return PostListResponse.builder()
                        .id(postResponse.getId())
                        .communityId(postResponse.getCommunityId())
                        .authorId(postResponse.getAuthorId())
                        .authorName(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername())
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
            }).collect(Collectors.toList());

            // Check if current user is following this user
            boolean isFollowing = false;
            if (currentUser != null && !currentUser.getId().equals(userId)) {
                isFollowing = userFollowRepository.existsByFollowerAndFollowing(currentUser, user);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", UserDTO.fromEntity(user));
            response.put("posts", postResponses);
            response.put("postCount", postResponses.size());
            response.put("isFollowing", isFollowing);
            response.put("isOwnProfile", currentUser != null && currentUser.getId().equals(userId));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get user profile: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get user profile: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get user's posts
     * GET /api/users/{userId}/posts
     */
    @GetMapping("/{userId}/posts")
    public ResponseEntity<List<PostListResponse>> getUserPosts(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "en") String lang,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        
        try {
            List<CommunityPost> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
            posts = posts.stream()
                    .filter(post -> post.getStatus() == CommunityPost.Status.APPROVED)
                    .collect(Collectors.toList());

            // Convert to PostListResponse
            List<PostListResponse> responses = posts.stream().map(post -> {
                long likeCount = postLikeRepository.countByPost(post);
                long commentCount = commentRepository.countByPost(post);
                com.globalbuddy.dto.PostResponse postResponse = postController.toPostResponse(post, lang);
                AppUser author = post.getAuthor();
                String authorName = author != null ? (author.getDisplayName() != null ? author.getDisplayName() : author.getUsername()) : "Unknown";
                
                return PostListResponse.builder()
                        .id(postResponse.getId())
                        .communityId(postResponse.getCommunityId())
                        .authorId(postResponse.getAuthorId())
                        .authorName(authorName)
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
            }).collect(Collectors.toList());

            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, responses.size());
            if (start >= responses.size()) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(responses.subList(start, end));

        } catch (Exception e) {
            log.error("Failed to get user posts: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get mutual follows list for current user
     * GET /api/users/mutual-follows
     */
    @GetMapping("/mutual-follows")
    public ResponseEntity<Map<String, Object>> getMutualFollows(
            @RequestParam(required = false) String q) {
        
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // Get users that current user is following
            List<UserFollow> followingList = userFollowRepository.findAll().stream()
                    .filter(follow -> follow.getFollower().getId().equals(currentUser.getId()))
                    .collect(java.util.stream.Collectors.toList());

            // Get users that are following current user
            List<UserFollow> followersList = userFollowRepository.findAll().stream()
                    .filter(follow -> follow.getFollowing().getId().equals(currentUser.getId()))
                    .collect(java.util.stream.Collectors.toList());

            // Find mutual follows (users that both follow current user and are followed by current user)
            List<Map<String, Object>> mutualFollows = new ArrayList<>();
            for (UserFollow following : followingList) {
                AppUser followedUser = following.getFollowing();
                // Check if this user also follows current user
                boolean isMutual = followersList.stream()
                        .anyMatch(f -> f.getFollower().getId().equals(followedUser.getId()));
                
                if (isMutual) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", followedUser.getId());
                    userInfo.put("username", followedUser.getUsername());
                    userInfo.put("displayName", followedUser.getDisplayName());
                    userInfo.put("email", followedUser.getEmail());
                    mutualFollows.add(userInfo);
                }
            }

            // Apply search filter if provided
            if (q != null && !q.trim().isEmpty()) {
                String query = q.toLowerCase().trim();
                mutualFollows = mutualFollows.stream()
                        .filter(user -> {
                            String username = ((String) user.get("username")).toLowerCase();
                            String displayName = user.get("displayName") != null 
                                    ? ((String) user.get("displayName")).toLowerCase() : "";
                            return username.contains(query) || displayName.contains(query);
                        })
                        .collect(java.util.stream.Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", mutualFollows);
            response.put("count", mutualFollows.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get mutual follows: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get mutual follows: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Unfollow a user
     * DELETE /api/users/{userId}/follow
     */
    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Map<String, Object>> unfollowUser(@PathVariable String userId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Optional<AppUser> targetUserOpt = userRepository.findById(userId);
            if (!targetUserOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            AppUser targetUser = targetUserOpt.get();
            Optional<UserFollow> followOpt = userFollowRepository.findByFollowerAndFollowing(currentUser, targetUser);
            
            if (followOpt.isPresent()) {
                userFollowRepository.delete(followOpt.get());
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Unfollowed successfully");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Not following this user");
                return ResponseEntity.badRequest().body(error);
            }

        } catch (Exception e) {
            log.error("Failed to unfollow user: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to unfollow user: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get current user's own posts with all statuses (for profile page)
     * GET /api/users/me/posts
     * Returns all posts including PENDING_REVIEW, APPROVED, and REJECTED
     */
    @GetMapping("/me/posts")
    public ResponseEntity<Map<String, Object>> getMyPosts(
            @RequestParam(required = false, defaultValue = "en") String lang) {
        
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            // Get all posts by current user (including all statuses)
            List<CommunityPost> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(currentUser.getId());

            // Convert to response format with moderation status
            List<Map<String, Object>> postResponses = posts.stream().map(post -> {
                long likeCount = postLikeRepository.countByPost(post);
                long commentCount = commentRepository.countByPost(post);
                com.globalbuddy.dto.PostResponse postResponse = postController.toPostResponse(post, lang);
                
                Map<String, Object> postMap = new HashMap<>();
                postMap.put("id", post.getId());
                postMap.put("title", postResponse.getTitle());
                postMap.put("body", postResponse.getBody());
                postMap.put("tags", postResponse.getTags());
                postMap.put("category", postResponse.getCategory());
                postMap.put("createdAt", post.getCreatedAt());
                postMap.put("updatedAt", post.getUpdatedAt());
                postMap.put("imageUrl", postResponse.getImageUrl());
                postMap.put("likeCount", likeCount);
                postMap.put("commentCount", commentCount);
                
                // Add moderation status information
                postMap.put("status", post.getStatus().name());
                postMap.put("reviewNote", post.getReviewNote());
                postMap.put("reviewedAt", post.getReviewedAt());
                postMap.put("reviewedBy", post.getReviewedBy());
                
                return postMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", postResponses);
            response.put("count", postResponses.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get my posts: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get my posts: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get current user's profile
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyProfile() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", UserDTO.fromEntity(currentUser));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get my profile: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get my profile: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Update current user's profile
     * PUT /api/users/me
     * Allows updating: displayName, avatar, preferredLanguage
     */
    @PutMapping("/me")
    public ResponseEntity<Map<String, Object>> updateMyProfile(@RequestBody Map<String, Object> updates) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            boolean updated = false;

            // Update display name
            if (updates.containsKey("displayName")) {
                String displayName = (String) updates.get("displayName");
                if (displayName != null && !displayName.trim().isEmpty()) {
                    currentUser.setDisplayName(displayName.trim());
                    updated = true;
                    log.info("Updated display name for user: {}", currentUser.getId());
                }
            }

            // Update avatar
            if (updates.containsKey("avatar")) {
                String avatar = (String) updates.get("avatar");
                currentUser.setAvatar(avatar); // Can be null to remove avatar
                updated = true;
                log.info("Updated avatar for user: {}", currentUser.getId());
            }

            // Update preferred language
            if (updates.containsKey("preferredLanguage")) {
                String preferredLanguage = (String) updates.get("preferredLanguage");
                if (preferredLanguage != null && (preferredLanguage.equals("zh") || preferredLanguage.equals("en"))) {
                    currentUser.setPreferredLanguage(preferredLanguage);
                    updated = true;
                    log.info("Updated preferred language for user: {} to {}", currentUser.getId(), preferredLanguage);
                }
            }

            if (updated) {
                userRepository.save(currentUser);
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Profile updated successfully");
                response.put("data", UserDTO.fromEntity(currentUser));
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No valid fields to update");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Failed to update profile: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}

