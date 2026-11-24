package com.wzu.aispringboot.AiService;

public class StructOutput {

    /**
     * 会话创建结果。
     * <p>
     * 本设计下"创建会话"不再写入初始消息，因此 messageCount 恒为 0，
     * 由前端在会话建立后调用 /stream 发起第一轮对话。
     *
     * @param sessionId    外部会话标识，形如 session_12
     * @param userId       归属用户 ID
     * @param startTime    创建时间戳（毫秒）
     * @param expiryTime   过期时间戳（毫秒）
     * @param messageCount 当前消息数
     * @param status       状态标识，创建后为 ACTIVE
     */
    public record StreamChatSession(
            String sessionId,
            Long userId,
            Long startTime,
            Long expiryTime,
            Integer messageCount,
            String status) {

    }
}
