package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.service.FileRecordService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RequireAuth
@RestController
public class FileController {

    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("id_card_front", "id_card_back");

    private final FileRecordService fileRecordService;

    public FileController(FileRecordService fileRecordService) {
        this.fileRecordService = fileRecordService;
    }

    @PostMapping("/files/upload")
    public ApiResponse<FileUploadResponse> upload(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam("bizType") String bizType
    ) {
        if (!ALLOWED_BIZ_TYPES.contains(bizType)) {
            throw BusinessException.badRequest("不支持的业务类型");
        }
        FileUploadResponse result = fileRecordService.upload(
                CurrentUser.id(request), file, bizType
        );
        return ApiResponse.success(result);
    }
}
