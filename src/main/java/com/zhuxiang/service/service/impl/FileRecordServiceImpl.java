package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.entity.FileRecord;
import com.zhuxiang.service.mapper.FileRecordMapper;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter PATH_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final ObjectStorageService objectStorageService;

    public FileRecordServiceImpl(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    /** 校验图片并保存到当前环境配置的对象存储。 */
    @Override
    public FileUploadResponse upload(String userId, MultipartFile file, String bizType) {
        return uploadImage(userId, file, bizType, "id-card");
    }

    /** 上传管理端房源图片到独立对象目录。 */
    @Override
    public FileUploadResponse uploadHouseImage(String operatorId, MultipartFile file) {
        return uploadImage(operatorId, file, "house_image", "house-images");
    }

    /** 执行通用图片校验、对象存储上传和文件记录保存。 */
    private FileUploadResponse uploadImage(
            String userId,
            MultipartFile file,
            String bizType,
            String objectDirectory
    ) {
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
        String objectKey = objectDirectory + "/" + LocalDate.now().format(PATH_DATE)
                + "/" + UUID.randomUUID() + extension;
        try (InputStream input = file.getInputStream()) {
            String url = objectStorageService.store(
                    objectKey, input, file.getSize(), file.getContentType()
            );

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

    /** 校验图片记录属于当前用户和指定业务类型。 */
    @Override
    public void validateFileOwnership(String userId, String url, String bizType) {
        long count = count(Wrappers.<FileRecord>lambdaQuery()
                .eq(FileRecord::getUserId, userId)
                .eq(FileRecord::getUrl, url)
                .eq(FileRecord::getBizType, bizType));
        if (count == 0) {
            throw BusinessException.badRequest("图片无效或不属于当前用户");
        }
    }
}
