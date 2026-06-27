package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminHouseAttributeDtos;
import com.zhuxiang.service.service.AdminHouseAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequireAuth
@Tag(name = "管理端房源设施与标签", description = "查询设施和标签字典，并配置房源关联关系")
@SecurityRequirement(name = "bearerAuth")
public class AdminHouseAttributeController {

    private final AdminHouseAttributeService attributeService;

    public AdminHouseAttributeController(AdminHouseAttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @GetMapping("/house-facilities")
    @Operation(summary = "查询设施字典", description = "返回全部设施字典项，包括已停用项。")
    public ApiResponse<List<AdminHouseAttributeDtos.FacilityItem>> getFacilityDictionary(
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(attributeService.getFacilityDictionary(CurrentUser.id(servletRequest)));
    }

    @PostMapping("/house-facilities")
    @Operation(summary = "新增设施字典项", description = "创建可供房源选择的设施字典项。")
    public ApiResponse<AdminHouseAttributeDtos.FacilityItem> createFacility(
            HttpServletRequest servletRequest,
            @Valid @RequestBody AdminHouseAttributeDtos.CreateFacilityRequest request
    ) {
        return ApiResponse.success(
                "设施创建成功",
                attributeService.createFacility(request, CurrentUser.id(servletRequest))
        );
    }

    @PutMapping("/house-facilities/{id}")
    @Operation(summary = "编辑设施字典项", description = "更新设施名称、图标、排序和启用状态。")
    public ApiResponse<AdminHouseAttributeDtos.FacilityItem> updateFacility(
            HttpServletRequest servletRequest,
            @Parameter(description = "设施ID", example = "wifi") @PathVariable String id,
            @Valid @RequestBody AdminHouseAttributeDtos.UpdateFacilityRequest request
    ) {
        return ApiResponse.success(
                "设施更新成功",
                attributeService.updateFacility(id, request, CurrentUser.id(servletRequest))
        );
    }

    @DeleteMapping("/house-facilities/{id}")
    @Operation(summary = "删除设施字典项", description = "仅允许删除未被任何房源引用的设施字典项。")
    public ApiResponse<Boolean> deleteFacility(
            HttpServletRequest servletRequest,
            @Parameter(description = "设施ID", example = "wifi") @PathVariable String id
    ) {
        attributeService.deleteFacility(id, CurrentUser.id(servletRequest));
        return ApiResponse.success("设施删除成功", true);
    }

    @GetMapping("/house-tags")
    @Operation(summary = "查询房源标签字典", description = "返回全部房源标签字典项，包括已停用项。")
    public ApiResponse<List<AdminHouseAttributeDtos.TagItem>> getTagDictionary(
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(attributeService.getTagDictionary(CurrentUser.id(servletRequest)));
    }

    @PostMapping("/house-tags")
    @Operation(summary = "新增标签字典项", description = "创建可供房源选择的标签字典项。")
    public ApiResponse<AdminHouseAttributeDtos.TagItem> createTag(
            HttpServletRequest servletRequest,
            @Valid @RequestBody AdminHouseAttributeDtos.CreateTagRequest request
    ) {
        return ApiResponse.success(
                "标签创建成功",
                attributeService.createTag(request, CurrentUser.id(servletRequest))
        );
    }

    @PutMapping("/house-tags/{id}")
    @Operation(summary = "编辑标签字典项", description = "更新标签名称、类型、排序和启用状态。")
    public ApiResponse<AdminHouseAttributeDtos.TagItem> updateTag(
            HttpServletRequest servletRequest,
            @Parameter(description = "标签ID", example = "tag-metro") @PathVariable String id,
            @Valid @RequestBody AdminHouseAttributeDtos.UpdateTagRequest request
    ) {
        return ApiResponse.success(
                "标签更新成功",
                attributeService.updateTag(id, request, CurrentUser.id(servletRequest))
        );
    }

    @DeleteMapping("/house-tags/{id}")
    @Operation(summary = "删除标签字典项", description = "仅允许删除未被任何房源引用的标签字典项。")
    public ApiResponse<Boolean> deleteTag(
            HttpServletRequest servletRequest,
            @Parameter(description = "标签ID", example = "tag-metro") @PathVariable String id
    ) {
        attributeService.deleteTag(id, CurrentUser.id(servletRequest));
        return ApiResponse.success("标签删除成功", true);
    }

    @GetMapping("/houses/{houseId}/attributes")
    @Operation(summary = "查询房源设施和标签配置", description = "按房源ID返回当前关联的设施和标签。")
    public ApiResponse<AdminHouseAttributeDtos.HouseAttributes> getHouseAttributes(
            HttpServletRequest servletRequest,
            @Parameter(description = "房源ID", example = "house_006") @PathVariable String houseId
    ) {
        return ApiResponse.success(
                attributeService.getHouseAttributes(houseId, CurrentUser.id(servletRequest))
        );
    }

    @PutMapping("/houses/{houseId}/facilities")
    @Operation(summary = "替换房源设施", description = "使用提交的 facilityIds 完整替换房源设施；空数组表示清空。")
    public ApiResponse<AdminHouseAttributeDtos.HouseAttributes> replaceFacilities(
            HttpServletRequest servletRequest,
            @Parameter(description = "房源ID", example = "house_006") @PathVariable String houseId,
            @Valid @RequestBody AdminHouseAttributeDtos.UpdateFacilitiesRequest request
    ) {
        return ApiResponse.success(
                "房源设施配置成功",
                attributeService.replaceFacilities(
                        houseId,
                        request.facilityIds(),
                        CurrentUser.id(servletRequest)
                )
        );
    }

    @PutMapping("/houses/{houseId}/tags")
    @Operation(summary = "替换房源标签", description = "使用提交的 tagIds 完整替换房源标签；空数组表示清空。")
    public ApiResponse<AdminHouseAttributeDtos.HouseAttributes> replaceTags(
            HttpServletRequest servletRequest,
            @Parameter(description = "房源ID", example = "house_006") @PathVariable String houseId,
            @Valid @RequestBody AdminHouseAttributeDtos.UpdateTagsRequest request
    ) {
        return ApiResponse.success(
                "房源标签配置成功",
                attributeService.replaceTags(houseId, request.tagIds(), CurrentUser.id(servletRequest))
        );
    }
}
