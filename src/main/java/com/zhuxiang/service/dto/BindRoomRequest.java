package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 门锁绑定房源请求。
 */
@Schema(description = "门锁绑定房源请求")
public record BindRoomRequest(
        @NotBlank(message = "houseId 不能为空")
        @Schema(description = "房源 ID", example = "house_001") String houseId,
        @Schema(description = "房间 ID；整套房源绑定时可不传", example = "room_001") String roomId
) {
}
