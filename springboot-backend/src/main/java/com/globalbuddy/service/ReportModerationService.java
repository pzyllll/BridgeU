package com.globalbuddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalbuddy.model.*;
import com.globalbuddy.repository.CommentRepository;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Report Moderation Service
 * Handles AI-based moderation of reported content
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportModerationService {

    private final ReportRepository reportRepository;
    private final CommunityPostRepository postRepository;
    private final CommentRepository commentRepository;
    private final QwenService qwenService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process a report asynchronously using AI moderation
     */
    @Async
    @Transactional
    public void processReport(Long reportId) {
        try {
            Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));
            
            log.info("Processing report: id={}, targetType={}, targetId={}", 
                reportId, report.getTargetType(), report.getTargetId());

            // Get the reported content
            String content = null;
            String title = null;
            
            if (report.getTargetType() == Report.TargetType.POST) {
                CommunityPost post = postRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Post not found: " + report.getTargetId()));
                content = post.getBody();
                title = post.getTitle();
            } else {
                Comment comment = commentRepository.findById(report.getTargetId())
                    .orElseThrow(() -> new RuntimeException("Comment not found: " + report.getTargetId()));
                content = comment.getContent();
            }

            // Build AI prompt for report review
            String prompt = buildModerationPrompt(content, title, report.getReasons(), report.getDescription());
            
            // Call AI for moderation
            String aiResponse = qwenService.answerQuestion(prompt, "");
            
            // Parse AI response
            ModerationResult result = parseAiResponse(aiResponse);
            
            // Update report with AI results
            report.setAiResult(aiResponse);
            report.setAiConfidence(result.confidence);
            report.setIsViolation(!result.isSafe);
            report.setViolationSnippet(result.violationSnippet);
            report.setReviewedAt(new Date());
            report.setStatus(result.isSafe ? Report.Status.DISMISSED : Report.Status.REVIEWED);
            reportRepository.save(report);

            // Handle based on result
            if (result.isSafe) {
                // Content is compliant - notify reporter only
                notificationService.notifyReporterFailed(
                    report.getReporter(),
                    reportId,
                    report.getTargetType().name(),
                    report.getTargetId(),
                    result.reason
                );
            } else {
                // Content violates guidelines - take action
                handleViolation(report, result);
            }

            log.info("Report processed: id={}, isViolation={}, confidence={}", 
                reportId, !result.isSafe, result.confidence);

        } catch (Exception e) {
            log.error("Failed to process report: id={}", reportId, e);
            // Mark report as reviewed with error
            try {
                Report report = reportRepository.findById(reportId).orElse(null);
                if (report != null) {
                    report.setStatus(Report.Status.REVIEWED);
                    report.setReviewNotes("Processing error: " + e.getMessage());
                    reportRepository.save(report);
                }
            } catch (Exception ex) {
                log.error("Failed to update report status", ex);
            }
        }
    }

    /**
     * Build AI prompt for report moderation
     */
    private String buildModerationPrompt(String content, String title, List<String> reasons, String description) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a content moderation AI. A user has reported content for the following reasons: ");
        prompt.append(String.join(", ", reasons));
        if (description != null && !description.isEmpty()) {
            prompt.append("\n\nReporter's description: ").append(description);
        }
        prompt.append("\n\nPlease review the reported content and determine if it violates community guidelines.");
        prompt.append("\n\nRespond ONLY in JSON format with the following fields:");
        prompt.append("\n- is_safe: boolean (true if content is compliant, false if it violates guidelines)");
        prompt.append("\n- confidence_score: number 0-100 (your confidence in the judgment)");
        prompt.append("\n- reason: string (brief explanation of your judgment)");
        prompt.append("\n- violation_snippet: string (if is_safe is false, extract the specific part that violates guidelines, otherwise empty string)");
        prompt.append("\n- guideline_violated: string (if is_safe is false, specify which guideline was violated, e.g., 'Spam', 'Fraud or Scam', 'Illegal Service Promotion', 'Abusive Language')");
        
        prompt.append("\n\nJSON example:");
        prompt.append("\n{\"is_safe\": false, \"confidence_score\": 85, \"reason\": \"Contains spam content\", \"violation_snippet\": \"Buy cheap products now!\", \"guideline_violated\": \"Spam\"}");
        
        if (title != null) {
            prompt.append("\n\nTitle: ").append(title);
        }
        prompt.append("\n\nContent: ").append(content);
        
        return prompt.toString();
    }

    /**
     * Parse AI response JSON
     */
    private ModerationResult parseAiResponse(String aiResponse) {
        try {
            // Clean AI response (remove markdown code blocks if present)
            String cleaned = aiResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z0-9]*", "");
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.lastIndexOf("```"));
            }
            cleaned = cleaned.trim();
            
            // Extract JSON from response
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1);
            }

            JsonNode node = objectMapper.readTree(cleaned);
            boolean isSafe = node.path("is_safe").asBoolean(true);
            double confidence = node.path("confidence_score").asDouble(0);
            String reason = node.path("reason").asText("No reason provided");
            String violationSnippet = node.path("violation_snippet").asText("");
            String guidelineViolated = node.path("guideline_violated").asText("");

            return new ModerationResult(isSafe, confidence, reason, violationSnippet, guidelineViolated);
        } catch (Exception e) {
            log.warn("Failed to parse AI response, defaulting to safe: {}", e.getMessage());
            return new ModerationResult(true, 0, "AI response parsing failed", "", "");
        }
    }

    /**
     * Handle content violation - remove/collapse content and send notifications
     */
    private void handleViolation(Report report, ModerationResult result) {
        if (report.getTargetType() == Report.TargetType.POST) {
            handlePostViolation(report, result);
        } else {
            handleCommentViolation(report, result);
        }
    }

    /**
     * Handle post violation
     */
    private void handlePostViolation(Report report, ModerationResult result) {
        CommunityPost post = postRepository.findById(report.getTargetId())
            .orElseThrow(() -> new RuntimeException("Post not found: " + report.getTargetId()));

        // Mark post as removed due to report
        post.setStatus(CommunityPost.Status.REPORTED_REMOVED);
        postRepository.save(post);

        // Determine penalty action
        String penaltyAction = "帖子已被下架/折叠 | Post has been removed/collapsed";
        
        // Notify reporter
        notificationService.notifyReporterSuccess(
            report.getReporter(),
            report.getId(),
            "POST",
            report.getTargetId()
        );

        // Notify post author
        notificationService.notifyPostAuthorPenalty(
            post.getAuthor(),
            report.getId(),
            report.getTargetId(),
            result.violationSnippet,
            penaltyAction
        );

        log.info("Post violation handled: postId={}, reportId={}", report.getTargetId(), report.getId());
    }

    /**
     * Handle comment violation
     */
    private void handleCommentViolation(Report report, ModerationResult result) {
        Comment comment = commentRepository.findById(report.getTargetId())
            .orElseThrow(() -> new RuntimeException("Comment not found: " + report.getTargetId()));

        // Mark comment as removed due to report
        comment.setStatus(Comment.Status.REPORTED_REMOVED);
        commentRepository.save(comment);

        // Determine penalty action
        String penaltyAction = "评论已被删除 | Comment has been deleted";
        
        // Notify reporter
        notificationService.notifyReporterSuccess(
            report.getReporter(),
            report.getId(),
            "COMMENT",
            report.getTargetId()
        );

        // Notify comment author
        notificationService.notifyCommentAuthorPenalty(
            comment.getAuthor(),
            report.getId(),
            report.getTargetId(),
            result.violationSnippet,
            penaltyAction
        );

        log.info("Comment violation handled: commentId={}, reportId={}", report.getTargetId(), report.getId());
    }

    /**
     * Restore content after false positive (can be called manually or by AI re-review)
     */
    @Transactional
    public void restoreContent(Report report) {
        if (report.getTargetType() == Report.TargetType.POST) {
            CommunityPost post = postRepository.findById(report.getTargetId()).orElse(null);
            if (post != null && post.getStatus() == CommunityPost.Status.REPORTED_REMOVED) {
                post.setStatus(CommunityPost.Status.APPROVED);
                postRepository.save(post);
                notificationService.notifyPostAuthorRestored(post.getAuthor(), report.getTargetId());
                log.info("Post restored: postId={}, reportId={}", report.getTargetId(), report.getId());
            }
        } else {
            Comment comment = commentRepository.findById(report.getTargetId()).orElse(null);
            if (comment != null && comment.getStatus() == Comment.Status.REPORTED_REMOVED) {
                comment.setStatus(Comment.Status.ACTIVE);
                commentRepository.save(comment);
                notificationService.notifyCommentAuthorRestored(comment.getAuthor(), report.getTargetId());
                log.info("Comment restored: commentId={}, reportId={}", report.getTargetId(), report.getId());
            }
        }
    }

    /**
     * Moderation result data class
     */
    private record ModerationResult(
        boolean isSafe,
        double confidence,
        String reason,
        String violationSnippet,
        String guidelineViolated
    ) {}
}

