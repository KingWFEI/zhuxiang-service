package com.zhuxiang.service.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.auth.sms")
public class SmsCodeProperties {

    @Min(60)
    private int expiresSeconds = 300;

    @Min(1)
    private int retryAfterSeconds = 60;

    @Min(1)
    private int maxVerifyAttempts = 5;

    @Min(1)
    private int phoneHourlyLimit = 5;

    @Min(1)
    private int phoneDailyLimit = 10;

    @Min(1)
    private int ipMinuteLimit = 10;

    @Min(1)
    private int ipDailyLimit = 50;

    private String rateLimitStore = "memory";
    private String fixedCode = "";
    private boolean exposeCode = false;
    private String provider = "none";
    private Aliyun aliyun = new Aliyun();

    public int getExpiresSeconds() {
        return expiresSeconds;
    }

    public void setExpiresSeconds(int expiresSeconds) {
        this.expiresSeconds = expiresSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(int retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }

    public int getPhoneHourlyLimit() {
        return phoneHourlyLimit;
    }

    public void setPhoneHourlyLimit(int phoneHourlyLimit) {
        this.phoneHourlyLimit = phoneHourlyLimit;
    }

    public int getPhoneDailyLimit() {
        return phoneDailyLimit;
    }

    public void setPhoneDailyLimit(int phoneDailyLimit) {
        this.phoneDailyLimit = phoneDailyLimit;
    }

    public int getIpMinuteLimit() {
        return ipMinuteLimit;
    }

    public void setIpMinuteLimit(int ipMinuteLimit) {
        this.ipMinuteLimit = ipMinuteLimit;
    }

    public int getIpDailyLimit() {
        return ipDailyLimit;
    }

    public void setIpDailyLimit(int ipDailyLimit) {
        this.ipDailyLimit = ipDailyLimit;
    }

    public String getRateLimitStore() {
        return rateLimitStore;
    }

    public void setRateLimitStore(String rateLimitStore) {
        this.rateLimitStore = rateLimitStore;
    }

    public String getFixedCode() {
        return fixedCode;
    }

    public void setFixedCode(String fixedCode) {
        this.fixedCode = fixedCode;
    }

    public boolean isExposeCode() {
        return exposeCode;
    }

    public void setExposeCode(boolean exposeCode) {
        this.exposeCode = exposeCode;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Aliyun getAliyun() {
        return aliyun;
    }

    public void setAliyun(Aliyun aliyun) {
        this.aliyun = aliyun;
    }

    /**
     * 阿里云短信服务配置。模板需只包含一个变量 ${code}。
     */
    public static class Aliyun {
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String endpoint = "dysmsapi.aliyuncs.com";
        private String signName = "";
        private String templateCode = "";

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getSignName() {
            return signName;
        }

        public void setSignName(String signName) {
            this.signName = signName;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }
    }
}
