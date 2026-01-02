package com.globalbuddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalbuddy.model.News;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 新闻相关性筛选服务
 * 使用 AI 检测新闻是否对留学生有帮助
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsRelevanceService {

    private final QwenService qwenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 对留学生有帮助的关键词（用于快速预筛选）
    private static final List<String> RELEVANT_KEYWORDS = Arrays.asList(
        // 签证相关
        "visa", "签证", "immigration", "移民", "入境", "出境", "护照", "passport",
        "work permit", "工作许可", "居留", "residence", "extension", "延期",
        // 教育相关
        "education", "教育", "university", "大学", "college", "学院", "student", "学生",
        "scholarship", "奖学金", "admission", "录取", "enrollment", "注册", "tuition", "学费",
        "degree", "学位", "graduation", "毕业", "academic", "学术",
        // 生活相关
        "accommodation", "住宿", "housing", "住房", "rent", "租金", "dormitory", "宿舍",
        "transportation", "交通", "transport", "public transport", "公共交通",
        "health", "健康", "medical", "医疗", "hospital", "医院", "insurance", "保险",
        "bank", "银行", "account", "账户", "finance", "金融",
        // 工作相关
        "job", "工作", "employment", "就业", "career", "职业", "internship", "实习",
        "salary", "薪资", "wage", "工资",
        // 政策相关
        "policy", "政策", "regulation", "规定", "law", "法律", "announcement", "公告",
        "update", "更新", "change", "变化", "new", "新",
        // 大学相关
        "university", "大学", "campus", "校园", "faculty", "学院", "department", "系",
        "research", "研究", "conference", "会议", "seminar", "研讨会",
        // 泰国特定
        "thailand", "泰国", "thai", "bangkok", "曼谷", "chiang mai", "清迈"
    );

    /**
     * 检测新闻是否对留学生有帮助
     * 
     * @param news 待检测的新闻
     * @return 相关性检测结果
     */
    public RelevanceResult checkRelevance(News news) {
        log.info("开始检测新闻相关性: {}", news.getTitle());
        
        // 1. 快速预筛选：检查关键词
        boolean hasRelevantKeywords = checkKeywords(news);
        if (!hasRelevantKeywords) {
            log.debug("新闻未包含相关关键词，可能不相关: {}", news.getTitle());
            // 即使没有关键词，也继续AI检测（因为AI可能发现隐含的相关性）
        }
        
        // 2. 调用 AI 进行深度分析
        try {
            String aiAnalysis = analyzeRelevanceWithAI(news);
            ParsedResult parsed = parseAiResult(aiAnalysis);
            
            // 大幅放宽筛选标准：只要AI判断为相关就保留，不设置信度阈值
            // 或者如果置信度 >= 30 且 AI 判断为相关，也保留
            boolean shouldKeep = parsed.isRelevant;
            
            // 如果置信度 >= 30 且 AI 判断为相关，保留
            if (parsed.isRelevant && parsed.confidenceScore >= 30) {
                shouldKeep = true;
            }
            
            // 如果有关键词，即使AI判断不相关但置信度不高，也倾向于保留（可能是误判）
            if (hasRelevantKeywords && !parsed.isRelevant && parsed.confidenceScore < 70) {
                shouldKeep = true; // 有关键词但AI说不相关且置信度不高，可能是误判，保留
            }
            
            return RelevanceResult.builder()
                .isRelevant(shouldKeep)
                .confidence(parsed.confidenceScore / 100.0)
                .reason(parsed.reason + (shouldKeep != parsed.isRelevant ? " (已放宽标准)" : ""))
                .category(parsed.category)
                .aiResult(parsed.rawJson)
                .hasKeywords(hasRelevantKeywords)
                .build();
        } catch (Exception e) {
            log.error("AI 相关性检测失败: {} - {}", news.getTitle(), e.getMessage(), e);
            // AI失败时，如果有关键词则保留，否则拒绝
            return RelevanceResult.builder()
                .isRelevant(hasRelevantKeywords)
                .confidence(hasRelevantKeywords ? 0.6 : 0.3)
                .reason("AI 分析失败，基于关键词判断: " + (hasRelevantKeywords ? "包含相关关键词" : "无相关关键词"))
                .category("unknown")
                .aiResult("AI unavailable: " + e.getMessage())
                .hasKeywords(hasRelevantKeywords)
                .build();
        }
    }

    /**
     * 检查新闻是否包含相关关键词
     */
    private boolean checkKeywords(News news) {
        StringBuilder combinedTextBuilder = new StringBuilder();
        if (news.getTitle() != null) {
            combinedTextBuilder.append(news.getTitle().toLowerCase()).append(" ");
        }
        if (news.getSummary() != null) {
            combinedTextBuilder.append(news.getSummary().toLowerCase()).append(" ");
        }
        if (news.getOriginalContent() != null) {
            // 只检查前500字符，避免处理过长内容
            String contentPreview = news.getOriginalContent().length() > 500 
                ? news.getOriginalContent().substring(0, 500).toLowerCase()
                : news.getOriginalContent().toLowerCase();
            combinedTextBuilder.append(contentPreview).append(" ");
        }
        
        final String combinedText = combinedTextBuilder.toString();
        
        return RELEVANT_KEYWORDS.stream()
            .anyMatch(keyword -> combinedText.contains(keyword.toLowerCase()));
    }

    /**
     * 调用 AI 分析新闻相关性
     */
    private String analyzeRelevanceWithAI(News news) {
        // 构建分析文本（优先使用完整内容，否则使用摘要和标题）
        String contentToAnalyze = news.getOriginalContent();
        if (contentToAnalyze == null || contentToAnalyze.isEmpty()) {
            contentToAnalyze = (news.getSummary() != null ? news.getSummary() : "") + " " + 
                              (news.getTitle() != null ? news.getTitle() : "");
        }
        
        // 限制长度，避免API错误
        if (contentToAnalyze.length() > 2000) {
            contentToAnalyze = contentToAnalyze.substring(0, 2000) + "...";
        }
        
        String prompt = String.format(
            "你是一个专门为在泰国的国际留学生筛选有用新闻的AI助手。请分析以下新闻是否对留学生有帮助。\n\n" +
            "对留学生有帮助的新闻包括但不限于：\n" +
            "1. 签证和移民政策更新（visa, immigration, work permit等）\n" +
            "2. 教育政策和大学新闻（education, university, scholarship, admission等）\n" +
            "3. 生活实用信息（accommodation, transportation, health, bank, 交通, 医疗, 银行等）\n" +
            "4. 就业和工作许可（job, employment, career等）\n" +
            "5. 政府政策变化（policy, regulation, announcement等）\n" +
            "6. 大学活动和学术信息（campus, research, conference等）\n" +
            "7. 社会新闻和时事（可能影响留学生的日常生活）\n" +
            "8. 经济新闻（如果涉及物价、汇率、生活成本等）\n" +
            "9. 安全相关新闻（涉及公共安全、犯罪等，留学生需要了解）\n\n" +
            "筛选标准：\n" +
            "- 如果新闻可能对留学生的日常生活、学习、工作、安全有任何帮助或参考价值，请标记为相关\n" +
            "- 只有完全无关的纯娱乐八卦、纯体育赛事（无教育意义）、纯政治斗争（不影响留学生）才应被忽略\n" +
            "- 放宽标准：只要新闻可能对留学生有参考价值，即使相关性不是特别强，也建议保留\n\n" +
            "请用JSON格式回答，包含以下字段：\n" +
            "- is_relevant: boolean（是否对留学生有帮助，标准放宽）\n" +
            "- confidence_score: number 0-100（置信度）\n" +
            "- reason: string（判断理由）\n" +
            "- category: string（新闻类别，如：visa, education, life, work, policy, university, society, economy, safety等）\n\n" +
            "JSON示例：{\"is_relevant\": true, \"confidence_score\": 60, \"reason\": \"社会新闻，可能对留学生了解当地情况有帮助\", \"category\": \"society\"}\n\n" +
            "新闻标题：%s\n" +
            "新闻内容：%s",
            news.getTitle() != null ? news.getTitle() : "",
            contentToAnalyze
        );

        try {
            return qwenService.answerQuestion(prompt, "");
        } catch (Exception e) {
            log.warn("AI 服务调用失败: {}", e.getMessage());
            throw new RuntimeException("AI service unavailable", e);
        }
    }

    /**
     * 解析 AI 返回的 JSON 结果
     */
    private ParsedResult parseAiResult(String aiResponse) {
        try {
            // 尝试提取JSON（可能被代码块包裹）
            String jsonStr = aiResponse.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();
            
            JsonNode jsonNode = objectMapper.readTree(jsonStr);
            
            boolean isRelevant = jsonNode.has("is_relevant") && jsonNode.get("is_relevant").asBoolean();
            int confidenceScore = jsonNode.has("confidence_score") 
                ? jsonNode.get("confidence_score").asInt() 
                : 50;
            String reason = jsonNode.has("reason") 
                ? jsonNode.get("reason").asText() 
                : "No reason provided";
            String category = jsonNode.has("category") 
                ? jsonNode.get("category").asText() 
                : "unknown";
            
            return new ParsedResult(isRelevant, confidenceScore, reason, aiResponse, category);
        } catch (Exception e) {
            log.error("解析 AI 结果失败: {} - {}", aiResponse, e.getMessage());
            // 如果解析失败，尝试从文本中推断（放宽标准：倾向于保留）
            String lowerResponse = aiResponse.toLowerCase();
            boolean isRelevant = lowerResponse.contains("true") || 
                                lowerResponse.contains("是") || 
                                lowerResponse.contains("relevant") ||
                                lowerResponse.contains("有帮助") ||
                                !lowerResponse.contains("false") && !lowerResponse.contains("不相关"); // 如果没有明确说不相关，倾向于保留
            // 解析失败时，给予中等置信度，让系统有机会保留
            return new ParsedResult(isRelevant, isRelevant ? 60 : 30, "Failed to parse AI response, using text inference", aiResponse, "unknown");
        }
    }

    /**
     * 相关性检测结果
     */
    @Data
    @Builder
    public static class RelevanceResult {
        /** 是否对留学生有帮助 */
        private boolean isRelevant;
        
        /** 置信度 (0-1) */
        private double confidence;
        
        /** 判断理由 */
        private String reason;
        
        /** 新闻类别 */
        private String category;
        
        /** AI 原始返回结果 */
        private String aiResult;
        
        /** 是否包含相关关键词 */
        private boolean hasKeywords;
    }

    /**
     * 解析后的 AI 结果
     */
    private static class ParsedResult {
        final boolean isRelevant;
        final int confidenceScore;
        final String reason;
        final String rawJson;
        final String category;

        ParsedResult(boolean isRelevant, int confidenceScore, String reason, String rawJson, String category) {
            this.isRelevant = isRelevant;
            this.confidenceScore = confidenceScore;
            this.reason = reason;
            this.rawJson = rawJson;
            this.category = category;
        }
    }
}

