package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.dto.HousePropertyCertificateDtos;
import com.zhuxiang.service.service.HousePropertyCertificateService;
import com.zhuxiang.service.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 房东端房源管理接口。
 */
@RestController
@RequireAuth
@RequestMapping("/landlord/houses")
@Tag(name = "房东房源", description = "房东对自己的房源进行增删改查和上下架")
@SecurityRequirement(name = "bearerAuth")
public class LandlordHouseController {

    private final HouseService houseService;
    private final HousePropertyCertificateService propertyCertificateService;

    public LandlordHouseController(
            HouseService houseService,
            HousePropertyCertificateService propertyCertificateService
    ) {
        this.houseService = houseService;
        this.propertyCertificateService = propertyCertificateService;
    }

    @GetMapping
    @Operation(
            summary = "房源列表",
            description = "返回当前房东未删除的房源，支持 draft、pendingReview、available、rejected、offline 状态筛选。"
    )
    public ApiResponse<List<AdminHouseDtos.AdminHouseView>> listHouses(
            HttpServletRequest request,
            @Parameter(description = "房源状态") @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(houseService.getLandlordHouses(CurrentUser.id(request), status));
    }

    @GetMapping("/{houseId}")
    @Operation(summary = "房源详情", description = "返回房源详情，校验归属权")
    public ApiResponse<AdminHouseDtos.AdminHouseView> getHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId
    ) {
        return ApiResponse.success(
                houseService.getLandlordHouseById(houseId, CurrentUser.id(request))
        );
    }

    @PostMapping
    @Operation(summary = "发布房源", description = "创建新房源，landlordId 从 Token 自动获取。创建后状态为草稿，需调用上架接口发布。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> createHouse(
            HttpServletRequest request,
            @Valid @RequestBody AdminHouseDtos.CreateHouseRequest body
    ) {
        return ApiResponse.success(
                "房源创建成功",
                houseService.createLandlordHouse(body, CurrentUser.id(request))
        );
    }

    @PutMapping("/{houseId}")
    @Operation(summary = "修改房源", description = "仅更新传入的字段，校验房源归属当前房东。设施和标签采用完整替换语义。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> updateHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId,
            @Valid @RequestBody AdminHouseDtos.UpdateHouseRequest body
    ) {
        return ApiResponse.success(
                "房源修改成功",
                houseService.updateLandlordHouse(houseId, body, CurrentUser.id(request))
        );
    }

    @PutMapping("/{houseId}/publish")
    @Operation(
            summary = "提交房源审核",
            description = "房产证已上传后，将草稿、已驳回或已下架房源提交管理员审核，状态变为 pendingReview；已下架房源不会直接恢复上架。"
    )
    public ApiResponse<AdminHouseDtos.AdminHouseView> publishHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId
    ) {
        return ApiResponse.success(
                "房源已提交审核",
                houseService.publishLandlordHouse(houseId, CurrentUser.id(request))
        );
    }

    @PostMapping(value = "/{houseId}/property-certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "上传或替换房产证",
            description = "仅房源所属房东可操作，支持 JPG、PNG、WebP，最大10MB。替换后保留历史记录。"
    )
    public ApiResponse<HousePropertyCertificateDtos.CertificateView> uploadPropertyCertificate(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId,
            @Parameter(description = "房产证图片", required = true)
            @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
                "房产证上传成功",
                propertyCertificateService.uploadForLandlord(
                        houseId, CurrentUser.id(request), file)
        );
    }

    @GetMapping("/{houseId}/property-certificates")
    @Operation(summary = "查看房产证历史", description = "查看当前房源历次上传和审核结果。")
    public ApiResponse<List<HousePropertyCertificateDtos.CertificateView>> listPropertyCertificates(
            HttpServletRequest request,
            @PathVariable String houseId
    ) {
        return ApiResponse.success(propertyCertificateService.listForLandlord(
                houseId, CurrentUser.id(request)));
    }

    @GetMapping("/{houseId}/property-certificates/{certificateId}/file")
    @Operation(summary = "查看房产证文件", description = "鉴权后读取私有存储中的房产证原图。")
    public ResponseEntity<InputStreamResource> openPropertyCertificate(
            HttpServletRequest request,
            @PathVariable String houseId,
            @PathVariable String certificateId
    ) {
        HousePropertyCertificateService.CertificateDownload download =
                propertyCertificateService.openForLandlord(
                        houseId, certificateId, CurrentUser.id(request));
        return fileResponse(download);
    }

    @PutMapping("/{houseId}/offline")
    @Operation(summary = "下架房源", description = "将可租状态的房源下架，不再对外展示。校验归属权。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> offlineHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId
    ) {
        return ApiResponse.success(
                "房源下架成功",
                houseService.offlineLandlordHouse(houseId, CurrentUser.id(request))
        );
    }

    @DeleteMapping("/{houseId}")
    @Operation(
            summary = "删除已下架房源",
            description = "仅房源所属房东可操作，只有 offline 状态可以删除。采用软删除并保留历史合同、订单和审核记录。"
    )
    public ApiResponse<Boolean> deleteHouse(
            HttpServletRequest request,
            @Parameter(description = "房源 ID") @PathVariable String houseId
    ) {
        houseService.deleteLandlordHouse(houseId, CurrentUser.id(request));
        return ApiResponse.success("房源删除成功", true);
    }

    private ResponseEntity<InputStreamResource> fileResponse(
            HousePropertyCertificateService.CertificateDownload download) {
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(download.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new InputStreamResource(download.inputStream()));
    }
}
