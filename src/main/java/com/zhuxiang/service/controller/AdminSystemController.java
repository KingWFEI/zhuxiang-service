package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminSystemDtos;
import com.zhuxiang.service.service.AdminSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequireAuth
@RestController
@RequestMapping("/admin/system")
@Tag(name = "管理端系统", description = "查看代码内置角色模型和数据库账号统计")
@SecurityRequirement(name = "bearerAuth")
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    public AdminSystemController(AdminSystemService adminSystemService) {
        this.adminSystemService = adminSystemService;
    }

    @GetMapping("/overview")
    @Operation(summary = "查询系统概览", description = "返回后端代码内置角色定义及 user 表实时账号统计，仅管理员可访问。")
    public ApiResponse<AdminSystemDtos.SystemOverview> getOverview(HttpServletRequest request) {
        return ApiResponse.success(adminSystemService.getOverview(CurrentUser.id(request)));
    }
}
