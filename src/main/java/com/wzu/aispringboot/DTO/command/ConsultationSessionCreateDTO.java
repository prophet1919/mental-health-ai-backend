package com.wzu.aispringboot.DTO.command;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建会话入参。
 * initialMessage 不再是必填项——本设计下"创建会话"只建空会话，
 * 首轮对话由 /stream 发起，避免同一句话被写两次。
 */
@Data
public class ConsultationSessionCreateDTO {

    @Size(max = 200, message = "会话标题不能超过200个字符")
    private String sessionTitle;
}
