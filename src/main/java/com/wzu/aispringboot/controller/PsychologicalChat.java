package com.wzu.aispringboot.controller;

import cn.hutool.json.JSONUtil;
import com.wzu.aispringboot.AiService.ChatStreamEvent;
import com.wzu.aispringboot.AiService.PsychologicalSupportService;
import com.wzu.aispringboot.AiService.StructOutput;
import com.wzu.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.wzu.aispringboot.DTO.command.ConsultationStreamDTO;
import com.wzu.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.wzu.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import com.wzu.aispringboot.DTO.response.PageResponse;
import com.wzu.aispringboot.common.Result;
import com.wzu.aispringboot.common.ResultCode;
import com.wzu.aispringboot.service.ConsultationQueryService;
import com.wzu.aispringboot.service.CurrentUserHolder;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/psychological-chat")
public class PsychologicalChat {

    private final PsychologicalSupportService psychologicalSupportService;
    private final ConsultationQueryService consultationQueryService;
    private final CurrentUserHolder currentUserHolder;

    public PsychologicalChat(PsychologicalSupportService psychologicalSupportService,
                             ConsultationQueryService consultationQueryService,
                             CurrentUserHolder currentUserHolder) {
        this.psychologicalSupportService = psychologicalSupportService;
        this.consultationQueryService = consultationQueryService;
        this.currentUserHolder = currentUserHolder;
    }

    /**
     * 创建会话：只建空会话，返回 sessionId。不写任何消息。
     */
    @PostMapping("/session/start")
    public Result<StructOutput.StreamChatSession> startSession(
            @Valid @RequestBody(required = false) ConsultationSessionCreateDTO createDTO) {
        Long userId = currentUserHolder.requireUserId();
        StructOutput.StreamChatSession session =
                psychologicalSupportService.startSession(userId, createDTO == null ? new ConsultationSessionCreateDTO() : createDTO);
        return Result.ok(session);
    }

    /**
     * 流式对话。事件类型：thinking / message / done / error。
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@Valid @RequestBody ConsultationStreamDTO streamDTO) {
        Long userId = currentUserHolder.requireUserId();
        Long sessionId = psychologicalSupportService.extractSessionId(streamDTO.getSessionId());

        if (sessionId == null) {
            return Flux.just(toSse(ChatStreamEvent.error("PARAM_ERROR", "sessionId 格式错误")));
        }

        // 归属校验：不允许向他人会话写入消息
        try {
            consultationQueryService.requireOwnedSession(sessionId, userId);
        } catch (RuntimeException e) {
            return Flux.just(toSse(ChatStreamEvent.error("FORBIDDEN", e.getMessage())));
        }

        return psychologicalSupportService
                .streamPsychologicalChat(sessionId, streamDTO.getUserMessage())
                .map(this::toSse);
    }

    /**
     * 会话列表。普通用户只看自己的；管理员可通过 all=true 查看全部。
     */
    @GetMapping("/sessions")
    public Result<PageResponse<ConsultationSessionResponseDTO>> listSessions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean all) {
        Long userId = currentUserHolder.requireUserId();
        Long owner = (all && currentUserHolder.isAdmin()) ? null : userId;
        return Result.ok(consultationQueryService.listSessions(owner, keyword, page, size));
    }

    /**
     * 会话详情。
     */
    @GetMapping("/sessions/{sessionId}")
    public Result<ConsultationSessionResponseDTO> getSession(@PathVariable String sessionId) {
        Long userId = currentUserHolder.requireUserId();
        Long dbId = requireSessionId(sessionId);
        Long owner = currentUserHolder.isAdmin() ? null : userId;
        return Result.ok(consultationQueryService.getSession(dbId, owner));
    }

    /**
     * 消息分页：默认取最近 size 条（时间正序返回），beforeId 用于向前翻页。
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> listMessages(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = currentUserHolder.requireUserId();
        Long dbId = requireSessionId(sessionId);
        Long owner = currentUserHolder.isAdmin() ? null : userId;
        return Result.ok(consultationQueryService.listMessages(dbId, owner, beforeId, size));
    }

    private Long requireSessionId(String sessionId) {
        Long id = psychologicalSupportService.extractSessionId(sessionId);
        if (id == null) {
            throw new com.wzu.aispringboot.exception.BusinessException("sessionId 格式错误");
        }
        return id;
    }

    /** 把内部事件映射为 SSE（event 名 + JSON data） */
    private ServerSentEvent<String> toSse(ChatStreamEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        switch (event.getType()) {
            case MESSAGE -> payload.put("content", event.getContent());
            case ERROR -> {
                payload.put("code", event.getCode() == null ? ResultCode.ERROR.getCode() : event.getCode());
                payload.put("msg", event.getContent());
            }
            default -> {
                // thinking / done 无载荷
            }
        }
        return ServerSentEvent.<String>builder()
                .event(event.getType().value())
                .data(payload.isEmpty() ? "{}" : JSONUtil.toJsonStr(payload))
                .build();
    }
}
