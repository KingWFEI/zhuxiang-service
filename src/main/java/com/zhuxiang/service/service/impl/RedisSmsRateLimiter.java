package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.service.SmsRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.auth.sms.rate-limit-store", havingValue = "redis")
public class RedisSmsRateLimiter implements SmsRateLimiter {

    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>("""
            local retry = 0
            for i = 1, #KEYS do
                local argIndex = (i - 1) * 2 + 1
                local limit = tonumber(ARGV[argIndex])
                local window = tonumber(ARGV[argIndex + 1])
                local current = tonumber(redis.call('GET', KEYS[i]) or '0')
                if current >= limit then
                    local ttl = redis.call('TTL', KEYS[i])
                    if ttl < 1 then ttl = window end
                    if ttl > retry then retry = ttl end
                end
            end
            if retry > 0 then return retry end
            for i = 1, #KEYS do
                local argIndex = (i - 1) * 2 + 1
                local window = tonumber(ARGV[argIndex + 1])
                local current = redis.call('INCR', KEYS[i])
                if current == 1 then redis.call('EXPIRE', KEYS[i], window) end
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SmsCodeProperties properties;

    public RedisSmsRateLimiter(StringRedisTemplate redisTemplate, SmsCodeProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public RateLimitDecision acquire(String phone, String scene, String clientIp) {
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        List<String> keys = List.of(
                key("cooldown", scene + ":" + phone),
                key("phone-hour", phone),
                key("phone-day", phone),
                key("ip-minute", ip),
                key("ip-day", ip)
        );
        Object[] args = {
                1, properties.getRetryAfterSeconds(),
                properties.getPhoneHourlyLimit(), 3600,
                properties.getPhoneDailyLimit(), 86400,
                properties.getIpMinuteLimit(), 60,
                properties.getIpDailyLimit(), 86400
        };
        try {
            Long retryAfter = redisTemplate.execute(ACQUIRE_SCRIPT, keys, args);
            if (retryAfter == null) {
                throw BusinessException.serviceUnavailable("验证码服务暂时不可用，请稍后重试");
            }
            return retryAfter > 0
                    ? RateLimitDecision.rejected(retryAfter)
                    : RateLimitDecision.permit();
        } catch (DataAccessException exception) {
            throw BusinessException.serviceUnavailable("验证码服务暂时不可用，请稍后重试");
        }
    }

    private String key(String bucket, String value) {
        return "wuyou:sms:" + bucket + ":" + value;
    }
}
