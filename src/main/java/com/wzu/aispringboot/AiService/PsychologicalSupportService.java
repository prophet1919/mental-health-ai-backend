package com.wzu.aispringboot.AiService;

import com.wzu.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.wzu.aispringboot.entity.ConsultationSession;
import com.wzu.aispringboot.service.ConsultationMessageService;
import com.wzu.aispringboot.service.ConsultationSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PsychologicalSupportService {
    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    public StructOutput.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        //创建数据库会话记录
        ConsultationSession consultationSession = consultationSessionService.createSession(userId, createDTO);

        //将初始用户消息保存到Message表
        consultationMessageService.saveUserMessage(consultationSession.getId(), createDTO.getInitialMessage(), null);

        //创建会话信息
        String sessionId = "session_" + consultationSession.getId();
        return new StructOutput.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, //24小时
                1,
                "ACTIVE"
        );
    }

}
