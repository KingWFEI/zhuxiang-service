package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 通通锁OAuth令牌响应。
 */
public class TtLockOAuthTokenResponse {

    /**
     * OAuth访问令牌。
     */
    @JsonAlias("access_token")
    private String accessToken;

    /**
     * OAuth刷新令牌。
     */
    @JsonAlias("refresh_token")
    private String refreshToken;

    /**
     * 访问令牌有效期，单位秒。
     */
    @JsonAlias("expires_in")
    private Long expiresIn;

    /**
     * OAuth错误码。
     */
    @JsonAlias({"errcode", "error", "error_code"})
    private String errorCode;

    /**
     * OAuth错误描述。
     */
    @JsonAlias({"errmsg", "error_description", "error_msg"})
    private String errorMessage;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
