package com.wzu.aispringboot.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.wzu.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.wzu.aispringboot.entity.ConsultationSession;
import com.wzu.aispringboot.entity.User;
import com.wzu.aispringboot.exception.BusinessException;
import com.wzu.aispringboot.mapper.ConsultationSessionMapper;
import com.wzu.aispringboot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ConsultationSessionService {

    private final UserMapper userMapper;
    private final ConsultationSessionMapper consultationSessionMapper;

    public ConsultationSessionService(UserMapper userMapper,
                                      ConsultationSessionMapper consultationSessionMapper) {
        this.userMapper = userMapper;
        this.consultationSessionMapper = consultationSessionMapper;
    }

    /**
     * 创建会话。只建会话记录，不写消息（消息由对话接口写入）。
     *
     * @throws BusinessException 用户不存在时抛出，而不是返回 null
     */
    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在，无法创建会话");
        }

        ConsultationSession session = ConsultationSession.builder()
                .userId(userId)
                .sessionTitle(createDTO == null ? null : createDTO.getSessionTitle())
                .startedAt(LocalDateTime.now())
                .build();

        // 未提供标题时生成默认标题
        if (StrUtil.isBlank(session.getSessionTitle())) {
            session.setSessionTitle("小暖助手 - " + DateUtil.format(LocalDateTime.now(), "yyyy-MM-dd HH:mm"));
        }

        consultationSessionMapper.insert(session);
        return session;
    }
}
