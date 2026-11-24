package com.wzu.aispringboot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置。
 * <p>
 * 注意：这里不再注册 {@code MessageChatMemoryAdvisor}。
 * 会话上下文改由 {@link com.wzu.aispringboot.AiService.memory.ChatMemoryStore}
 * 在每次请求时按 sessionId 重建后显式放进 Prompt，
 * 若同时挂载 Advisor，会因缺少 conversationId 参数而抛
 * "conversationId cannot be null"，且两套记忆来源会互相干扰。
 */
@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.openai.chat.options.temperature:0.7}")
    private Double temperature;

    @Bean("open-ai")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem("你是一个专业的心理健康助手，你的任务是帮助用户解决心理健康问题。")
                .build();
    }
}
