package com.zhuxiang.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.realtime.LocalMessageEventBroker;
import com.zhuxiang.service.realtime.MessageEventBroker;
import com.zhuxiang.service.realtime.MessageSseHub;
import com.zhuxiang.service.realtime.RedisMessageEventBroker;
import com.zhuxiang.service.realtime.RedisMessageEventSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
@ConditionalOnProperty(
        name = "app.message.realtime.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MessageRealtimeConfig {

    @Bean
    @ConditionalOnProperty(
            name = "app.message.realtime.broker",
            havingValue = "memory",
            matchIfMissing = true
    )
    public MessageEventBroker localMessageEventBroker(MessageSseHub sseHub) {
        return new LocalMessageEventBroker(sseHub);
    }

    @Bean
    @ConditionalOnProperty(name = "app.message.realtime.broker", havingValue = "redis")
    public MessageEventBroker redisMessageEventBroker(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MessageRealtimeProperties properties
    ) {
        return new RedisMessageEventBroker(redisTemplate, objectMapper, properties);
    }

    @Bean
    @ConditionalOnProperty(name = "app.message.realtime.broker", havingValue = "redis")
    public RedisMessageEventSubscriber redisMessageEventSubscriber(
            ObjectMapper objectMapper,
            MessageSseHub sseHub
    ) {
        return new RedisMessageEventSubscriber(objectMapper, sseHub);
    }

    @Bean
    @ConditionalOnProperty(name = "app.message.realtime.broker", havingValue = "redis")
    public MessageListenerAdapter messageRealtimeListenerAdapter(
            RedisMessageEventSubscriber subscriber
    ) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    @Bean
    @ConditionalOnProperty(name = "app.message.realtime.broker", havingValue = "redis")
    public RedisMessageListenerContainer messageRealtimeListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageRealtimeListenerAdapter,
            MessageRealtimeProperties properties
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                messageRealtimeListenerAdapter,
                new ChannelTopic(properties.getRedisChannel())
        );
        return container;
    }
}
