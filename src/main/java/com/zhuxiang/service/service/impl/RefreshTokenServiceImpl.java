package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.auth.TokenProvider;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.entity.AppUser;
import com.zhuxiang.service.entity.RefreshToken;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.mapper.AppUserMapper;
import com.zhuxiang.service.mapper.RefreshTokenMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
* @author king-wang
* @description 针对表【refresh_token(用户刷新令牌表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:56
*/
@Service
public class RefreshTokenServiceImpl extends ServiceImpl<RefreshTokenMapper, RefreshToken>
    implements RefreshTokenService{

    private final AppUserMapper appUserMapper;
    private final TokenProvider tokenProvider;
    private final long refreshTokenDays;

    public RefreshTokenServiceImpl(
            AppUserMapper appUserMapper,
            TokenProvider tokenProvider,
            @Value("${app.auth.refresh-token-days}") long refreshTokenDays
    ) {
        this.appUserMapper = appUserMapper;
        this.tokenProvider = tokenProvider;
        this.refreshTokenDays = refreshTokenDays;
    }

    /**
     * 校验并轮换刷新令牌。
     */
    @Override
    @Transactional
    public AuthDtos.TokenResult refresh(AuthDtos.RefreshRequest request) {
        RefreshToken current = getOne(
                Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getRefreshToken, request.refreshToken())
                        .last("LIMIT 1"),
                false
        );
        LocalDateTime now = LocalDateTime.now();
        if (current == null || Integer.valueOf(1).equals(current.getRevoked())
                || current.getExpiresAt().isBefore(now)) {
            throw BusinessException.unauthorized("Refresh Token 无效或已过期");
        }
        AppUser user = appUserMapper.selectById(current.getUserId());
        if (user == null || !"active".equals(user.getStatus())) {
            throw BusinessException.unauthorized("用户不存在或状态不可用");
        }
        revoke(current, now);
        RefreshToken next = createRefreshToken(user.getId(), now);
        return new AuthDtos.TokenResult(
                tokenProvider.createAccessToken(user.getId()),
                next.getRefreshToken(),
                tokenProvider.accessTokenSeconds()
        );
    }

    /**
     * 注销当前用户的刷新令牌。
     */
    @Override
    @Transactional
    public boolean logout(String userId, AuthDtos.LogoutRequest request) {
        RefreshToken token = getOne(
                Wrappers.<RefreshToken>lambdaQuery()
                        .eq(RefreshToken::getRefreshToken, request.refreshToken())
                        .eq(RefreshToken::getUserId, userId)
                        .last("LIMIT 1"),
                false
        );
        if (token != null && !Integer.valueOf(1).equals(token.getRevoked())) {
            revoke(token, LocalDateTime.now());
        }
        return true;
    }

    /**
     * 创建并保存新的刷新令牌。
     */
    @Override
    public RefreshToken createRefreshToken(String userId, LocalDateTime now) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID().toString());
        refreshToken.setUserId(userId);
        refreshToken.setRefreshToken(tokenProvider.createRefreshToken());
        refreshToken.setExpiresAt(now.plusDays(refreshTokenDays));
        refreshToken.setRevoked(0);
        refreshToken.setCreatedAt(now);
        save(refreshToken);
        return refreshToken;
    }

    /**
     * 将刷新令牌标记为已撤销。
     */
    private void revoke(RefreshToken refreshToken, LocalDateTime now) {
        refreshToken.setRevoked(1);
        refreshToken.setRevokedAt(now);
        updateById(refreshToken);
    }
}




