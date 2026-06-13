package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.service.LandlordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 房东资料查询接口。
 */
@RestController
@RequestMapping("/landlords")
public class LandlordController {

    private final LandlordService landlordService;

    public LandlordController(LandlordService landlordService) {
        this.landlordService = landlordService;
    }

    /**
     * 获取指定房东资料。
     */
    @GetMapping("/{landlordId}")
    public ApiResponse<HouseDtos.LandlordView> detail(@PathVariable String landlordId) {
        return ApiResponse.success(landlordService.getLandlordDetail(landlordId));
    }
}
