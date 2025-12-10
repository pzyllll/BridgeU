package com.globalbuddy.service;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Qwen 模型服务
 * 使用阿里云 DashScope SDK 调用 Qwen 模型进行文本总结
 */
@Service
public class QwenService {

    @Value("${dashscope.api.key:}")
    private String apiKey;

    private final Generation gen = new Generation();

    /**
     * 使用 Qwen 模型总结文本内容
     * 
     * @param text 需要总结的文本
     * @return 总结后的文本
     */
    public String summarizeText(String text) throws NoApiKeyException, ApiException, InputRequiredException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("请配置 dashscope.api.key");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个专业的文本总结助手，请用简洁明了的语言总结用户提供的内容。")
                .build());
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content("请总结以下内容：\n" + text)
                .build());

        QwenParam param = QwenParam.builder()
                .apiKey(apiKey)
                .model("qwen-max-2025-01-25") // Updated model version
                .messages(messages)
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                // 当前 SDK 版本不支持 maxTokens(...)，使用默认长度配置
                .temperature(0.7f)
                .build();

        return gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();
    }

    /**
     * 使用 Qwen 模型回答问题
     * 
     * @param question 用户问题
     * @param context 上下文信息（如相关帖子内容）
     * @return 回答内容
     */
    public String answerQuestion(String question, String context) throws NoApiKeyException, ApiException, InputRequiredException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("请配置 dashscope.api.key");
        }

        List<Message> messages = new ArrayList<>();
        messages.add(Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个留学生互助平台的智能助手，请根据提供的上下文信息回答用户问题。")
                .build());
        messages.add(Message.builder()
                .role(Role.USER.getValue())
                .content(String.format("上下文信息：\n%s\n\n用户问题：%s", context, question))
                .build());

        QwenParam param = QwenParam.builder()
                .apiKey(apiKey)
                .model("qwen-max-2025-01-25") // Updated model version
                .messages(messages)
                .resultFormat(QwenParam.ResultFormat.MESSAGE)
                // 当前 SDK 版本不支持 maxTokens(...)，使用默认长度配置
                .temperature(0.7f)
                .build();

        return gen.call(param).getOutput().getChoices().get(0).getMessage().getContent();
    }
}

