package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "esign")
public class EsignV3Properties {

    private boolean enabled = true;
    private String baseUrl = "https://smlopenapi.esign.cn";
    private String appId;
    private String appSecret;
    private boolean autoFinish = true;
    private String signOrderMode = "SIMULTANEOUS";
    private String notifyUrl;
    private String redirectUrl;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public boolean isAutoFinish() { return autoFinish; }
    public void setAutoFinish(boolean autoFinish) { this.autoFinish = autoFinish; }

    public String getSignOrderMode() { return signOrderMode; }
    public void setSignOrderMode(String signOrderMode) { this.signOrderMode = signOrderMode; }

    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }

    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }

    public boolean isCredentialsConfigured() {
        return enabled && appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}
