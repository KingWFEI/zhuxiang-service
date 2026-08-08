package com.zhuxiang.service.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.config.MessageRealtimeProperties;
import com.zhuxiang.service.event.MessageRealtimeEvent;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisMessageEventBroker implements MessageEventBroker {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final MessageRealtimeProperties properties;

    public RedisMessageEventBroker(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MessageRealtimeProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void publish(MessageRealtimeEvent event) {
        try {
            redisTemplate.convertAndSend(
                    properties.getRedisChannel(),
                    objectMapper.writeValueAsString(event)
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化消息实时事件", exception);
        }
    }
}
