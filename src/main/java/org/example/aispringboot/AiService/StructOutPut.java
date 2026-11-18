package org.example.aispringboot.AiService;

public class StructOutPut {
    public record  StreamChatSession(
            String sessionid,
            Long userHash,
            String initialMessage,
            Long startTime,
            Long expireTime,
            Integer messageCount,
            String status

    ){ }
}
