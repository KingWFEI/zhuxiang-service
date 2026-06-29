package com.zhuxiang.service.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.zhuxiang.service.config.ObjectStorageProperties;
import com.zhuxiang.service.service.ObjectStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 腾讯云 COS 图片存储实现。
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "cos")
public class TencentCosObjectStorageService implements ObjectStorageService {

    private final COSClient cosClient;
    private final ObjectStorageProperties.Cos properties;

    public TencentCosObjectStorageService(COSClient cosClient, ObjectStorageProperties properties) {
        this.cosClient = cosClient;
        this.properties = properties.getCos();
    }

    /** 使用已知长度输入流上传小图片并返回 COS 或 CDN URL。 */
    @Override
    public String store(String objectKey, InputStream input, long size, String contentType) {
        String cosKey = withPrefix(objectKey);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        try {
            cosClient.putObject(new PutObjectRequest(properties.getBucket(), cosKey, input, metadata));
            return buildPublicUrl(cosKey);
        } catch (CosClientException exception) {
            throw new IllegalStateException("图片上传腾讯云 COS 失败");
        }
    }

    /** 追加统一对象键前缀。 */
    private String withPrefix(String objectKey) {
        if (!StringUtils.hasText(properties.getKeyPrefix())) {
            return objectKey;
        }
        String prefix = trimSlashes(properties.getKeyPrefix().trim());
        return prefix.isEmpty() ? objectKey : prefix + "/" + objectKey;
    }

    /** 构造稳定的对象访问 URL。 */
    private String buildPublicUrl(String objectKey) {
        String baseUrl = StringUtils.hasText(properties.getPublicBaseUrl())
                ? properties.getPublicBaseUrl().trim()
                : "https://" + properties.getBucket() + ".cos."
                + properties.getRegion() + ".myqcloud.com";
        return trimTrailingSlash(baseUrl) + "/" + objectKey;
    }

    /** 去除对象键前后的斜杠。 */
    private String trimSlashes(String value) {
        return value.replaceAll("^/+|/+$", "");
    }

    /** 去除访问域名结尾斜杠。 */
    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
