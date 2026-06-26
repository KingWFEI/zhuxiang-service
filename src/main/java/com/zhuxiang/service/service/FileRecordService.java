package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.entity.FileRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface FileRecordService extends IService<FileRecord> {

    FileUploadResponse upload(String userId, MultipartFile file, String bizType);

    void validateFileOwnership(String userId, String url, String bizType);
}
