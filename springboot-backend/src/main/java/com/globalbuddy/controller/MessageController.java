package com.globalbuddy.controller;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Conversation;
import com.globalbuddy.model.Message;
import com.globalbuddy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Message Controller
 * Handles private messaging between users
 */
@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AppUserRepository userRepository;
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
     * Get all conversations for current user
     * GET /api/messages/conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations() {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // Get conversations where user is user1 or user2, and not deleted by them
            List<Conversation> conversations1 = conversationRepository.findByUser1OrderByLastMessageAtDesc(currentUser);
            List<Conversation> conversations2 = conversationRepository.findByUser2OrderByLastMessageAtDesc(currentUser);
            
            // Filter out conversations deleted by current user
            conversations1 = conversations1.stream()
                    .filter(conv -> conv.getDeletedBy() == null || !conv.getDeletedBy().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());
            conversations2 = conversations2.stream()
                    .filter(conv -> conv.getDeletedBy() == null || !conv.getDeletedBy().getId().equals(currentUser.getId()))
                    .collect(Collectors.toList());

            // Combine and deduplicate
            Set<Conversation> allConversations = new HashSet<>();
            allConversations.addAll(conversations1);
            allConversations.addAll(conversations2);

            // Sort by last message time
            List<Map<String, Object>> conversationList = allConversations.stream()
                    .sorted((a, b) -> {
                        Instant timeA = a.getLastMessageAt() != null ? a.getLastMessageAt() : a.getCreatedAt();
                        Instant timeB = b.getLastMessageAt() != null ? b.getLastMessageAt() : b.getCreatedAt();
                        return timeB.compareTo(timeA); // Descending order
                    })
                    .map(conv -> {
                        AppUser otherUser = conv.getUser1().getId().equals(currentUser.getId()) 
                                ? conv.getUser2() : conv.getUser1();
                        
                        // Get unread count
                        long unreadCount = messageRepository.countByConversationAndIsReadFalseAndReceiver(conv, currentUser);
                        
                        // Get last message
                        List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conv);
                        Message lastMessage = messages.isEmpty() ? null : messages.get(messages.size() - 1);
                        
                        Map<String, Object> convData = new HashMap<>();
                        convData.put("id", conv.getId());
                        convData.put("otherUser", Map.of(
                                "id", otherUser.getId(),
                                "username", otherUser.getUsername(),
                                "displayName", otherUser.getDisplayName()
                        ));
                        convData.put("unreadCount", unreadCount);
                        convData.put("lastMessage", lastMessage != null ? Map.of(
                                "content", lastMessage.getContent(),
                                "createdAt", lastMessage.getCreatedAt(),
                                "senderId", lastMessage.getSender().getId()
                        ) : null);
                        convData.put("lastMessageAt", conv.getLastMessageAt());
                        convData.put("createdAt", conv.getCreatedAt());
                        return convData;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", conversationList);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get conversations: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get conversations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get or create conversation with a user
     * POST /api/messages/conversations
     */
    @PostMapping("/conversations")
    public ResponseEntity<Map<String, Object>> createOrGetConversation(@RequestBody Map<String, String> request) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String otherUserId = request.get("userId");
        if (otherUserId == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "userId is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Optional<AppUser> otherUserOpt = userRepository.findById(otherUserId);
            if (!otherUserOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "User not found");
                return ResponseEntity.notFound().build();
            }

            AppUser otherUser = otherUserOpt.get();

            // Check if users are mutual follows
            boolean currentFollowsOther = userFollowRepository.existsByFollowerAndFollowing(currentUser, otherUser);
            boolean otherFollowsCurrent = userFollowRepository.existsByFollowerAndFollowing(otherUser, currentUser);
            
            if (!currentFollowsOther || !otherFollowsCurrent) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "You can only message users you mutually follow");
                return ResponseEntity.badRequest().body(error);
            }

            // Find existing conversation
            Optional<Conversation> existingConv1 = conversationRepository.findByUser1AndUser2(currentUser, otherUser);
            Optional<Conversation> existingConv2 = conversationRepository.findByUser2AndUser1(currentUser, otherUser);
            
            Conversation conversation = existingConv1.orElse(existingConv2.orElse(null));
            
            if (conversation == null) {
                // Create new conversation
                conversation = Conversation.builder()
                        .user1(currentUser)
                        .user2(otherUser)
                        .createdAt(Instant.now())
                        .build();
                conversation = conversationRepository.save(conversation);
            } else if (conversation.getDeletedBy() != null && conversation.getDeletedBy().getId().equals(currentUser.getId())) {
                // Restore deleted conversation
                conversation.setDeletedBy(null);
                conversation = conversationRepository.save(conversation);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversationId", conversation.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to create/get conversation: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to create/get conversation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get messages in a conversation
     * GET /api/messages/conversations/{conversationId}
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, Object>> getConversationMessages(@PathVariable String conversationId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Conversation not found");
                return ResponseEntity.notFound().build();
            }

            Conversation conversation = convOpt.get();
            
            // Verify user is part of conversation
            if (!conversation.getUser1().getId().equals(currentUser.getId()) 
                    && !conversation.getUser2().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get messages
            List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
            
            List<Map<String, Object>> messageList = messages.stream().map(msg -> {
                Map<String, Object> msgData = new HashMap<>();
                msgData.put("id", msg.getId());
                msgData.put("content", msg.getContent());
                msgData.put("senderId", msg.getSender().getId());
                msgData.put("receiverId", msg.getReceiver().getId());
                msgData.put("isRead", msg.getIsRead());
                msgData.put("readAt", msg.getReadAt());
                msgData.put("createdAt", msg.getCreatedAt());
                return msgData;
            }).collect(Collectors.toList());

            AppUser otherUser = conversation.getUser1().getId().equals(currentUser.getId()) 
                    ? conversation.getUser2() : conversation.getUser1();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversationId", conversation.getId());
            response.put("otherUser", Map.of(
                    "id", otherUser.getId(),
                    "username", otherUser.getUsername(),
                    "displayName", otherUser.getDisplayName()
            ));
            response.put("messages", messageList);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get conversation messages: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to get conversation messages: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Send a message in a conversation
     * POST /api/messages/conversations/{conversationId}/messages
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> request) {
        
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Message content is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Conversation not found");
                return ResponseEntity.notFound().build();
            }

            Conversation conversation = convOpt.get();
            
            // Verify user is part of conversation
            if (!conversation.getUser1().getId().equals(currentUser.getId()) 
                    && !conversation.getUser2().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            AppUser receiver = conversation.getUser1().getId().equals(currentUser.getId()) 
                    ? conversation.getUser2() : conversation.getUser1();

            // Create message
            Message message = Message.builder()
                    .conversation(conversation)
                    .sender(currentUser)
                    .receiver(receiver)
                    .content(content.trim())
                    .isRead(false)
                    .createdAt(Instant.now())
                    .build();
            message = messageRepository.save(message);

            // Update conversation last message time
            conversation.setLastMessageAt(Instant.now());
            conversationRepository.save(conversation);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messageId", message.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to send message: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to send message: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Mark messages as read
     * PUT /api/messages/{messageId}/read
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable String messageId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<Message> msgOpt = messageRepository.findById(messageId);
            if (!msgOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Message not found");
                return ResponseEntity.notFound().build();
            }

            Message message = msgOpt.get();
            
            // Verify user is the receiver
            if (!message.getReceiver().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            message.setIsRead(true);
            message.setReadAt(Instant.now());
            messageRepository.save(message);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to mark message as read: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to mark message as read: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Mark all messages in a conversation as read
     * PUT /api/messages/conversations/{conversationId}/read
     */
    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markConversationAsRead(@PathVariable String conversationId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Conversation not found");
                return ResponseEntity.notFound().build();
            }

            Conversation conversation = convOpt.get();
            
            // Verify user is part of conversation
            if (!conversation.getUser1().getId().equals(currentUser.getId()) 
                    && !conversation.getUser2().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Mark all unread messages as read
            List<Message> messages = messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
            Instant now = Instant.now();
            for (Message msg : messages) {
                if (!msg.getIsRead() && msg.getReceiver().getId().equals(currentUser.getId())) {
                    msg.setIsRead(true);
                    msg.setReadAt(now);
                }
            }
            messageRepository.saveAll(messages);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to mark conversation as read: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to mark conversation as read: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete a conversation (soft delete)
     * DELETE /api/messages/conversations/{conversationId}
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable String conversationId) {
        AppUser currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Optional<Conversation> convOpt = conversationRepository.findById(conversationId);
            if (!convOpt.isPresent()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Conversation not found");
                return ResponseEntity.notFound().build();
            }

            Conversation conversation = convOpt.get();
            
            // Verify user is part of conversation
            if (!conversation.getUser1().getId().equals(currentUser.getId()) 
                    && !conversation.getUser2().getId().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Soft delete - mark as deleted by current user
            conversation.setDeletedBy(currentUser);
            conversationRepository.save(conversation);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Conversation deleted successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete conversation: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to delete conversation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

