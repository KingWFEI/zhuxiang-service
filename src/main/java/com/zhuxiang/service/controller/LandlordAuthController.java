package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.LandlordAuthDtos;
import com.zhuxiang.service.service.LandlordAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@RequestMapping("/landlord-auth")
@Tag(name = "房东认证")
@SecurityRequirement(name = "bearerAuth")
public class LandlordAuthController {
    private final LandlordAuthService service;

    public LandlordAuthController(LandlordAuthService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ApiResponse<LandlordAuthDtos.StatusView> status(HttpServletRequest request) {
        return ApiResponse.success(service.getMyStatus(CurrentUser.id(request)));
    }

    @PostMapping("/applications")
    public ApiResponse<LandlordAuthDtos.ApplicationView> submit(
            HttpServletRequest request,
            @Valid @RequestBody LandlordAuthDtos.SubmitRequest body
    ) {
        return ApiResponse.success(
                "房东认证申请已提交",
                service.submit(CurrentUser.id(request), body)
        );
    }
}
