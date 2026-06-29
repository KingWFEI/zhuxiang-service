package com.zhuxiang.service.service.impl;

import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.service.AdminFileService;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.Locale;

/**
 * 管理端房源图片上传实现。
 */
@Service
public class AdminFileServiceImpl implements AdminFileService {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");
    private final UserService userService;
    private final FileRecordService fileRecordService;

    public AdminFileServiceImpl(UserService userService, FileRecordService fileRecordService) {
        this.userService = userService;
        this.fileRecordService = fileRecordService;
    }

    /** 仅允许管理端角色上传房源图片。 */
    @Override
    public FileUploadResponse uploadHouseImage(String operatorId, MultipartFile file) {
        User operator = userService.requireActiveUser(operatorId);
        if (operator.getRole() == null
                || !ADMIN_ROLES.contains(operator.getRole().toUpperCase(Locale.ROOT))) {
            throw BusinessException.forbidden("当前账号无权上传房源图片");
        }
        return fileRecordService.uploadHouseImage(operatorId, file);
    }
}
