package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 门锁期限密码安全配置。
 */
@Component
@ConfigurationProperties(prefix = "app.lock-passcode")
public class LockPasscodeProperties {

    /** Base64 编码的 AES 密钥，仅允许从安全配置注入。 */
    private String encryptionKey;

    /** 当前密钥版本，用于后续密钥轮换。 */
    private String keyVersion = "1";

    /** 单用户单租约每分钟允许查看明文密码的次数。 */
    private int queryLimitPerMinute = 10;

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(String keyVersion) {
        this.keyVersion = keyVersion;
    }

    public int getQueryLimitPerMinute() {
        return queryLimitPerMinute;
    }

    public void setQueryLimitPerMinute(int queryLimitPerMinute) {
        this.queryLimitPerMinute = queryLimitPerMinute;
    }
}
