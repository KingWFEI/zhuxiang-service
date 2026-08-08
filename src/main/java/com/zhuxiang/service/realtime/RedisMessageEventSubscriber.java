package com.zhuxiang.service.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.event.MessageRealtimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisMessageEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageEventSubscriber.class);

    private final ObjectMapper objectMapper;
    private final MessageSseHub sseHub;

    public RedisMessageEventSubscriber(ObjectMapper objectMapper, MessageSseHub sseHub) {
        this.objectMapper = objectMapper;
        this.sseHub = sseHub;
    }

    public void onMessage(String payload) {
        try {
            sseHub.broadcast(objectMapper.readValue(payload, MessageRealtimeEvent.class));
        } catch (Exception exception) {
            log.warn("忽略无法解析的Redis消息实时事件: {}", exception.getMessage());
        }
    }
}
