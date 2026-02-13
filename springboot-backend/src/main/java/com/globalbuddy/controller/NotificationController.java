package com.globalbuddy.controller;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Notification;
import com.globalbuddy.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification Controller
 * Handles user notifications
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

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
     * Get all notifications for current user
     * GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Notification> notifications;
        if (unreadOnly) {
            notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUser.getId());
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        }

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        long count = notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark notification as read
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable Long id) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Notification not found");
            return ResponseEntity.notFound().build();
        }

        // Verify ownership
        if (!notification.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        notificationRepository.markAsRead(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * Mark all notifications as read for current user
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        notificationRepository.markAllAsRead(currentUser.getId());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All notifications marked as read");
        return ResponseEntity.ok(response);
    }
}

