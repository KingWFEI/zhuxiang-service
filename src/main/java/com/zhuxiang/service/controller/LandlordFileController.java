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
 * 房东端文件上传接口。
 */
@RestController
@RequireAuth
@RequestMapping("/landlord/files")
@Tag(name = "房东文件", description = "房东房源图片上传")
@SecurityRequirement(name = "bearerAuth")
public class LandlordFileController {

    private final AdminFileService adminFileService;

    public LandlordFileController(AdminFileService adminFileService) {
        this.adminFileService = adminFileService;
    }

    @PostMapping("/house-images/upload")
    @Operation(summary = "上传房源图片", description = "房东上传 JPG、PNG、WebP 房源图片，最大 5MB。")
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
