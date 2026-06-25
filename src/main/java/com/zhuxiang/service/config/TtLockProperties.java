package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通通锁开放平台配置。
 */
@Component
@ConfigurationProperties(prefix = "ttlock")
public class TtLockProperties {

    /**
     * 通通锁开放平台应用ID。
     */
    private String clientId;

    /**
     * 通通锁开放平台应用密钥。
     */
    private String clientSecret;

    /**
     * 通通锁开放平台基础地址。
     */
    private String baseUrl = "https://api.sciener.com";

    /**
     * 通通锁账号。
     */
    private String username;

    /**
     * 通通锁账号密码。
     */
    private String password;

    /**
     * 临时访问令牌，未配置账号密码时可作为兜底。
     */
    private String accessToken;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
