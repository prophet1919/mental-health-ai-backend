package com.wzu.aispringboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wzu.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.wzu.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import com.wzu.aispringboot.DTO.response.PageResponse;
import com.wzu.aispringboot.entity.ConsultationMessage;
import com.wzu.aispringboot.entity.ConsultationSession;
import com.wzu.aispringboot.exception.BusinessException;
import com.wzu.aispringboot.mapper.ConsultationMessageMapper;
import com.wzu.aispringboot.mapper.ConsultationSessionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话与消息查询服务：列表、详情、消息分页。
 * <p>
 * 归属校验集中在本类，避免每个 Controller 各写一遍导致漏校验越权。
 */
@Service
public class ConsultationQueryService {

    private static final int PREVIEW_MAX = 60;

    private final ConsultationSessionMapper sessionMapper;
    private final ConsultationMessageMapper messageMapper;

    public ConsultationQueryService(ConsultationSessionMapper sessionMapper,
                                    ConsultationMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    /**
     * 会话列表。ownerUserId 非空时只看该用户；为空时（管理员）看全部。
     */
    public PageResponse<ConsultationSessionResponseDTO> listSessions(Long ownerUserId,
                                                                    String keyword,
                                                                    long page,
                                                                    long size) {
        long safePage = Math.max(1, page);
        long safeSize = Math.min(Math.max(1, size), 100);

        LambdaQueryWrapper<ConsultationSession> countQuery = new LambdaQueryWrapper<>();
        applyOwnerAndKeyword(countQuery, ownerUserId, keyword);
        Long totalCount = sessionMapper.selectCount(countQuery);

        LambdaQueryWrapper<ConsultationSession> pageQuery = new LambdaQueryWrapper<>();
        applyOwnerAndKeyword(pageQuery, ownerUserId, keyword);
        pageQuery.orderByDesc(ConsultationSession::getStartedAt)
                .last("limit " + safeSize + " offset " + (safePage - 1) * safeSize);
        List<ConsultationSession> sessions = sessionMapper.selectList(pageQuery);

        if (sessions == null || sessions.isEmpty()) {
            return PageResponse.empty(safePage, safeSize);
        }

        List<ConsultationSessionResponseDTO> records = new ArrayList<>(sessions.size());
        for (ConsultationSession s : sessions) {
            int count = countMessages(s.getId());
            // 过滤空会话：建了会话但一条消息都没有的记录不进列表
            if (count == 0) {
                continue;
            }
            ConsultationMessage lastMessage = findLastMessage(s.getId());
            records.add(toSessionDTO(s, lastMessage, count, null));
        }

        // 按最后活跃时间倒序（仅调整当前页内顺序）
        records.sort((a, b) -> {
            LocalDateTime ta = a.getLastActiveAt();
            LocalDateTime tb = b.getLastActiveAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        long total = totalCount == null ? records.size() : totalCount;
        return PageResponse.of(records, total, safePage, safeSize);
    }

    /**
     * 会话详情，含归属校验。
     */
    public ConsultationSessionResponseDTO getSession(Long sessionId, Long ownerUserId) {
        ConsultationSession session = requireOwnedSession(sessionId, ownerUserId);
        return toSessionDTO(session, findLastMessage(sessionId), countMessages(sessionId), null);
    }

    /**
     * 消息分页：默认取最近 size 条，返回时间正序；beforeId 用于继续向前翻页。
     */
    public List<ConsultationMessageResponseDTO> listMessages(Long sessionId,
                                                             Long ownerUserId,
                                                             Long beforeId,
                                                             int size) {
        requireOwnedSession(sessionId, ownerUserId);
        int safeSize = Math.min(Math.max(1, size), 200);

        LambdaQueryWrapper<ConsultationMessage> query = new LambdaQueryWrapper<>();
        query.eq(ConsultationMessage::getSessionId, sessionId);
        if (beforeId != null && beforeId > 0) {
            query.lt(ConsultationMessage::getId, beforeId);
        }
        query.orderByDesc(ConsultationMessage::getId).last("limit " + safeSize);

        List<ConsultationMessage> desc = messageMapper.selectList(query);
        if (desc == null || desc.isEmpty()) {
            return List.of();
        }
        List<ConsultationMessage> asc = new ArrayList<>(desc);
        asc.sort((a, b) -> Long.compare(a.getId(), b.getId()));

        List<ConsultationMessageResponseDTO> result = new ArrayList<>(asc.size());
        for (ConsultationMessage m : asc) {
            result.add(toMessageDTO(m));
        }
        return result;
    }

    /**
     * 取会话并校验归属。不存在与无权访问返回同一提示，避免探测他人会话。
     */
    public ConsultationSession requireOwnedSession(Long sessionId, Long ownerUserId) {
        if (sessionId == null) {
            throw new BusinessException("会话ID不能为空");
        }
        ConsultationSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (ownerUserId != null && !ownerUserId.equals(session.getUserId())) {
            throw new BusinessException("无权访问该会话");
        }
        return session;
    }

    private void applyOwnerAndKeyword(LambdaQueryWrapper<ConsultationSession> query,
                                      Long ownerUserId,
                                      String keyword) {
        if (ownerUserId != null) {
            query.eq(ConsultationSession::getUserId, ownerUserId);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like(ConsultationSession::getSessionTitle, keyword.trim());
        }
    }

    private ConsultationSessionResponseDTO toSessionDTO(ConsultationSession session,
                                                        ConsultationMessage lastMessage,
                                                        int messageCount,
                                                        String username) {
        return ConsultationSessionResponseDTO.builder()
                .id(session.getId())
                .sessionId("session_" + session.getId())
                .userId(session.getUserId())
                .username(username)
                .sessionTitle(session.getSessionTitle())
                .startedAt(session.getStartedAt())
                .lastActiveAt(lastMessage != null ? lastMessage.getCreatedAt() : session.getStartedAt())
                .messageCount(messageCount)
                .lastMessagePreview(preview(lastMessage))
                .build();
    }

    private ConsultationMessage findLastMessage(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> query = new LambdaQueryWrapper<>();
        query.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByDesc(ConsultationMessage::getId)
                .last("limit 1");
        return messageMapper.selectOne(query);
    }

    private int countMessages(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> query = new LambdaQueryWrapper<>();
        query.eq(ConsultationMessage::getSessionId, sessionId);
        Long count = messageMapper.selectCount(query);
        return count == null ? 0 : count.intValue();
    }

    private String preview(ConsultationMessage message) {
        if (message == null || message.getContent() == null) {
            return "";
        }
        String content = message.getContent().replaceAll("\\s+", " ").trim();
        return content.length() <= PREVIEW_MAX ? content : content.substring(0, PREVIEW_MAX) + "…";
    }

    private ConsultationMessageResponseDTO toMessageDTO(ConsultationMessage message) {
        ConsultationMessageResponseDTO dto = new ConsultationMessageResponseDTO();
        dto.setId(message.getId());
        dto.setSessionId(message.getSessionId());
        dto.setSenderType(message.getSenderType());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setEmotionTag(message.getEmotionTag());
        dto.setAiModel(message.getAiModel());
        dto.setCreatedAt(message.getCreatedAt());
        dto.setSenderTypeDesc(message.getSenderTypeDesc());
        dto.setMessageTypeDesc(message.getMessageTypeDesc());
        dto.calculateContentLength();
        return dto;
    }
}
