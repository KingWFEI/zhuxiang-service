package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.config.ObjectStorageProperties;
import com.zhuxiang.service.service.ObjectStorageService;
import com.zhuxiang.service.service.PrivateObjectStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘文件存储实现，公开目录、私有目录和访问 URL 均由 app.storage.local 配置。
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorageService
        implements ObjectStorageService, PrivateObjectStorageService {

    private final Path uploadDirectory;
    private final Path privateUploadDirectory;
    private final String contextPath;
    private final String publicPath;

    public LocalObjectStorageService(
            ObjectStorageProperties storageProperties,
            @Value("${server.servlet.context-path:/api}") String contextPath
    ) {
        ObjectStorageProperties.Local local = storageProperties.getLocal();
        this.uploadDirectory = Path.of(local.getDirectory()).toAbsolutePath().normalize();
        this.privateUploadDirectory = Path.of(local.getPrivateDirectory()).toAbsolutePath().normalize();
        if (privateUploadDirectory.startsWith(this.uploadDirectory)) {
            throw new IllegalArgumentException("本地敏感文件目录必须位于公开文件目录之外");
        }
        this.contextPath = normalizeContextPath(contextPath);
        this.publicPath = normalizePublicPath(local.getPublicPath());
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
            return contextPath + publicPath + "/" + objectKey.replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件保存失败", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        Path target = uploadDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("对象路径无效");
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件清理失败", exception);
        }
    }

    @Override
    public String storePrivate(
            String objectKey, InputStream input, long size, String contentType) {
        Path target = privateUploadDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(privateUploadDirectory)) {
            throw new IllegalArgumentException("私有对象路径无效");
        }
        try {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            return objectKey.replace('\\', '/');
        } catch (IOException exception) {
            throw new IllegalStateException("本地敏感文件保存失败", exception);
        }
    }

    @Override
    public StoredPrivateObject openPrivate(String objectKey) {
        Path target = privateUploadDirectory.resolve(objectKey).normalize();
        if (!target.startsWith(privateUploadDirectory) || !Files.isRegularFile(target)) {
            throw new IllegalArgumentException("私有文件不存在");
        }
        try {
            String contentType = Files.probeContentType(target);
            return new StoredPrivateObject(
                    Files.newInputStream(target),
                    Files.size(target),
                    contentType
            );
        } catch (IOException exception) {
            throw new IllegalStateException("本地敏感文件读取失败", exception);
        }
    }

    private static String normalizeContextPath(String path) {
        if (path == null || path.isBlank() || "/".equals(path.trim())) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String normalizePublicPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("必须配置本地公开文件 URL 路径");
        }
        String normalized = path.trim().replace('\\', '/');
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if ("/".equals(normalized) || normalized.contains("..")) {
            throw new IllegalArgumentException("本地公开文件 URL 路径无效");
        }
        return normalized;
    }
}
