package com.zhuxiang.service.service;

import com.zhuxiang.service.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端文件服务。
 */
public interface AdminFileService {

    /** 校验管理端角色后上传房源图片。 */
    FileUploadResponse uploadHouseImage(String operatorId, MultipartFile file);
}
