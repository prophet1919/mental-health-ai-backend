package com.wzu.aispringboot.AiService;

/**
 * 流式对话事件。统一成单一类型后，Controller 只需把 type 映射为 SSE 的 event 名，
 * 前端按 event 分派渲染（thinking / message / done / error）。
 */
public final class ChatStreamEvent {

    /** 事件类型：thinking=模型推理中，message=正文片段，done=结束，error=失败 */
    public enum Type {
        THINKING("thinking"),
        MESSAGE("message"),
        DONE("done"),
        ERROR("error");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private final Type type;
    private final String content;
    private final String code;

    private ChatStreamEvent(Type type, String content, String code) {
        this.type = type;
        this.content = content;
        this.code = code;
    }

    public static ChatStreamEvent thinking() {
        return new ChatStreamEvent(Type.THINKING, null, null);
    }

    public static ChatStreamEvent message(String fragment) {
        return new ChatStreamEvent(Type.MESSAGE, fragment, null);
    }

    public static ChatStreamEvent done() {
        return new ChatStreamEvent(Type.DONE, null, null);
    }

    public static ChatStreamEvent error(String code, String message) {
        return new ChatStreamEvent(Type.ERROR, message, code);
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getCode() {
        return code;
    }
}
