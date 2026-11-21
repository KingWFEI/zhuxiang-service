package com.zhuxiang.service.service;

public interface SmsRateLimiter {

    RateLimitDecision acquire(String phone, String scene, String clientIp);

    enum LimitType {
        PHONE_SCENE_COOLDOWN(1),
        PHONE_HOURLY(2),
        PHONE_DAILY(3),
        IP_MINUTE(4),
        IP_DAILY(5);

        private final int redisCode;

        LimitType(int redisCode) {
            this.redisCode = redisCode;
        }

        public static LimitType fromRedisCode(int redisCode) {
            for (LimitType type : values()) {
                if (type.redisCode == redisCode) {
                    return type;
                }
            }
            throw new IllegalStateException("未知的短信限流类型: " + redisCode);
        }
    }

    record RateLimitDecision(boolean allowed, long retryAfter, LimitType limitType) {
        public static RateLimitDecision permit() {
            return new RateLimitDecision(true, 0, null);
        }

        public static RateLimitDecision rejected(long retryAfter) {
            return rejected(retryAfter, LimitType.PHONE_SCENE_COOLDOWN);
        }

        public static RateLimitDecision rejected(long retryAfter, LimitType limitType) {
            return new RateLimitDecision(false, Math.max(1, retryAfter), limitType);
        }
    }
}
