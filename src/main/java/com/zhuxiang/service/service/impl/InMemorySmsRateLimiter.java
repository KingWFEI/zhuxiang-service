package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.service.SmsRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.auth.sms.rate-limit-store", havingValue = "memory", matchIfMissing = true)
public class InMemorySmsRateLimiter implements SmsRateLimiter {

    private final SmsCodeProperties properties;
    private final Clock clock;
    private final Map<String, Counter> counters = new HashMap<>();

    @Autowired
    public InMemorySmsRateLimiter(SmsCodeProperties properties) {
        this(properties, Clock.systemUTC());
    }

    public InMemorySmsRateLimiter(SmsCodeProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public synchronized RateLimitDecision acquire(String phone, String scene, String clientIp) {
        long now = clock.millis();
        String ip = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        List<Limit> limits = List.of(
                new Limit("cooldown:" + scene + ":" + phone, 1, properties.getRetryAfterSeconds()),
                new Limit("phone-hour:" + phone, properties.getPhoneHourlyLimit(), 3600),
                new Limit("phone-day:" + phone, properties.getPhoneDailyLimit(), 86400),
                new Limit("ip-minute:" + ip, properties.getIpMinuteLimit(), 60),
                new Limit("ip-day:" + ip, properties.getIpDailyLimit(), 86400)
        );

        long retryAfter = 0;
        for (Limit limit : limits) {
            Counter counter = counters.get(limit.key());
            if (counter != null && counter.expiresAtMillis() <= now) {
                counters.remove(limit.key());
                counter = null;
            }
            if (counter != null && counter.count() >= limit.maxCount()) {
                retryAfter = Math.max(retryAfter,
                        (counter.expiresAtMillis() - now + 999) / 1000);
            }
        }
        if (retryAfter > 0) {
            return RateLimitDecision.rejected(retryAfter);
        }

        for (Limit limit : limits) {
            Counter counter = counters.get(limit.key());
            if (counter == null) {
                counters.put(limit.key(), new Counter(1, now + limit.windowSeconds() * 1000L));
            } else {
                counters.put(limit.key(), new Counter(counter.count() + 1, counter.expiresAtMillis()));
            }
        }
        return RateLimitDecision.permit();
    }

    private record Limit(String key, int maxCount, int windowSeconds) {
    }

    private record Counter(int count, long expiresAtMillis) {
    }
}
