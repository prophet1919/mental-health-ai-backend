package com.wzu.aispringboot.AiService.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzu.aispringboot.entity.ConsultationMessage;
import com.wzu.aispringboot.mapper.ConsultationMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 MySQL 的上下文实现：每次按会话 ID 查最近 N 条消息重建上下文。
 * <p>
 * 作为 {@link ChatMemoryStore} 的降级/兜底实现：服务重启不丢上下文，
 * 且不依赖任何外部中间件。Redis 实现就绪后可通过配置切换。
 */
@Component
@ConditionalOnProperty(name = "app.chat-memory.store", havingValue = "mysql", matchIfMissing = true)
public class MysqlChatMemoryStore implements ChatMemoryStore {

    private static final Logger LOG = LoggerFactory.getLogger(MysqlChatMemoryStore.class);

    /** 单条上下文消息的最大字符数，防止超长历史把 prompt 撑爆 */
    private static final int MAX_CONTENT_CHARS = 2000;

    private final ConsultationMessageMapper messageMapper;

    public MysqlChatMemoryStore(ConsultationMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public List<StoredMessage> loadRecent(Long sessionId, int limit) {
        if (sessionId == null || limit <= 0) {
            return Collections.emptyList();
        }
        try {
            LambdaQueryWrapper<ConsultationMessage> query = new LambdaQueryWrapper<>();
            query.eq(ConsultationMessage::getSessionId, sessionId)
                    .orderByDesc(ConsultationMessage::getId)
                    .last("limit " + limit);

            List<ConsultationMessage> desc = messageMapper.selectList(query);
            if (desc == null || desc.isEmpty()) {
                return Collections.emptyList();
            }

            // 查询是倒序（最新在前），上下文需要正序
            List<StoredMessage> result = new ArrayList<>(desc.size());
            for (int i = desc.size() - 1; i >= 0; i--) {
                ConsultationMessage m = desc.get(i);
                if (m == null || m.getContent() == null || m.getContent().isBlank()) {
                    continue;
                }
                String content = m.getContent();
                if (content.length() > MAX_CONTENT_CHARS) {
                    content = content.substring(0, MAX_CONTENT_CHARS);
                }
                int senderType = m.getSenderType() == null ? 2 : m.getSenderType();
                result.add(new StoredMessage(senderType, content));
            }
            return result;
        } catch (Exception e) {
            // 上下文读取失败不应阻断对话：降级为"无上下文"继续回答
            LOG.warn("读取会话上下文失败 sessionId={}，本次对话将不带历史", sessionId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void append(Long sessionId, StoredMessage message) {
        // MySQL 实现下消息已由 ConsultationMessageService 落库，
        // 此处无需重复写入，避免产生双份数据。
        LOG.debug("MysqlChatMemoryStore.append 跳过写库 sessionId={}（消息已由业务层落库）", sessionId);
    }

    @Override
    public void clear(Long sessionId) {
        // 同上：会话消息的删除由业务层负责，此处不重复处理
        LOG.debug("MysqlChatMemoryStore.clear 跳过 sessionId={}", sessionId);
    }
}
