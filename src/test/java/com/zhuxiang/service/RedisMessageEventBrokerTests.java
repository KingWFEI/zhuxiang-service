package com.zhuxiang.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.config.MessageRealtimeProperties;
import com.zhuxiang.service.event.MessageRealtimeEvent;
import com.zhuxiang.service.realtime.RedisMessageEventBroker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisMessageEventBrokerTests {

    @Test
    void eventIsSerializedToConfiguredRedisChannel() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MessageRealtimeProperties properties = new MessageRealtimeProperties();
        properties.setRedisChannel("test:message-events");
        RedisMessageEventBroker broker = new RedisMessageEventBroker(
                redisTemplate, objectMapper, properties
        );
        MessageRealtimeEvent event = MessageRealtimeEvent.changed(
                "user-1", "deleted", "message-1"
        );

        broker.publish(event);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("test:message-events"), payload.capture());
        MessageRealtimeEvent decoded = objectMapper.readValue(
                payload.getValue(), MessageRealtimeEvent.class
        );
        assertThat(decoded.userId()).isEqualTo("user-1");
        assertThat(decoded.operation()).isEqualTo("deleted");
    }
}
