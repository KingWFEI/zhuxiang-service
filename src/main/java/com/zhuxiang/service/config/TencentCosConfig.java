package com.zhuxiang.service.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 腾讯云 COS 客户端配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "cos")
@ConditionalOnProperty(name = "app.storage.cos.secret-id", matchIfMissing = false)
public class TencentCosConfig {

    /**
     * 创建线程安全的 COS 客户端，并在应用停止时关闭连接池。
     */
    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient(ObjectStorageProperties properties) {
        ObjectStorageProperties.Cos cos = properties.getCos();
        requireText(cos.getSecretId(), "TENCENT_COS_SECRET_ID 未配置");
        requireText(cos.getSecretKey(), "TENCENT_COS_SECRET_KEY 未配置");
        requireText(cos.getRegion(), "TENCENT_COS_REGION 未配置");
        requireText(cos.getBucket(), "TENCENT_COS_BUCKET 未配置");

        COSCredentials credentials = StringUtils.hasText(cos.getSessionToken())
                ? new BasicSessionCredentials(cos.getSecretId(), cos.getSecretKey(), cos.getSessionToken())
                : new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cos.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        return new COSClient(credentials, clientConfig);
    }

    /** 校验 COS 必填配置。 */
    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(message);
        }
    }
}
