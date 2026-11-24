package com.wzu.aispringboot.DTO.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项 / 会话详情响应。
 */
@Data
@Builder
public class ConsultationSessionResponseDTO {

    private Long id;

    /** 外部会话标识，形如 session_12 */
    private String sessionId;

    private Long userId;

    /** 会话归属用户的显示名（后台列表使用） */
    private String username;

    private String sessionTitle;

    private LocalDateTime startedAt;

    /** 最后一条消息时间，列表按此倒序 */
    private LocalDateTime lastActiveAt;

    /** 消息总数 */
    private Integer messageCount;

    /** 最后一条消息摘要（列表展示用，截断） */
    private String lastMessagePreview;
}
