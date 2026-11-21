package com.wzu.aispringboot.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.wzu.aispringboot.AiService.PsychologicalSupportService;
import com.wzu.aispringboot.AiService.StructOutput;
import com.wzu.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.wzu.aispringboot.common.Result;
import com.wzu.aispringboot.util.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/psychological-chat")
public class PsychologicalChat {
    @Autowired
    private  PsychologicalSupportService psychologicalSupportService;

    @PostMapping("/session/start")
    public Result<StructOutput.StreamChatSession> startSession(@Valid @RequestBody ConsultationSessionCreateDTO createDTO) {
        //获取当前用户
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        StructOutput.StreamChatSession session = psychologicalSupportService.startSession(userId, createDTO);
        return Result.ok(session);
    }

}
