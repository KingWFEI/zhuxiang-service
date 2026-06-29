package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.entity.FileRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface FileRecordService extends IService<FileRecord> {

    FileUploadResponse upload(String userId, MultipartFile file, String bizType);

    /** 上传管理端房源图片并记录文件归属。 */
    FileUploadResponse uploadHouseImage(String operatorId, MultipartFile file);

    void validateFileOwnership(String userId, String url, String bizType);
}
