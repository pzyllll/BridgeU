package com.globalbuddy.service;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Notification;
import com.globalbuddy.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Notification Service
 * Handles creating and managing notifications for users
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Send notification to reporter when report is successful
     */
    @Transactional
    public void notifyReporterSuccess(AppUser reporter, Long reportId, String targetType, String targetId) {
        String titleZh = "举报成功 | Report Successful";
        String titleEn = "Report Successful | 举报成功";
        String contentZh = String.format(
            "感谢您的反馈！您举报的%s已被下架/折叠。您的贡献让社区环境变得更好。",
            targetType.equals("POST") ? "帖子" : "评论"
        );
        String contentEn = String.format(
            "Thank you for your feedback! The %s you reported has been removed/collapsed. Your contribution makes our community better.",
            targetType.equals("POST") ? "post" : "comment"
        );

        Notification notification = Notification.builder()
            .user(reporter)
            .type(Notification.Type.REPORT_SUCCESS)
            .title(createBilingualText(titleZh, titleEn))
            .content(createBilingualText(contentZh, contentEn))
            .reportId(reportId)
            .postId(targetType.equals("POST") ? targetId : null)
            .commentId(targetType.equals("COMMENT") ? targetId : null)
            .isRead(false)
            .createdAt(Instant.now())
            .build();

        notificationRepository.save(notification);
        log.info("Sent success notification to reporter: userId={}, reportId={}", reporter.getId(), reportId);
    }

    /**
     * Send notification to reporter when report is dismissed
     */
    @Transactional
    public void notifyReporterFailed(AppUser reporter, Long reportId, String targetType, String targetId, String aiReason) {
        String titleZh = "举报未通过 | Report Dismissed";
        String titleEn = "Report Dismissed | 举报未通过";
        String contentZh = String.format(
            "感谢您的反馈。经过AI审核，您举报的%s内容合规，无需处理。\n\nAI判定理由：%s",
            targetType.equals("POST") ? "帖子" : "评论",
            aiReason != null ? aiReason : "内容符合社区准则"
        );
        String contentEn = String.format(
            "Thank you for your feedback. After AI review, the %s you reported is compliant and requires no action.\n\nAI reasoning: %s",
            targetType.equals("POST") ? "post" : "comment",
            aiReason != null ? aiReason : "Content complies with community guidelines"
        );

        Notification notification = Notification.builder()
            .user(reporter)
            .type(Notification.Type.REPORT_FAILED)
            .title(createBilingualText(titleZh, titleEn))
            .content(createBilingualText(contentZh, contentEn))
            .reportId(reportId)
            .postId(targetType.equals("POST") ? targetId : null)
            .commentId(targetType.equals("COMMENT") ? targetId : null)
            .isRead(false)
            .createdAt(Instant.now())
            .build();

        notificationRepository.save(notification);
        log.info("Sent failed notification to reporter: userId={}, reportId={}", reporter.getId(), reportId);
    }

    /**
     * Send notification to post author when post is penalized
     */
    @Transactional
    public void notifyPostAuthorPenalty(AppUser author, Long reportId, String postId, String violationSnippet, String penaltyAction) {
        String titleZh = "内容违规通知 | Content Violation Notice";
        String titleEn = "Content Violation Notice | 内容违规通知";
        String contentZh = String.format(
            "您的帖子因违反社区准则而被处理。\n\n违规片段：%s\n\n处理措施：%s\n\n如有异议，您可以申诉。",
            violationSnippet != null ? violationSnippet : "（AI已识别违规内容）",
            penaltyAction
        );
        String contentEn = String.format(
            "Your post has been processed for violating community guidelines.\n\nViolation snippet: %s\n\nPenalty action: %s\n\nIf you disagree, you can appeal.",
            violationSnippet != null ? violationSnippet : "(AI identified violation)",
            penaltyAction
        );

        Notification notification = Notification.builder()
            .user(author)
            .type(Notification.Type.POST_VIOLATION_PENALTY)
            .title(createBilingualText(titleZh, titleEn))
            .content(createBilingualText(contentZh, contentEn))
            .reportId(reportId)
            .postId(postId)
            .isRead(false)
            .createdAt(Instant.now())
            .build();

        notificationRepository.save(notification);
        log.info("Sent penalty notification to post author: userId={}, postId={}", author.getId(), postId);
    }

    /**
     * Send notification to post author when post is restored
     */
    @Transactional
    public void notifyPostAuthorRestored(AppUser author, String postId) {
        String titleZh = "内容已恢复 | Content Restored";
        String titleEn = "Content Restored | 内容已恢复";
        String contentZh = "抱歉，我们之前的审核有误。您的帖子已恢复，感谢您的理解。";
        String contentEn = "Sorry, our previous review was incorrect. Your post has been restored. Thank you for your understanding.";

        Notification notification = Notification.builder()
            .user(author)
            .type(Notification.Type.POST_RESTORED)
            .title(createBilingualText(titleZh, titleEn))
            .content(createBilingualText(contentZh, contentEn))
            .postId(postId)
            .isRead(false)
            .createdAt(Instant.now())
            .build();

        notificationRepository.save(notification);
        log.info("Sent restoration notification to post author: userId={}, postId={}", author.getId(), postId);
    }

    /**
     * Send notification to comment author when comment is penalized
     */
    @Transactional
    public void notifyCommentAuthorPenalty(AppUser author, Long reportId, String commentId, String violationSnippet, String penaltyAction) {
        String titleZh = "内容违规通知 | Content Violation Notice";
        String titleEn = "Content Violation Notice | 内容违规通知";
        String contentZh = String.format(
            "您的评论因违反社区准则而被处理。\n\n违规片段：%s\n\n处理措施：%s\n\n如有异议，您可以申诉。",
            violationSnippet != null ? violationSnippet : "（AI已识别违规内容）",
            penaltyAction
        );
        String contentEn = String.format(
            "Your comment has been processed for violating community guidelines.\n\nViolation snippet: %s\n\nPenalty action: %s\n\nIf you disagree, you can appeal.",
            violationSnippet != null ? violationSnippet : "(AI identified violation)",
            penaltyAction
        );

        Notification notification = Notification.builder()
            .user(author)
            .type(Notification.Type.COMMENT_VIOLATION_PENALTY)
            .title(createBilingualText(titleZh, titleEn))
            .content(createBilingualText(contentZh, contentEn))
            .reportId(reportId)
            .commentId(commentId)
            .isRead(false)
            .createdAt(Instant.now())
            .build();

        notificationRepository.save(notification);
        log.info("Sent penalty notification to comment author: userId={}, commentId={}", author.getId(), commentId);
    }

    /**
     * Send notification to comment author when comment is restored
     */
    @Transactional
    public void notifyCommentAuthorRestored(AppUser author, String commentId) {
        String titleZh = "内容已恢复 | Content Restored";
        String titleEn = "Content Restored | 内容已恢复";
        String contentZh = "抱歉，我们之前的审核有误。您的评论已恢复，感谢您的理解。";
        String contentEn = "Sorry, our previous review was incorrect. Your comment has been restored. Thank you for your understanding.";

        Notification notification = Notification.builder()
            .user(author)
            .type(Notification.Type.COMMENT_RESTORED)
            .title(createBilingualText(titleZh, titleEn))
            .content(createBilingualText(contentZh, contentEn))
            .commentId(commentId)
            .isRead(false)
            .createdAt(Instant.now())
            .build();

        notificationRepository.save(notification);
        log.info("Sent restoration notification to comment author: userId={}, commentId={}", author.getId(), commentId);
    }

    /**
     * Create bilingual text format: "ZH | EN"
     */
    private String createBilingualText(String zh, String en) {
        return String.format("%s | %s", zh, en);
    }
}

