package com.zhuxiang.service.service;

/**
 * 明文密码查询限流扩展点；分布式部署时可替换为项目统一的 Redis 实现。
 */
public interface PasscodeQueryRateLimiter {

    /** 校验当前用户对指定租约的查询频率。 */
    void check(String userId, String leaseId);
}
