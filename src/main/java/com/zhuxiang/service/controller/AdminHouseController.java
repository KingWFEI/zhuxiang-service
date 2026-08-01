package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseDtos;
import com.zhuxiang.service.dto.HousePropertyCertificateDtos;
import com.zhuxiang.service.service.HousePropertyCertificateService;
import com.zhuxiang.service.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端房源管理接口。
 */
@RestController
@RequireAuth
@RequestMapping("/admin/houses")
@Tag(name = "管理端房源", description = "管理端创建和查询房源")
@SecurityRequirement(name = "bearerAuth")
public class AdminHouseController {

    private final HouseService houseService;
    private final HousePropertyCertificateService propertyCertificateService;

    public AdminHouseController(
            HouseService houseService,
            HousePropertyCertificateService propertyCertificateService
    ) {
        this.houseService = houseService;
        this.propertyCertificateService = propertyCertificateService;
    }

    /**
     * 新增房源。
     */
    @PostMapping
    @Operation(summary = "新增房源", description = "在同一事务中创建房源，并关联已上传图片、设施和标签。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> createHouse(
            HttpServletRequest servletRequest,
            @Valid @RequestBody AdminHouseDtos.CreateHouseRequest request
    ) {
        return ApiResponse.success(
                "房源创建成功",
                houseService.createHouse(request, CurrentUser.id(servletRequest))
        );
    }

    /**
     * 获取所有房源详情（含智能锁绑定信息）。
     */
    @GetMapping
    @Operation(summary = "查询全部房源", description = "返回所有房源详情以及每套房源当前绑定的智能锁信息。")
    public ApiResponse<List<AdminHouseDtos.AdminHouseView>> getAllHouses() {
        return ApiResponse.success(houseService.getAllHousesWithLockInfo());
    }

    /**
     * 根据房源 ID 获取管理端房源详情。
     */
    @GetMapping("/{houseId}")
    @Operation(summary = "查询单条房源", description = "根据房源ID返回房源详情、图片列表和智能锁绑定信息。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> getHouse(
            @PathVariable String houseId
    ) {
        return ApiResponse.success(houseService.getAdminHouseById(houseId));
    }

    /**
     * 发布房源（草稿 → 可租）。
     */
    @PutMapping("/{houseId}/publish")
    @Operation(summary = "发布房源", description = "将草稿状态的房源改为可租状态，对外公开可见。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> publishHouse(
            @PathVariable String houseId
    ) {
        return ApiResponse.success("房源发布成功", houseService.publishHouse(houseId));
    }

    /**
     * 下架房源（可租/草稿 → 下架）。
     */
    @PutMapping("/{houseId}/offline")
    @Operation(summary = "下架房源", description = "将可租或草稿状态的房源下架，不再对外展示。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> offlineHouse(
            @PathVariable String houseId
    ) {
        return ApiResponse.success("房源下架成功", houseService.offlineHouse(houseId));
    }

    /**
     * 重新上架房源（下架 → 可租）。
     */
    @PutMapping("/{houseId}/online")
    @Operation(summary = "重新上架房源", description = "将已下架房源恢复为可租状态，对外重新展示。")
    public ApiResponse<AdminHouseDtos.AdminHouseView> onlineHouse(
            @PathVariable String houseId
    ) {
        return ApiResponse.success("房源重新上架成功", houseService.onlineHouse(houseId));
    }

    @GetMapping("/{houseId}/property-certificates")
    @Operation(summary = "查看房产证历史", description = "管理员或管家查看房源历次房产证及审核记录。")
    public ApiResponse<List<HousePropertyCertificateDtos.CertificateView>> listPropertyCertificates(
            HttpServletRequest request,
            @PathVariable String houseId
    ) {
        return ApiResponse.success(propertyCertificateService.listForAdmin(
                houseId, CurrentUser.id(request)));
    }

    @GetMapping("/{houseId}/property-certificates/{certificateId}/file")
    @Operation(summary = "查看房产证文件", description = "管理员或管家鉴权后读取私有房产证原图。")
    public ResponseEntity<InputStreamResource> openPropertyCertificate(
            HttpServletRequest request,
            @PathVariable String houseId,
            @PathVariable String certificateId
    ) {
        HousePropertyCertificateService.CertificateDownload download =
                propertyCertificateService.openForAdmin(
                        houseId, certificateId, CurrentUser.id(request));
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

    @PutMapping("/{houseId}/review")
    @Operation(
            summary = "审核房东房源",
            description = "待审核房源通过后自动变为 available，驳回后变为 rejected；重复同结果审核幂等。"
    )
    public ApiResponse<AdminHouseDtos.AdminHouseView> reviewLandlordHouse(
            HttpServletRequest request,
            @PathVariable String houseId,
            @Valid @RequestBody HousePropertyCertificateDtos.ReviewRequest body
    ) {
        return ApiResponse.success(
                "APPROVE".equals(body.action()) ? "房源审核通过" : "房源已驳回",
                houseService.reviewLandlordHouse(
                        houseId, body, CurrentUser.id(request))
        );
    }

    /**
     * 修改房源信息。
     */
    @PutMapping("/{houseId}")
    @Operation(
            summary = "修改房源",
            description = "仅更新传入的字段；设施和标签采用完整替换语义，新增图片会入库且保留已有图片记录。"
    )
    public ApiResponse<AdminHouseDtos.AdminHouseView> updateHouse(
            HttpServletRequest servletRequest,
            @PathVariable String houseId,
            @Valid @RequestBody AdminHouseDtos.UpdateHouseRequest request
    ) {
        return ApiResponse.success(
                "房源修改成功",
                houseService.updateHouse(houseId, request, CurrentUser.id(servletRequest))
        );
    }
}
