package com.zhuxiang.service.service;

import com.zhuxiang.service.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版 POI 搜索限流器，单用户最多 30 次/分钟。
 */
@Component
public class PoiRateLimiter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void check(String userId) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.compute(userId, (key, existing) -> {
            if (existing == null || now - existing.windowStart > 60_000) {
                return new Bucket(now, 1);
            }
            existing.count++;
            return existing;
        });
        if (bucket.count > MAX_REQUESTS_PER_MINUTE) {
            throw new BusinessException(429, "请求过于频繁，请稍后再试");
        }
    }

    private static class Bucket {
        final long windowStart;
        int count;

        Bucket(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
