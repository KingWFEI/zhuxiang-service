package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.LockPasscodeProperties;
import com.zhuxiang.service.service.PasscodeQueryRateLimiter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单实例轻量限流实现，不引入额外依赖；多实例部署时替换接口实现。
 */
@Component
public class InMemoryPasscodeQueryRateLimiter implements PasscodeQueryRateLimiter {

    private static final long WINDOW_SECONDS = 60;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final LockPasscodeProperties properties;

    public InMemoryPasscodeQueryRateLimiter(LockPasscodeProperties properties) {
        this.properties = properties;
    }

    /** 校验固定窗口内的密码操作次数。 */
    @Override
    public void check(String userId, String leaseId) {
        int limit = properties.getQueryLimitPerMinute();
        if (limit <= 0) {
            return;
        }
        long now = Instant.now().getEpochSecond();
        String key = userId + ":" + leaseId;
        AtomicBoolean rejected = new AtomicBoolean(false);
        windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt() >= WINDOW_SECONDS) {
                return new Window(now, 1);
            }
            if (current.count() >= limit) {
                rejected.set(true);
                return current;
            }
            return new Window(current.startedAt(), current.count() + 1);
        });
        if (rejected.get()) {
            throw BusinessException.tooManyRequests("密码操作过于频繁，请稍后再试");
        }
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= WINDOW_SECONDS);
        }
    }

    private record Window(long startedAt, int count) {
    }
}
