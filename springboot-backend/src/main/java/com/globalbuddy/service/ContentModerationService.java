package com.globalbuddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalbuddy.model.CommunityPost;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容审核服务
 * 使用 AI 模型对用户发布的内容进行自动审核
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentModerationService {

    private final QwenService qwenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 敏感词/违禁词列表（可扩展到数据库或配置文件）
    private static final List<String> SENSITIVE_WORDS = List.of(
        "scam", "fraud", "fake", "illegal", "hate", "violence",
        "drug", "drugs", "weapon", "kill", "terror", "porn", "sex",
        "spam", "phishing", "fuck", "shit", "bitch", "asshole",
        "rape",
        "欺诈", "诈骗", "非法", "暴力", "仇恨", "恐怖", "色情"
    );

    // AI 置信度阈值（0-100），低于等于此值走人工审核
    private static final double CONFIDENCE_THRESHOLD = 90.0;

    /**
     * 审核帖子内容
     * @param post 待审核的帖子
     * @return 审核结果
     */
    public ModerationResult moderatePost(CommunityPost post) {
        log.info("开始审核帖子: {}", post.getId());

        // 1. 强规则敏感词拦截（直接拒绝）
        String content = (post.getTitle() == null ? "" : post.getTitle()) + " " + (post.getBody() == null ? "" : post.getBody());
        String hit = findSensitiveWord(content);
        if (hit != null) {
            log.warn("帖子命中敏感词直接拒绝: {} -> {}", post.getId(), hit);
            return ModerationResult.builder()
                .approved(false)
                .needsManualReview(false)
                .aiResult("HIT_SENSITIVE_WORD: " + hit)
                .confidence(0.99)
                .reason("命中敏感词: " + hit)
                .status(CommunityPost.Status.REJECTED)
                .build();
        }

        // 2. 调用 AI 进行内容分析（结构化 JSON 输出）
        try {
            String aiAnalysis = analyzeContentWithAI(post);
            ParsedResult parsed = parseAiResult(aiAnalysis);

            // 根据规则设置状态
            CommunityPost.Status status;
            boolean needsManual = false;

            if (parsed.confidenceScore > CONFIDENCE_THRESHOLD) {
                if (parsed.isSafe) {
                    status = CommunityPost.Status.APPROVED;
                } else {
                    status = CommunityPost.Status.REJECTED;
                }
            } else {
                status = CommunityPost.Status.PENDING_REVIEW;
                needsManual = true;
            }

            return ModerationResult.builder()
                .approved(status == CommunityPost.Status.APPROVED)
                .needsManualReview(needsManual)
                .aiResult(parsed.rawJson)
                .confidence(parsed.confidenceScore / 100.0) // store as 0-1 for legacy; raw retained in aiResult
                .reason(parsed.reason)
                .status(status)
                .build();
        } catch (Exception e) {
            log.error("AI 审核失败，标记为待审核: {}", post.getId(), e);
            return ModerationResult.builder()
                .approved(false)
                .needsManualReview(true)
                .aiResult("AI 分析失败: " + e.getMessage())
                .confidence(0.0)
                .reason("AI 服务异常，需要人工审核")
                .status(CommunityPost.Status.PENDING_REVIEW)
                .build();
        }
    }

    /**
     * 检查敏感词
     */
    private String findSensitiveWord(String content) {
        String lowerContent = content.toLowerCase();
        return SENSITIVE_WORDS.stream()
            .filter(word -> lowerContent.contains(word.toLowerCase()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 调用 AI 分析内容
     */
    private String analyzeContentWithAI(CommunityPost post) {
        String prompt = String.format(
            "You are a strict content safety reviewer. Evaluate if the post is legal and compliant. "
            + "Flag fraud, hate speech, pornography, illegal activity. "
            + "Respond ONLY in JSON with fields: is_safe (boolean), reason (string), confidence_score (0-100). "
            + "JSON example: {\"is_safe\": true, \"reason\": \"no violations\", \"confidence_score\": 95}."
            + "\\nTitle: %s\\nContent: %s",
            post.getTitle(),
            post.getBody()
        );

        try {
            return qwenService.answerQuestion(prompt, "");
        } catch (Exception e) {
            log.warn("AI 服务调用失败，使用默认分析", e);
            // 返回默认的不确定结果
            return "{\"is_safe\": null, \"reason\": \"AI unavailable\", \"confidence_score\": 0}";
        }
    }

    private ParsedResult parseAiResult(String aiJson) {
        try {
            JsonNode node = objectMapper.readTree(aiJson);
            boolean isSafe = node.path("is_safe").asBoolean(false);
            double score = node.path("confidence_score").asDouble(0);
            String reason = node.path("reason").asText("未提供理由");
            return new ParsedResult(isSafe, score, reason, aiJson);
        } catch (Exception e) {
            log.warn("AI 返回解析失败，回退为待审核: {}", e.getMessage());
            return new ParsedResult(false, 0, "AI 结果解析失败", aiJson);
        }
    }

    /**
     * 审核结果
     */
    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModerationResult {
        private boolean approved;
        private boolean needsManualReview;
        private String aiResult;
        private double confidence; // 0-1
        private String reason;
        private CommunityPost.Status status;
    }

    private record ParsedResult(boolean isSafe, double confidenceScore, String reason, String rawJson) {}
}
