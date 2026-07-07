package com.zhuxiang.service.immersive.controller.admin;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.immersive.dto.request.SetEntryImageRequest;
import com.zhuxiang.service.immersive.dto.request.SortImagesRequest;
import com.zhuxiang.service.immersive.dto.request.UpdateImmersiveImageRequest;
import com.zhuxiang.service.immersive.dto.response.ImmersiveImageResponse;
import com.zhuxiang.service.immersive.service.ImmersiveImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequireAuth
@RestController
@RequestMapping("/admin")
@Tag(name = "沉浸式看房-图片管理")
@SecurityRequirement(name = "bearerAuth")
public class AdminImmersiveImageController {

    private final ImmersiveImageService imageService;

    public AdminImmersiveImageController(ImmersiveImageService imageService) { this.imageService = imageService; }

    @PostMapping(value = "/immersive-scenes/{sceneId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片")
    public ApiResponse<List<ImmersiveImageResponse>> uploadImages(@PathVariable String sceneId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "projectionType", required = false) String projectionType,
            HttpServletRequest req) {
        return ApiResponse.success(imageService.uploadImages(sceneId, files, projectionType, CurrentUser.id(req)));
    }

    @GetMapping("/immersive-scenes/{sceneId}/images")
    @Operation(summary = "查询场景图片列表")
    public ApiResponse<List<ImmersiveImageResponse>> listImages(@PathVariable String sceneId) {
        return ApiResponse.success(imageService.listImages(sceneId));
    }

    @PutMapping("/immersive-scenes/{sceneId}/images/sort")
    @Operation(summary = "图片排序")
    public ApiResponse<Void> sortImages(@PathVariable String sceneId,
            @Valid @RequestBody SortImagesRequest request, HttpServletRequest req) {
        imageService.sortImages(sceneId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @PutMapping("/immersive-scenes/{sceneId}/entry-image")
    @Operation(summary = "设置入口图片")
    public ApiResponse<Void> setEntryImage(@PathVariable String sceneId,
            @Valid @RequestBody SetEntryImageRequest request, HttpServletRequest req) {
        imageService.setEntryImage(sceneId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @PutMapping("/immersive-images/{imageId}")
    @Operation(summary = "更新图片元信息")
    public ApiResponse<Void> updateImage(@PathVariable String imageId,
            @Valid @RequestBody UpdateImmersiveImageRequest request, HttpServletRequest req) {
        imageService.updateImage(imageId, request, CurrentUser.id(req));
        return ApiResponse.success(null);
    }

    @DeleteMapping("/immersive-images/{imageId}")
    @Operation(summary = "删除图片")
    public ApiResponse<Void> deleteImage(@PathVariable String imageId,
            @RequestParam(value = "force", defaultValue = "false") boolean force, HttpServletRequest req) {
        imageService.deleteImage(imageId, force, CurrentUser.id(req));
        return ApiResponse.success(null);
    }
}
