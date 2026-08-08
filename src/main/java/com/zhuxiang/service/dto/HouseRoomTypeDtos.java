package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class HouseRoomTypeDtos {
    private HouseRoomTypeDtos() {
    }

    public record Item(
            String id,
            String name,
            Integer sortOrder,
            boolean enabled
    ) {
    }

    public record CreateRequest(
            @NotBlank(message = "户型名称不能为空")
            @Size(max = 50, message = "户型名称不能超过50个字符")
            @Schema(example = "2室1厅1卫") String name,
            @Min(value = 0, message = "排序值不能小于0") Integer sortOrder,
            Boolean enabled
    ) {
    }

    public record UpdateRequest(
            @NotBlank(message = "户型名称不能为空")
            @Size(max = 50, message = "户型名称不能超过50个字符") String name,
            @NotNull(message = "排序值不能为空")
            @Min(value = 0, message = "排序值不能小于0") Integer sortOrder,
            @NotNull(message = "enabled不能为空") Boolean enabled
    ) {
    }
}
