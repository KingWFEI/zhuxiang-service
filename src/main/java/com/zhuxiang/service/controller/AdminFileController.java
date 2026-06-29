package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.FileUploadResponse;
import com.zhuxiang.service.service.AdminFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端文件上传接口。
 */
@RequireAuth
@RestController
@RequestMapping("/admin/files")
@Tag(name = "管理端文件", description = "管理端房源图片上传")
@SecurityRequirement(name = "bearerAuth")
public class AdminFileController {

    private final AdminFileService adminFileService;

    public AdminFileController(AdminFileService adminFileService) {
        this.adminFileService = adminFileService;
    }

    /** 上传房源展示图片。 */
    @PostMapping("/house-images/upload")
    @Operation(
            summary = "上传房源图片",
            description = "仅管理员、管家或房东可上传 JPG、PNG、WebP 房源图片，最大 5MB。"
    )
    public ApiResponse<FileUploadResponse> uploadHouseImage(
            HttpServletRequest request,
            @Parameter(description = "房源图片文件，最大 5MB", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
                "房源图片上传成功",
                adminFileService.uploadHouseImage(CurrentUser.id(request), file)
        );
    }
}
