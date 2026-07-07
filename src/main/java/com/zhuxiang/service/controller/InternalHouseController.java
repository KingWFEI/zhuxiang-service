package com.zhuxiang.service.controller;

import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.service.HouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 面向内部微服务的房源校验接口，不通过 ApiResponse 统一包裹。
 */
@RestController
@Tag(name = "内部接口", description = "供 immersive-tour-service 等内部服务调用的房源校验接口")
public class InternalHouseController {

    private final HouseService houseService;

    public InternalHouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    /**
     * 校验房源是否存在且对用户可见。
     * exists=true 表示房源存在且未逻辑删除，
     * visible=true 与 GET /api/houses/{houseId} 用户端可查看规则一致。
     */
    @GetMapping("/internal/houses/{houseId}/reference")
    @Operation(summary = "校验房源引用", description = "返回房源是否存在及是否对用户可见，供内部微服务调用。")
    public Map<String, Boolean> reference(
            @Parameter(description = "房源 ID", example = "house_001") @PathVariable String houseId
    ) {
        House house = houseService.getById(houseId);
        if (house == null) {
            return Map.of("exists", false, "visible", false);
        }
        boolean visible = "available".equals(house.getStatus())
                || "reserved".equals(house.getStatus());
        return Map.of("exists", true, "visible", visible);
    }
}
