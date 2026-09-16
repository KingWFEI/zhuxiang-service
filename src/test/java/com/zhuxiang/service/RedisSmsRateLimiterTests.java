package com.zhuxiang.service;

import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.service.SmsRateLimiter;
import com.zhuxiang.service.service.impl.RedisSmsRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class RedisSmsRateLimiterTests {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void scriptArgumentsMustBeCompatibleWithStringRedisSerializer() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisSerializer serializer = new StringRedisSerializer();
        doAnswer(invocation -> {
            Object[] invocationArguments = invocation.getArguments();
            for (int index = 2; index < invocationArguments.length; index++) {
                serializer.serialize(invocationArguments[index]);
            }
            return 0L;
        }).when(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                any(Object[].class)
        );

        RedisSmsRateLimiter limiter = new RedisSmsRateLimiter(
                redisTemplate,
                new SmsCodeProperties()
        );

        SmsRateLimiter.RateLimitDecision decision =
                limiter.acquire("13900000000", "login", "127.0.0.1");

        assertThat(decision.allowed()).isTrue();
    }
}
