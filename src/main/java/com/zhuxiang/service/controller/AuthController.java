package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.service.AppUserService;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.service.SmsCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AppUserService appUserService;
    private final SmsCodeService smsCodeService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AppUserService appUserService,
            SmsCodeService smsCodeService,
            RefreshTokenService refreshTokenService
    ) {
        this.appUserService = appUserService;
        this.smsCodeService = smsCodeService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 发送短信验证码。
     */
    @PostMapping("/sms-code")
    public ApiResponse<AuthDtos.SmsCodeResult> sendSmsCode(
            @Valid @RequestBody AuthDtos.SmsCodeRequest request
    ) {
        return ApiResponse.success("验证码发送成功", smsCodeService.sendSmsCode(request));
    }

    /**
     * 使用短信验证码登录。
     */
    @PostMapping("/login/code")
    public ApiResponse<AuthDtos.AuthResult> loginByCode(
            @Valid @RequestBody AuthDtos.CodeLoginRequest request
    ) {
        return ApiResponse.success("登录成功", appUserService.loginByCode(request));
    }

    /**
     * 使用密码登录。
     */
    @PostMapping("/login/password")
    public ApiResponse<AuthDtos.AuthResult> loginByPassword(
            @Valid @RequestBody AuthDtos.PasswordLoginRequest request
    ) {
        return ApiResponse.success("登录成功", appUserService.loginByPassword(request));
    }

    /**
     * 注册移动端用户。
     */
    @PostMapping("/register")
    public ApiResponse<AuthDtos.AuthResult> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request
    ) {
        return ApiResponse.success("注册成功", appUserService.register(request));
    }

    /**
     * 刷新用户访问令牌。
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthDtos.TokenResult> refresh(
            @Valid @RequestBody AuthDtos.RefreshRequest request
    ) {
        return ApiResponse.success("刷新成功", refreshTokenService.refresh(request));
    }

    /**
     * 退出当前用户登录。
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
