package com.wzu.aispringboot.AiService;

import com.wzu.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import com.wzu.aispringboot.AiService.memory.ChatMemoryStore;
import com.wzu.aispringboot.entity.ConsultationSession;
import com.wzu.aispringboot.service.ConsultationMessageService;
import com.wzu.aispringboot.service.ConsultationSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class PsychologicalSupportService {

    private static final Logger LOG = LoggerFactory.getLogger(PsychologicalSupportService.class);

    private final ChatClient chatClient;
    private final ChatMemoryStore chatMemoryStore;
    private final ConsultationSessionService consultationSessionService;
    private final ConsultationMessageService consultationMessageService;

    /** 上下文条数上限（用户+AI 合计），可通过配置覆盖 */
    @Value("${app.chat-memory.max-messages:20}")
    private int maxContextMessages;

    /** 落库的模型标识 */
    @Value("${app.ai.model-label:deepseek-flash}")
    private String modelLabel;

    public PsychologicalSupportService(@Qualifier("open-ai") ChatClient chatClient,
                                       ChatMemoryStore chatMemoryStore,
                                       ConsultationSessionService consultationSessionService,
                                       ConsultationMessageService consultationMessageService) {
        this.chatClient = chatClient;
        this.chatMemoryStore = chatMemoryStore;
        this.consultationSessionService = consultationSessionService;
        this.consultationMessageService = consultationMessageService;
    }

    /**
     * 创建会话：只建空会话并返回 sessionId，不写任何消息。
     * 消息由 {@link #streamPsychologicalChat} 在真正对话时写入，
     * 避免"创建即落库 + 二次提交去重"的脆弱逻辑。
     */
    public StructOutput.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        ConsultationSession session = consultationSessionService.createSession(userId, createDTO);
        String sessionId = "session_" + session.getId();
        return new StructOutput.StreamChatSession(
                sessionId,
                userId,
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L, //24小时
                0,
                "ACTIVE"
        );
    }

    /**
     * 流式对话。
     * <p>
     * 职责：写用户消息 → 按 sessionId 重建上下文 → 调模型流式输出 → 完成后写 AI 消息。
     * 错误统一转成 {@link ChatStreamEvent#error} 事件，不写 AI 消息落库，
     * 由前端渲染为可重试气泡。
     */
    public Flux<ChatStreamEvent> streamPsychologicalChat(Long sessionId, String userMessage) {
        // 1. 保存用户消息（先落库，模型失败也不丢用户输入）
        consultationMessageService.saveUserMessage(sessionId, userMessage, null);

        // 2. 从上下文存储重建历史（只取本轮之前的内容，避免与本轮重复）
        List<Message> history = buildHistory(sessionId);

        // 3. 组装 Prompt：系统提示词 + 历史 + 本轮用户消息
        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT));
        promptMessages.addAll(history);
        promptMessages.add(new UserMessage(userMessage));

        StringBuilder fullResponse = new StringBuilder();

        // 4. 先发 thinking 事件，前端据此显示"正在思考"，避免推理期空白
        Flux<ChatStreamEvent> thinking = Flux.just(ChatStreamEvent.thinking());

        Flux<ChatStreamEvent> body = chatClient
                .prompt(new Prompt(promptMessages))
                .stream()
                .content()
                .map(fragment -> {
                    fullResponse.append(fragment);
                    return ChatStreamEvent.message(fragment);
                })
                .concatWith(Flux.defer(() -> {
                    String complete = fullResponse.toString();
                    if (complete.isBlank()) {
                        // 模型只产出了推理内容、没有正文：按失败处理，不落库
                        LOG.warn("模型未返回正文 sessionId={}", sessionId);
                        return Flux.just(ChatStreamEvent.error(
                                "MODEL_EMPTY_RESPONSE", "AI 暂时没有生成回复，请重试"));
                    }
                    consultationMessageService.saveAIMessage(sessionId, complete, modelLabel);
                    return Flux.just(ChatStreamEvent.done());
                }))
                .onErrorResume(error -> {
                    // 客户端主动断开（点停止/刷新/关闭页面）：不是服务端故障，不打 ERROR
                    if (isClientDisconnect(error)) {
                        LOG.debug("客户端已断开，终止本轮生成 sessionId={}", sessionId);
                        return Flux.empty();
                    }
                    LOG.error("对话流式输出失败 sessionId={}", sessionId, error);
                    return Flux.just(ChatStreamEvent.error(
                            "MODEL_CALL_FAILED", mapErrorMessage(error)));
                });

        return thinking.concatWith(body);
    }

    /** 判断是否为客户端断开导致的写入失败 */
    private boolean isClientDisconnect(Throwable error) {
        Throwable cur = error;
        while (cur != null) {
            String name = cur.getClass().getName();
            if (name.contains("ClientAbortException")
                    || name.contains("AsyncRequestNotUsableException")
                    || name.contains("AbortedException")) {
                return true;
            }
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("Broken pipe")
                    || msg.contains("Connection reset")
                    || msg.contains("response has already been committed"))) {
                return true;
            }
            cur = cur.getCause() == cur ? null : cur.getCause();
        }
        return false;
    }

    /**
     * 按会话重建上下文：从存储取最近 N 条，转成 Spring AI 的 Message。
     */
    private List<Message> buildHistory(Long sessionId) {
        List<ChatMemoryStore.StoredMessage> stored =
                chatMemoryStore.loadRecent(sessionId, Math.max(1, maxContextMessages));
        List<Message> history = new ArrayList<>(stored.size());
        for (ChatMemoryStore.StoredMessage m : stored) {
            if (m.isUser()) {
                history.add(new UserMessage(m.content()));
            } else {
                history.add(new AssistantMessage(m.content()));
            }
        }
        return history;
    }

    /** 把底层异常翻译成面向用户的中文提示 */
    private String mapErrorMessage(Throwable error) {
        String raw = error.getMessage() == null ? "" : error.getMessage();
        String lower = raw.toLowerCase();
        if (lower.contains("401") || lower.contains("unauthorized") || lower.contains("invalid api key")) {
            return "AI 服务鉴权失败，请联系管理员检查密钥配置";
        }
        if (lower.contains("402") || lower.contains("insufficient") || lower.contains("balance")) {
            return "AI 服务额度不足，请联系管理员充值";
        }
        if (lower.contains("429") || lower.contains("rate limit")) {
            return "请求过于频繁，请稍后再试";
        }
        if (lower.contains("timeout") || lower.contains("timed out")) {
            return "AI 响应超时，请重试";
        }
        return "AI 服务暂时不可用，请稍后重试";
    }

    /** 解析外部 sessionId（session_12）为数据库 ID */
    public Long extractSessionId(String sessionId) {
        if (sessionId != null && sessionId.startsWith("session_")) {
            String idStr = sessionId.substring("session_".length());
            try {
                return Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
