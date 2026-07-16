package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.RealNameAuthDtos;

/**
 * 个人实名认证业务服务。
 */
public interface RealNameAuthService {

    /**
     * 发起个人实名认证。
     */
    RealNameAuthDtos.StartResult startAuth(String userId, RealNameAuthDtos.StartRequest request);

    /**
     * 重新发起个人实名认证（强制将旧 VERIFYING 记录标记为 EXPIRED）。
     */
    RealNameAuthDtos.StartResult restartAuth(String userId, RealNameAuthDtos.RestartRequest request);

    /**
     * 主动刷新认证结果。
     */
    RealNameAuthDtos.RefreshResult refreshAuth(String userId, String realNameAuthNo);

    /**
     * 查询当前用户实名认证状态。
     */
    RealNameAuthDtos.StatusResult getStatus(String userId);

    /**
     * 判断指定用户是否已完成实名认证。
     */
    boolean isVerified(String userId);

    /**
     * 要求指定用户已完成实名认证，否则抛出业务异常。
     */
    void requireVerified(String userId);

    /**
     * 获取指定用户最新的 VERIFIED 实名认证记录。未认证时返回 null。
     */
    com.zhuxiang.service.entity.UserRealNameAuth getVerifiedRecord(String userId);
}
