package com.zhuxiang.service;

import com.zhuxiang.service.config.SmsCodeProperties;
import com.zhuxiang.service.service.SmsRateLimiter;
import com.zhuxiang.service.service.impl.InMemorySmsRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySmsRateLimiterTests {

    @Test
    void samePhoneAndSceneMustWaitForCooldown() {
        SmsCodeProperties properties = new SmsCodeProperties();
        InMemorySmsRateLimiter limiter = new InMemorySmsRateLimiter(
                properties,
                Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(limiter.acquire("13800138000", "login", "127.0.0.1").allowed())
                .isTrue();
        SmsRateLimiter.RateLimitDecision rejected =
                limiter.acquire("13800138000", "login", "127.0.0.1");

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(60);
    }

    @Test
    void changingSceneCannotBypassPhoneHourlyLimit() {
        SmsCodeProperties properties = new SmsCodeProperties();
        properties.setPhoneHourlyLimit(1);
        InMemorySmsRateLimiter limiter = new InMemorySmsRateLimiter(
                properties,
                Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(limiter.acquire("13800138000", "login", "127.0.0.1").allowed())
                .isTrue();
        SmsRateLimiter.RateLimitDecision rejected =
                limiter.acquire("13800138000", "register", "127.0.0.2");

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfter()).isEqualTo(3600);
    }
}
