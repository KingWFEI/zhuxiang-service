package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.service.ObjectStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 开发和测试使用的本地文件存储，当 COS 不可用时作为兜底实现。
 */
@Service
@ConditionalOnMissingBean(ObjectStorageService.class)
public class LocalObjectStorageService implements ObjectStorageService {

    private final Path uploadDirectory;
    private final String contextPath;

    public LocalObjectStorageService(
            @Value("${app.upload.directory}") String uploadDirectory,
            @Value("${server.servlet.context-path:/api}") String contextPath
    ) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.contextPath = contextPath;
    }

    /** 保存到本地上传目录并返回原有静态资源 URL。 */
    @Override
    public String store(String objectKey, InputStream input, long size, String contentType) {
        Path target = uploadDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("对象路径无效");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return contextPath + "/uploads/" + objectKey.replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件保存失败", exception);
        }
    }
}
