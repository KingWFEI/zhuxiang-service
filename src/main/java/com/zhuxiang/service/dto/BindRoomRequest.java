package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 门锁绑定房源请求。
 */
public record BindRoomRequest(
        @NotBlank(message = "houseId 不能为空") String houseId,
        String roomId
) {
}
