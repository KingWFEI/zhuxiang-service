package com.zhuxiang.service.service;

public interface SmsRateLimiter {

    RateLimitDecision acquire(String phone, String scene, String clientIp);

    record RateLimitDecision(boolean allowed, long retryAfter) {
        public static RateLimitDecision permit() {
            return new RateLimitDecision(true, 0);
        }

        public static RateLimitDecision rejected(long retryAfter) {
            return new RateLimitDecision(false, Math.max(1, retryAfter));
        }
    }
}
