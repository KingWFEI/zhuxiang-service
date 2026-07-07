package com.zhuxiang.service.immersive.storage;

import com.zhuxiang.service.immersive.config.ImmersiveStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储，使用原项目 uploads 目录下的 immersive 子目录。
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootPath;
    private final String publicBaseUrl;

    public LocalFileStorageService(ImmersiveStorageProperties properties) {
        this.rootPath = Paths.get(properties.getLocal().getRootPath()).toAbsolutePath().normalize();
        this.publicBaseUrl = properties.getLocal().getPublicBaseUrl();
    }

    @Override
    public StoredFile upload(MultipartFile file, String directory) {
        try {
            Path dir = rootPath.resolve(sanitize(directory));
            Files.createDirectories(dir);
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
            }
            String filename = UUID.randomUUID() + ext;
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            String relativePath = (directory.isEmpty() ? "" : directory + "/") + filename;
            String url = publicBaseUrl + "/" + relativePath;
            log.info("Immersive file saved: {}", relativePath);
            return new StoredFile(url, originalName, file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !isLocalUrl(fileUrl)) return;
        try {
            String relative = fileUrl.substring(publicBaseUrl.length());
            if (relative.startsWith("/")) relative = relative.substring(1);
            Path target = rootPath.resolve(sanitize(relative));
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("删除沉浸式文件失败: {}", fileUrl, e);
        }
    }

    @Override
    public boolean isLocalUrl(String fileUrl) {
        return StringUtils.hasText(fileUrl) && fileUrl.startsWith(publicBaseUrl);
    }

    private String sanitize(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.contains("..")) throw new IllegalArgumentException("非法路径: " + path);
        return normalized;
    }
}
