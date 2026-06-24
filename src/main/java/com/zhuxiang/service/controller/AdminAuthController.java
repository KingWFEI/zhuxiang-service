package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AdminAuthDtos;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.service.UserService;
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
    public ApiResponse<AuthDtos.AuthResult> login(
            @Valid @RequestBody AuthDtos.PasswordLoginRequest request
    ) {
        return ApiResponse.success("登录成功", userService.adminLogin(request));
    }

    /**
     * 管理端注册新用户（可指定角色）。
     */
    @PostMapping("/register")
    public ApiResponse<AuthDtos.AuthResult> register(
            @Valid @RequestBody AdminAuthDtos.AdminRegisterRequest request
    ) {
        return ApiResponse.success("注册成功", userService.adminRegister(request));
    }

    /**
     * 刷新访问令牌。
     */
    @PostMapping("/refresh")
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
