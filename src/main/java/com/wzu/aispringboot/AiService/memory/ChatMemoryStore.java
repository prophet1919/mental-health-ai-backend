package com.wzu.aispringboot.AiService.memory;

import java.util.List;

/**
 * 对话上下文存储抽象。
 * <p>
 * 实现可以是 MySQL 重建（降级路径）或 Redis（默认路径）。
 * 调用方（PsychologicalSupportService）只依赖本接口，切换实现无需改动业务代码。
 * <p>
 * 约定：返回的消息按时间正序（最早在前），元素已是 Spring AI 的
 * {@link org.springframework.ai.chat.messages.Message} 语义载体。
 */
public interface ChatMemoryStore {

    /**
     * 读取指定会话最近的消息，用于重建模型上下文。
     *
     * @param sessionId 数据库会话 ID（非 "session_12" 这种字符串形式）
     * @param limit     最多返回条数，实现方需自行裁剪
     * @return 时间正序的消息列表；无历史时返回空列表，不返回 null
     */
    List<StoredMessage> loadRecent(Long sessionId, int limit);

    /**
     * 追加一条消息到上下文存储。
     *
     * @param sessionId 数据库会话 ID
     * @param message   待追加消息
     */
    void append(Long sessionId, StoredMessage message);

    /**
     * 清空指定会话的全部上下文。
     */
    void clear(Long sessionId);

    /**
     * 上下文中的单条消息。
     *
     * @param senderType 1=用户，2=AI（与 consultation_message.sender_type 一致）
     * @param content    消息正文
     */
    record StoredMessage(int senderType, String content) {
        public boolean isUser() {
            return senderType == 1;
        }
    }
}
