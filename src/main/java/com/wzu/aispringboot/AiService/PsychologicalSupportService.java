package com.wzu.aispringboot.AiService;

import com.networknt.schema.SchemaLocation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import com.wzu.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.wzu.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import com.wzu.aispringboot.entity.ConsultationSession;
import com.wzu.aispringboot.service.ConsultationMessageService;
import com.wzu.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

@Service
public class PsychologicalSupportService {
    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

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

    public Flux<String> streamPsychologicalChat(String sessionId, String userMessage) {
        //创建响应流
        return Flux.create(sink -> {
            // sink.next("数据1") // 发送数据1
            // sink.complete() // 发送完成信号
            // sink.error(exception) // 发送错误
            Long dbsessionId = extractSessionId(sessionId);
            if(dbsessionId == null) {
                sink.error(new RuntimeException("sessionId格式错误"));
                return;
            }
            //是否为初始消息
            boolean isInitialMessage = false;
            // 检查是否为初始消息，避免重复处理
            Integer messageCount = consultationMessageService.getMessageCount(dbsessionId);
            if(messageCount == 1) {
                ConsultationMessageResponseDTO lastMessage = consultationMessageService.getLastMessageBySessionId(dbsessionId);
                if (lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())) {
                    isInitialMessage = true;
                }

            }
            if(!isInitialMessage) {
                //保存用户消息到数据库
                consultationMessageService.saveUserMessage(dbsessionId, userMessage, null);
            }

            //进行流式对话
            //生成对话记忆管理
            String conversationId = "conversation_" + sessionId;
            //构建系统提示词
            List<Message> userMessages = new ArrayList<>();
            userMessages.add(new UserMessage(userMessage));
            chatMemory.add(conversationId, userMessages);
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT),
                    new UserMessage(userMessage)

            ));
            //用于存储ai完整的响应
            StringBuilder fullResponse = new StringBuilder();

            //使用chatClient发送消息到openai
            chatClient.prompt(prompt)
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(chatMemory.CONVERSATION_ID, conversationId))
                    .stream()
                    .content()
                    .doOnNext(Fragment -> {
                        fullResponse.append(Fragment);
                        sink.next(Fragment);
                    })
                    .doOnComplete(() ->{
                        String completeResponse = fullResponse.toString();
                        //将ai返回的内容保存到数据库
                        consultationMessageService.saveAIMessage(dbsessionId, completeResponse, "open-ai");
                        //添加ai回复到chatMemory
                        List<Message> aiMessages = new ArrayList<>();
                        aiMessages.add(new AssistantMessage(completeResponse));
                        chatMemory.add(conversationId, aiMessages);
                        sink.complete();
                    })
                    .doOnError(error -> sink.error(error))
                    .subscribe(); // 订阅流，触发数据处理

                });
    }

    //获取参数中的sessionId
    public Long extractSessionId(String sessionId) {

        if(sessionId != null && sessionId.startsWith("session_")) {
            String idStr = sessionId.substring("session_".length());
            return Long.parseLong(idStr);
        }
        return null;
    }

}
