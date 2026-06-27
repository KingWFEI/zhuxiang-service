package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminHouseAttributeDtos {

    private AdminHouseAttributeDtos() {
    }

    @Schema(description = "房源设施字典项")
    public record FacilityItem(
            @Schema(description = "设施ID") String id,
            @Schema(description = "设施名称") String name,
            @Schema(description = "前端图标键") String iconKey,
            @Schema(description = "排序值") Integer sortOrder,
            @Schema(description = "是否启用") boolean enabled
    ) {
    }

    @Schema(description = "房源标签字典项")
    public record TagItem(
            @Schema(description = "标签ID") String id,
            @Schema(description = "标签名称") String name,
            @Schema(description = "标签类型") String tagType,
            @Schema(description = "排序值") Integer sortOrder,
            @Schema(description = "是否启用") boolean enabled
    ) {
    }

    @Schema(description = "新增设施字典项请求")
    public record CreateFacilityRequest(
            @NotBlank(message = "设施名称不能为空")
            @Size(max = 50, message = "设施名称不能超过50个字符")
            @Schema(description = "设施名称", example = "Wi-Fi") String name,
            @Size(max = 50, message = "图标键不能超过50个字符")
            @Schema(description = "前端图标键", example = "wifi") String iconKey,
            @Min(value = 0, message = "排序值不能小于0")
            @Schema(description = "排序值，默认0", example = "10") Integer sortOrder,
            @Schema(description = "是否启用，默认true", example = "true") Boolean enabled
    ) {
    }

    @Schema(description = "编辑设施字典项请求")
    public record UpdateFacilityRequest(
            @NotBlank(message = "设施名称不能为空")
            @Size(max = 50, message = "设施名称不能超过50个字符")
            @Schema(description = "设施名称", example = "高速Wi-Fi") String name,
            @Size(max = 50, message = "图标键不能超过50个字符")
            @Schema(description = "前端图标键", example = "wifi") String iconKey,
            @NotNull(message = "排序值不能为空")
            @Min(value = 0, message = "排序值不能小于0")
            @Schema(description = "排序值", example = "10") Integer sortOrder,
            @NotNull(message = "enabled不能为空")
            @Schema(description = "是否启用", example = "true") Boolean enabled
    ) {
    }

    @Schema(description = "新增房源标签字典项请求")
    public record CreateTagRequest(
            @NotBlank(message = "标签名称不能为空")
            @Size(max = 50, message = "标签名称不能超过50个字符")
            @Schema(description = "标签名称", example = "近地铁") String name,
            @NotBlank(message = "标签类型不能为空")
            @Size(max = 30, message = "标签类型不能超过30个字符")
            @Schema(description = "标签类型", example = "traffic") String tagType,
            @Min(value = 0, message = "排序值不能小于0")
            @Schema(description = "排序值，默认0", example = "10") Integer sortOrder,
            @Schema(description = "是否启用，默认true", example = "true") Boolean enabled
    ) {
    }

    @Schema(description = "编辑房源标签字典项请求")
    public record UpdateTagRequest(
            @NotBlank(message = "标签名称不能为空")
            @Size(max = 50, message = "标签名称不能超过50个字符")
            @Schema(description = "标签名称", example = "地铁房") String name,
            @NotBlank(message = "标签类型不能为空")
            @Size(max = 30, message = "标签类型不能超过30个字符")
            @Schema(description = "标签类型", example = "traffic") String tagType,
            @NotNull(message = "排序值不能为空")
            @Min(value = 0, message = "排序值不能小于0")
            @Schema(description = "排序值", example = "10") Integer sortOrder,
            @NotNull(message = "enabled不能为空")
            @Schema(description = "是否启用", example = "true") Boolean enabled
    ) {
    }

    @Schema(description = "房源当前设施和标签配置")
    public record HouseAttributes(
            @Schema(description = "房源ID") String houseId,
            @Schema(description = "已配置设施") List<FacilityItem> facilities,
            @Schema(description = "已配置标签") List<TagItem> tags
    ) {
    }

    @Schema(description = "替换房源设施请求；传空数组表示清空")
    public record UpdateFacilitiesRequest(
            @NotNull(message = "facilityIds 不能为空")
            @Schema(description = "需要完整替换的设施ID列表")
            List<@NotBlank(message = "facilityId 不能为空") String> facilityIds
    ) {
    }

    @Schema(description = "替换房源标签请求；传空数组表示清空")
    public record UpdateTagsRequest(
            @NotNull(message = "tagIds 不能为空")
            @Schema(description = "需要完整替换的标签ID列表")
            List<@NotBlank(message = "tagId 不能为空") String> tagIds
    ) {
    }
}
