package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.service.FileRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RequireAuth
@RestController
@Tag(name = "文件", description = "业务文件上传")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of("id_card_front", "id_card_back");

    private final FileRecordService fileRecordService;

    public FileController(FileRecordService fileRecordService) {
        this.fileRecordService = fileRecordService;
    }

    @PostMapping("/files/upload")
    @Operation(summary = "上传实名认证图片", description = "上传不超过 5MB 的身份证人像面或国徽面图片，返回可访问 URL 和文件 ID。")
    public ApiResponse<FileUploadResponse> upload(
            HttpServletRequest request,
            @Parameter(description = "待上传的图片文件，最大 5MB", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "业务类型：id_card_front-身份证人像面，id_card_back-身份证国徽面", example = "id_card_front", required = true)
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
