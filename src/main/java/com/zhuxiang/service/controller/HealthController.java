package com.zhuxiang.service.controller;

import com.zhuxiang.service.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Void> health() {
        return ApiResponse.success(null);
    }
}
