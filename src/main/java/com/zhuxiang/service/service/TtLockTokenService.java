package com.zhuxiang.service.service;

/**
 * 通通锁访问令牌服务。
 */
public interface TtLockTokenService {

    /**
     * 获取当前有效的通通锁accessToken。
     */
    String getAccessToken();
}
