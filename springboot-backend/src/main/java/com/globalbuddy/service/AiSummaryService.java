package com.globalbuddy.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Summary Service
 * Uses Alibaba Cloud DashScope SDK (Qwen model) to generate news summaries
 */
@Slf4j
@Service
public class AiSummaryService {

    @Value("${dashscope.api.key:}")
    private String apiKey;

    private final Generation gen = new Generation();

    /**
     * Generate news summary
     * Uses Qwen model to summarize news content, generating a Chinese summary not exceeding 100 characters
     * 
     * @param content Original news content
     * @return AI-generated summary text
     * @throws IllegalStateException If API Key is not configured
     * @throws NoApiKeyException API Key exception
     * @throws ApiException API call exception
     * @throws InputRequiredException Input parameter exception
     */
    public String generateSummary(String content) throws NoApiKeyException, ApiException, InputRequiredException {
        // Check if API Key is configured
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("DashScope API Key not configured, please set dashscope.api.key in application.properties or application.yml");
            throw new IllegalStateException("Please configure dashscope.api.key");
        }
        
        // Check API Key format (DashScope API Key usually starts with sk-)
        if (!apiKey.startsWith("sk-")) {
            log.warn("API Key format may be incorrect, DashScope API Key usually starts with 'sk-', current: {}...", 
                     apiKey.substring(0, Math.min(10, apiKey.length())));
        }
        
        log.info("Using API Key: {}...{} (length: {})", 
                 apiKey.substring(0, Math.min(10, apiKey.length())), 
                 apiKey.length() > 10 ? apiKey.substring(apiKey.length() - 4) : "",
                 apiKey.length());

        // Check if input content is empty
        if (content == null || content.trim().isEmpty()) {
            log.warn("Input content is empty, cannot generate summary");
            return "";
        }

        try {
            log.info("Starting to generate summary, content length: {}", content.length());

            // Construct prompt
            String prompt = String.format("Please read the following news content and generate a brief summary in Chinese (not exceeding 100 characters): %s", content);
            log.debug("Prompt: {}", prompt.substring(0, Math.min(100, prompt.length())));

            // Build message list
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build());

            // Build Qwen parameters
        QwenParam param = QwenParam.builder()
                .apiKey(apiKey)
                // 使用 qwen3-max 模型生成摘要
                .model("qwen3-max")
                    .messages(messages)
                    .resultFormat(QwenParam.ResultFormat.MESSAGE)
                    .temperature(0.3f) // Lower temperature value to make output more deterministic and concise
                    .build();

            log.info("Starting to call DashScope API...");
            // Call Qwen API (direct chained call, refer to QwenService)
            String summary = gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();
            log.info("DashScope API call successful");
            
            if (summary == null || summary.isEmpty()) {
                log.error("DashScope API returned empty summary content");
                throw new RuntimeException("DashScope API returned empty summary content");
            }

            log.info("Summary generated successfully, length: {}", summary.length());
            return summary.trim();

        } catch (NoApiKeyException e) {
            log.error("API Key exception: {} - {}", e.getMessage(), e.getClass().getName(), e);
            throw new RuntimeException("API Key exception: " + e.getMessage(), e);
        } catch (ApiException e) {
            log.error("Exception occurred while calling Qwen API: {} - Exception type: {}", 
                     e.getMessage(), e.getClass().getName(), e);
            throw new RuntimeException("Failed to call Qwen API: " + e.getMessage(), e);
        } catch (InputRequiredException e) {
            log.error("Input parameter exception: {}", e.getMessage(), e);
            throw new RuntimeException("Input parameter exception: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unknown exception occurred while generating summary: {} - Exception type: {}", e.getMessage(), e.getClass().getName(), e);
            throw new RuntimeException("Failed to generate summary: " + e.getMessage() + " (" + e.getClass().getSimpleName() + ")", e);
        }
    }

    /**
     * Batch generate summaries
     * Generate summaries for multiple news contents
     * 
     * @param contents List of news contents
     * @return List of summaries, corresponding to input list order
     */
    public List<String> generateSummaries(List<String> contents) {
        List<String> summaries = new ArrayList<>();
        for (String content : contents) {
            try {
                String summary = generateSummary(content);
                summaries.add(summary);
            } catch (Exception e) {
                log.warn("Failed to generate summary for content: {}", e.getMessage());
                summaries.add(""); // Return empty string on failure
            }
        }
        return summaries;
    }

    /**
     * Generate comment summary
     * Summarizes all comments for a post using AI
     * 
     * @param comments List of comment contents to summarize
     * @param lang Target language for summary (zh/en)
     * @return AI-generated summary text in the specified language
     * @throws IllegalStateException If API Key is not configured
     */
    public String generateCommentSummary(List<String> comments, String lang) {
        // Check if API Key is configured
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("DashScope API Key not configured");
            throw new IllegalStateException("Please configure dashscope.api.key");
        }

        // Check if comments list is empty
        if (comments == null || comments.isEmpty()) {
            log.warn("Comments list is empty, cannot generate summary");
            return lang.equals("zh") ? "暂无评论可总结" : "No comments to summarize";
        }

        // Filter out empty comments and combine them
        List<String> validComments = comments.stream()
                .filter(c -> c != null && !c.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());

        if (validComments.isEmpty()) {
            log.warn("No valid comments found");
            return lang.equals("zh") ? "暂无有效评论可总结" : "No valid comments to summarize";
        }

        // Combine all comments into a single text
        StringBuilder combinedText = new StringBuilder();
        for (int i = 0; i < validComments.size(); i++) {
            combinedText.append("评论").append(i + 1).append(": ").append(validComments.get(i)).append("\n");
        }

        String combinedContent = combinedText.toString();
        log.info("Generating comment summary for {} comments, content length: {}", validComments.size(), combinedContent.length());

        try {
            // Determine language instruction for summary
            String languageInstruction = lang.equals("zh") ? "请用中文" : "Please use English";
            
            // Construct prompt
            String prompt = String.format(
                "%s总结以下所有评论的主要内容、观点和关键信息。总结应该简洁明了，不超过200字，突出评论中的核心观点和常见主题：\n\n%s",
                languageInstruction,
                combinedContent
            );

            // Build message list
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build());

            // Build Qwen parameters
        QwenParam param = QwenParam.builder()
                .apiKey(apiKey)
                .model("qwen3-max")
                    .messages(messages)
                    .resultFormat(QwenParam.ResultFormat.MESSAGE)
                    .temperature(0.3f)
                    .build();

            log.info("Calling DashScope API for comment summary...");
            String summary = gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();
            log.info("Comment summary generated successfully, length: {}", summary != null ? summary.length() : 0);
            
            if (summary == null || summary.isEmpty()) {
                log.error("DashScope API returned empty summary content");
                return lang.equals("zh") ? "无法生成评论总结" : "Failed to generate comment summary";
            }

            return summary.trim();

        } catch (Exception e) {
            log.error("Failed to generate comment summary: {}", e.getMessage(), e);
            return lang.equals("zh") ? "生成评论总结时出错: " + e.getMessage() : "Error generating comment summary: " + e.getMessage();
        }
    }
}

