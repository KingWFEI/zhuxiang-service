package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * e签宝人脸认证配置。
 * <p>
 * AppSecret 只能从环境变量读取，不允许在 yml 中写真实值。
 */
@Component
@ConfigurationProperties(prefix = "esign.face")
public class EsignFaceProperties {

    /** e签宝网关地址，沙箱默认 https://smlopenapi.esign.cn */
    private String host = "https://smlopenapi.esign.cn";

    /** e签宝应用 AppId */
    private String appId;

    /** e签宝应用 AppSecret，仅从环境变量注入 */
    private String appSecret;

    /** 人脸认证模式，默认 ESIGN */
    private String mode = "ESIGN";

    /** 认证完成回调地址 */
    private String callbackUrl;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public boolean isConfigured() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}
