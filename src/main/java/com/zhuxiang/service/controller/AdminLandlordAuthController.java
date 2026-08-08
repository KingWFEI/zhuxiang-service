package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.LandlordAuthDtos;
import com.zhuxiang.service.service.LandlordAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@RequestMapping("/admin/landlord-auth")
@Tag(name = "管理端房东认证审核")
@SecurityRequirement(name = "bearerAuth")
public class AdminLandlordAuthController {
    private final LandlordAuthService service;

    public AdminLandlordAuthController(LandlordAuthService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageData<LandlordAuthDtos.AdminListItem>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize
    ) {
        return ApiResponse.success(service.listAdmin(
                CurrentUser.id(request), status, keyword, page, pageSize
        ));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<LandlordAuthDtos.ApplicationView> detail(
            HttpServletRequest request,
            @PathVariable String applicationId
    ) {
        return ApiResponse.success(service.adminDetail(
                CurrentUser.id(request), applicationId
        ));
    }

    @PostMapping("/{applicationId}/review")
    public ApiResponse<LandlordAuthDtos.ApplicationView> review(
            HttpServletRequest request,
            @PathVariable String applicationId,
            @Valid @RequestBody LandlordAuthDtos.ReviewRequest body
    ) {
        return ApiResponse.success("审核完成", service.review(
                CurrentUser.id(request), applicationId, body
        ));
    }
}
