package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片对象存储配置。
 */
@Component
@ConfigurationProperties(prefix = "app.storage")
public class ObjectStorageProperties {

    /** 存储类型：local 或 cos。 */
    private String type = "local";

    /** 腾讯云 COS 配置。 */
    private final Cos cos = new Cos();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Cos getCos() {
        return cos;
    }

    /**
     * 腾讯云 COS 连接与访问地址配置。
     */
    public static class Cos {
        private String secretId;
        private String secretKey;
        private String sessionToken;
        private String region;
        private String bucket;
        private String publicBaseUrl;
        private String keyPrefix;

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public void setSessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }
    }
}
