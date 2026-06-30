package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminUserDtos;
import com.zhuxiang.service.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequireAuth
@RestController
@RequestMapping("/admin/users")
@Tag(name = "管理端用户", description = "管理端查询用户并维护用户状态")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "分页查询用户", description = "按角色、状态以及用户 ID、手机号或昵称筛选用户。")
    public ApiResponse<PageData<AdminUserDtos.UserView>> getUsers(
            @Parameter(description = "用户角色：TENANT、HOUSEKEEPER、LANDLORD 或 ADMIN")
            @RequestParam(required = false) String role,
            @Parameter(description = "用户状态：active、disabled 或 cancelled")
            @RequestParam(required = false) String status,
            @Parameter(description = "搜索关键词，匹配用户 ID、手机号或昵称")
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @Parameter(description = "页码，从 1 开始", example = "1")
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围 1-100", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminUserService.getUsers(
                CurrentUser.id(request), role, status, keyword, page, pageSize
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "查询指定用户的账号、角色、认证和状态信息。")
    public ApiResponse<AdminUserDtos.UserView> getUser(
            @Parameter(description = "用户 ID") @PathVariable String id,
            HttpServletRequest request
    ) {
        return ApiResponse.success(adminUserService.getUser(CurrentUser.id(request), id));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "修改用户状态", description = "启用或禁用指定用户；不能禁用当前登录账号或恢复已注销账号。")
    public ApiResponse<AdminUserDtos.UserView> updateStatus(
            @Parameter(description = "用户 ID") @PathVariable String id,
            @Valid @RequestBody AdminUserDtos.UpdateStatusRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                "用户状态更新成功",
                adminUserService.updateStatus(CurrentUser.id(request), id, body)
        );
    }
}
