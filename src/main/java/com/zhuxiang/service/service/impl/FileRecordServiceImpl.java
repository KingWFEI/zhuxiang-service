package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.entity.FileRecord;
import com.zhuxiang.service.mapper.FileRecordMapper;
import com.zhuxiang.service.service.FileRecordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecord>
        implements FileRecordService {

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final Path uploadDirectory;
    private final String contextPath;

    public FileRecordServiceImpl(
            @Value("${app.upload.directory}") String uploadDirectory,
            @Value("${server.servlet.context-path:/api}") String contextPath
    ) {
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.contextPath = contextPath;
    }

    @Override
    public FileUploadResponse upload(String userId, MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("文件大小不能超过 5MB");
        }
        String extension = CONTENT_TYPE_EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw BusinessException.badRequest("仅支持 JPG、PNG、WebP 格式");
        }
        try {
            Path bizDirectory = uploadDirectory.resolve("id-card");
            Files.createDirectories(bizDirectory);
            String filename = UUID.randomUUID().toString() + extension;
            Path target = bizDirectory.resolve(filename).normalize();
            if (!target.startsWith(bizDirectory)) {
                throw BusinessException.badRequest("文件名无效");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String url = contextPath + "/uploads/id-card/" + filename;

            FileRecord record = new FileRecord();
            record.setId(UUID.randomUUID().toString());
            record.setUserId(userId);
            record.setUrl(url);
            record.setBizType(bizType);
            record.setCreatedAt(LocalDateTime.now());
            save(record);

            return new FileUploadResponse(url, record.getId());
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }
    }

    @Override
    public void validateFileOwnership(String userId, String url, String bizType) {
        long count = count(Wrappers.<FileRecord>lambdaQuery()
                .eq(FileRecord::getUserId, userId)
                .eq(FileRecord::getUrl, url)
                .eq(FileRecord::getBizType, bizType));
        if (count == 0) {
            throw BusinessException.badRequest("身份证图片无效或不属于当前用户");
        }
    }
}
