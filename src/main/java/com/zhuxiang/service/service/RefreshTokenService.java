package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.RefreshToken;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.AuthDtos;

import java.time.LocalDateTime;

/**
* @author king-wang
* @description 针对表【refresh_token(用户刷新令牌表)】的数据库操作Service
* @createDate 2026-06-12 19:57:56
*/
public interface RefreshTokenService extends IService<RefreshToken> {

    /**
     * 使用刷新令牌获取新令牌。
     */
    AuthDtos.TokenResult refresh(AuthDtos.RefreshRequest request);

    /**
     * 注销用户当前刷新令牌。
     */
    boolean logout(String userId, AuthDtos.LogoutRequest request);

    /**
     * 创建用户刷新令牌。
     */
    RefreshToken createRefreshToken(String userId, LocalDateTime now);
}
