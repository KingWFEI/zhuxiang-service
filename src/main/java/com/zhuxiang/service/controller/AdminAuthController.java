package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminAuthDtos;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证接口。
 */
@RestController
@RequestMapping("/admin/auth")
@Tag(name = "管理端认证", description = "管理端用户登录、注册、令牌刷新和退出登录")
public class AdminAuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    public AdminAuthController(
            UserService userService,
            RefreshTokenService refreshTokenService
    ) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 管理端账号密码登录（仅限 ADMIN/HOUSEKEEPER/LANDLORD）。
     */
    @PostMapping("/login")
    @Operation(summary = "管理端登录", description = "使用手机号和密码登录，仅允许管理员、管家或房东角色。")
    public ApiResponse<AuthDtos.AuthResult> login(
            @Valid @RequestBody AuthDtos.PasswordLoginRequest request
    ) {
        return ApiResponse.success("登录成功", userService.adminLogin(request));
    }

    /**
     * 管理端注册新用户（可指定角色）。
     */
    @PostMapping("/register")
    @Operation(summary = "注册管理端用户", description = "创建管理员、管家或房东账号，并返回访问令牌和刷新令牌。")
    public ApiResponse<AuthDtos.AuthResult> register(
            @Valid @RequestBody AdminAuthDtos.AdminRegisterRequest request
    ) {
        return ApiResponse.success("注册成功", userService.adminRegister(request));
    }

    /**
     * 刷新访问令牌。
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新管理端访问令牌", description = "使用有效的刷新令牌换取新的访问令牌和刷新令牌。")
    public ApiResponse<AuthDtos.TokenResult> refresh(
            @Valid @RequestBody AuthDtos.RefreshRequest request
    ) {
        return ApiResponse.success("刷新成功", refreshTokenService.refresh(request));
    }

    /**
     * 退出登录。
     */
    @RequireAuth
    @PostMapping("/logout")
    @Operation(summary = "管理端退出登录", description = "注销当前用户提交的刷新令牌。")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Boolean> logout(
            HttpServletRequest servletRequest,
            @Valid @RequestBody AuthDtos.LogoutRequest request
    ) {
        return ApiResponse.success(
                "退出成功",
                refreshTokenService.logout(CurrentUser.id(servletRequest), request)
        );
    }
}
