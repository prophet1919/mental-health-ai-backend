package com.wzu.aispringboot.controller;

import com.wzu.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.wzu.aispringboot.DTO.response.ConsultationSessionResponseDTO;
import com.wzu.aispringboot.DTO.response.PageResponse;
import com.wzu.aispringboot.common.Result;
import com.wzu.aispringboot.service.ConsultationQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端会话审阅：列表与详情。
 * <p>
 * 访问控制由 SecurityConfig 的 /api/admin/** = ROLE_2 统一完成，
 * 此处不再重复判断角色；但查询仍显式传 ownerUserId=null，语义清晰。
 */
@RestController
@RequestMapping("/api/admin/consultations")
public class AdminConsultationController {

    private final ConsultationQueryService consultationQueryService;

    public AdminConsultationController(ConsultationQueryService consultationQueryService) {
        this.consultationQueryService = consultationQueryService;
    }

    /**
     * 全部用户的会话列表。
     *
     * @param userId 可选，按归属用户筛选
     */
    @GetMapping
    public Result<PageResponse<ConsultationSessionResponseDTO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId) {
        PageResponse<ConsultationSessionResponseDTO> result =
                consultationQueryService.listSessions(userId, keyword, page, size);
        return Result.ok(result);
    }

    /**
     * 会话详情（含消息）。
     */
    @GetMapping("/{sessionId}")
    public Result<PageResponse<ConsultationSessionResponseDTO>> detail(@PathVariable String sessionId) {
        // 复用同一结果类型，前端从 records[0] 取会话信息
        Long dbId = parse(sessionId);
        ConsultationSessionResponseDTO session = consultationQueryService.getSession(dbId, null);
        return Result.ok(PageResponse.of(List.of(session), 1, 1, 1));
    }

    /**
     * 会话消息列表。
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<ConsultationMessageResponseDTO>> messages(
            @PathVariable String sessionId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "100") int size) {
        Long dbId = parse(sessionId);
        return Result.ok(consultationQueryService.listMessages(dbId, null, beforeId, size));
    }

    private Long parse(String sessionId) {
        String raw = sessionId != null && sessionId.startsWith("session_")
                ? sessionId.substring("session_".length())
                : sessionId;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new com.wzu.aispringboot.exception.BusinessException("sessionId 格式错误");
        }
    }
}
