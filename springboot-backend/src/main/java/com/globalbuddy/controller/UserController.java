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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

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
    
    @Value("${file.upload.base-path:C:/Users/pzy/Documents/java/work/hh/pictures}")
    private String uploadBasePath;

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

            // Get followers count (users who follow this user)
            long followersCount = userFollowRepository.countByFollowing(user);
            
            // Get mutual follows count (users who both follow this user and are followed by this user)
            long mutualFollowsCount = 0;
            List<UserFollow> userFollowingList = userFollowRepository.findAll().stream()
                    .filter(follow -> follow.getFollower().getId().equals(user.getId()))
                    .collect(Collectors.toList());
            List<UserFollow> userFollowersList = userFollowRepository.findAll().stream()
                    .filter(follow -> follow.getFollowing().getId().equals(user.getId()))
                    .collect(Collectors.toList());
            
            for (UserFollow following : userFollowingList) {
                AppUser followedUser = following.getFollowing();
                boolean isMutual = userFollowersList.stream()
                        .anyMatch(f -> f.getFollower().getId().equals(followedUser.getId()));
                if (isMutual) {
                    mutualFollowsCount++;
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", UserDTO.fromEntity(user));
            response.put("posts", postResponses);
            response.put("postCount", postResponses.size());
            response.put("isFollowing", isFollowing);
            response.put("isOwnProfile", currentUser != null && currentUser.getId().equals(userId));
            response.put("followersCount", followersCount);
            response.put("mutualFollowsCount", mutualFollowsCount);

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
     * Get followers list for a user
     * GET /api/users/{userId}/followers
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<Map<String, Object>> getFollowers(@PathVariable String userId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Optional<AppUser> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            AppUser user = userOpt.get();

            // Get users who follow this user
            List<UserFollow> followersList = userFollowRepository.findAll().stream()
                    .filter(follow -> follow.getFollowing().getId().equals(user.getId()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> followers = new ArrayList<>();
            for (UserFollow follow : followersList) {
                AppUser follower = follow.getFollower();
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", follower.getId());
                userInfo.put("username", follower.getUsername());
                userInfo.put("displayName", follower.getDisplayName());
                userInfo.put("avatar", follower.getAvatar());
                // Check if current user is following this follower
                boolean isFollowing = userFollowRepository.existsByFollowerAndFollowing(currentUser, follower);
                userInfo.put("isFollowing", isFollowing);
                followers.add(userInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", followers);
            response.put("count", followers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get followers: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get followers: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get mutual follows list for a user
     * GET /api/users/{userId}/mutual-follows
     */
    @GetMapping("/{userId}/mutual-follows")
    public ResponseEntity<Map<String, Object>> getUserMutualFollows(@PathVariable String userId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Optional<AppUser> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            AppUser user = userOpt.get();

            // Get users that this user is following
            List<UserFollow> followingList = userFollowRepository.findAll().stream()
                    .filter(follow -> follow.getFollower().getId().equals(user.getId()))
                    .collect(Collectors.toList());

            // Get users that are following this user
            List<UserFollow> followersList = userFollowRepository.findAll().stream()
                    .filter(follow -> follow.getFollowing().getId().equals(user.getId()))
                    .collect(Collectors.toList());

            // Find mutual follows (users that both follow this user and are followed by this user)
            List<Map<String, Object>> mutualFollows = new ArrayList<>();
            for (UserFollow following : followingList) {
                AppUser followedUser = following.getFollowing();
                // Check if this user also follows the target user
                boolean isMutual = followersList.stream()
                        .anyMatch(f -> f.getFollower().getId().equals(followedUser.getId()));
                
                if (isMutual) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", followedUser.getId());
                    userInfo.put("username", followedUser.getUsername());
                    userInfo.put("displayName", followedUser.getDisplayName());
                    userInfo.put("avatar", followedUser.getAvatar());
                    // Check if current user is following this mutual follow
                    boolean isFollowing = userFollowRepository.existsByFollowerAndFollowing(currentUser, followedUser);
                    userInfo.put("isFollowing", isFollowing);
                    mutualFollows.add(userInfo);
                }
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
     * Search users by username or display name
     * GET /api/users/search?q=keyword
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            if (q == null || q.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Search query is required");
                return ResponseEntity.badRequest().body(error);
            }

            String query = q.trim();
            List<AppUser> users = userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query);
            
            // Exclude current user from results
            users = users.stream()
                    .filter(user -> !user.getId().equals(currentUser.getId()))
                    .limit(limit)
                    .collect(Collectors.toList());

            // Convert to response format with follow status
            List<Map<String, Object>> userList = users.stream().map(user -> {
                boolean isFollowing = userFollowRepository.existsByFollowerAndFollowing(currentUser, user);
                
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("displayName", user.getDisplayName());
                userInfo.put("avatar", user.getAvatar());
                userInfo.put("isFollowing", isFollowing);
                return userInfo;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", userList);
            response.put("count", userList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to search users: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to search users: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Follow a user
     * POST /api/users/{userId}/follow
     */
    @PostMapping("/{userId}/follow")
    public ResponseEntity<Map<String, Object>> followUser(@PathVariable String userId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            if (currentUser.getId().equals(userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Cannot follow yourself");
                return ResponseEntity.badRequest().body(error);
            }

            Optional<AppUser> targetUserOpt = userRepository.findById(userId);
            if (!targetUserOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            AppUser targetUser = targetUserOpt.get();
            
            // Check if already following
            if (userFollowRepository.existsByFollowerAndFollowing(currentUser, targetUser)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Already following this user");
                return ResponseEntity.badRequest().body(error);
            }

            // Create follow relationship
            UserFollow follow = UserFollow.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .build();
            userFollowRepository.save(follow);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Followed successfully");
            response.put("following", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to follow user: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to follow user: " + e.getMessage());
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
                response.put("following", false);
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

    /**
     * Upload avatar image
     * POST /api/users/me/avatar
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadAvatar(@RequestPart("file") MultipartFile file) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("Received avatar upload request for user: {}", currentUser.getId());
        
        if (file == null || file.isEmpty()) {
            log.warn("Avatar upload failed: File is empty");
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文件不能为空");
            return ResponseEntity.badRequest().body(error);
        }
        
        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(fileExtension)) {
            log.warn("Avatar upload failed: Invalid file extension: {}", fileExtension);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "不支持的文件类型，仅支持 JPG, PNG, GIF, WEBP");
            return ResponseEntity.badRequest().body(error);
        }
        
        // 验证文件大小（限制为 5MB）
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            log.warn("Avatar upload failed: File too large: {} bytes", file.getSize());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文件大小不能超过 5MB");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            // 准备目录和文件名
            String sanitized = sanitizeFileName(originalFilename);
            String newName = "avatar_" + currentUser.getId() + "_" + UUID.randomUUID() + "." + fileExtension;
            Path dir = Paths.get(uploadBasePath).toAbsolutePath().normalize();
            
            // 确保目录存在
            if (!Files.exists(dir)) {
                log.warn("Upload directory does not exist, creating: {}", dir);
                Files.createDirectories(dir);
            }
            
            // 检查目录是否可写
            if (!Files.isWritable(dir)) {
                log.error("Upload directory is not writable: {}", dir);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "服务器存储目录不可写");
                return ResponseEntity.status(500).body(error);
            }
            
            // 保存文件
            Path target = dir.resolve(newName);
            log.info("Saving avatar to: {}", target);
            file.transferTo(target.toFile());
            
            // 验证文件是否保存成功
            if (!Files.exists(target) || Files.size(target) == 0) {
                log.error("Failed to save avatar: {}", target);
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "文件保存失败");
                return ResponseEntity.status(500).body(error);
            }
            
            // 更新用户头像URL
            String avatarUrl = "/pictures/" + newName;
            currentUser.setAvatar(avatarUrl);
            userRepository.save(currentUser);
            
            log.info("Avatar uploaded successfully: {}", avatarUrl);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "头像上传成功");
            response.put("url", avatarUrl);
            response.put("data", UserDTO.fromEntity(currentUser));
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Failed to upload avatar: " + originalFilename, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文件上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        } catch (Exception e) {
            log.error("Unexpected error during avatar upload", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Sanitize file name to prevent security issues
     */
    private String sanitizeFileName(String original) {
        if (!StringUtils.hasText(original)) {
            return "avatar";
        }
        return original.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }
}

