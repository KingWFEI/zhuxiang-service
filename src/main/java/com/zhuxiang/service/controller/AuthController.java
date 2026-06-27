package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.service.SmsCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "用户认证", description = "移动端短信验证码、登录、注册、令牌刷新和退出登录")
public class AuthController {

    private final UserService userService;
    private final SmsCodeService smsCodeService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            UserService userService,
            SmsCodeService smsCodeService,
            RefreshTokenService refreshTokenService
    ) {
        this.userService = userService;
        this.smsCodeService = smsCodeService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 发送短信验证码。
     */
    @PostMapping("/sms-code")
    @Operation(summary = "发送短信验证码", description = "按指定业务场景发送 6 位短信验证码；开发环境可能返回固定验证码。")
    public ApiResponse<AuthDtos.SmsCodeResult> sendSmsCode(
            @Valid @RequestBody AuthDtos.SmsCodeRequest request
    ) {
        return ApiResponse.success("验证码发送成功", smsCodeService.sendSmsCode(request));
    }

    /**
     * 使用短信验证码登录。
     */
    @PostMapping("/login/code")
    @Operation(summary = "验证码登录", description = "使用手机号和有效短信验证码登录移动端。")
    public ApiResponse<AuthDtos.AuthResult> loginByCode(
            @Valid @RequestBody AuthDtos.CodeLoginRequest request
    ) {
        return ApiResponse.success("登录成功", userService.loginByCode(request));
    }

    /**
     * 使用密码登录。
     */
    @PostMapping("/login/password")
    @Operation(summary = "密码登录", description = "使用手机号和密码登录移动端。")
    public ApiResponse<AuthDtos.AuthResult> loginByPassword(
            @Valid @RequestBody AuthDtos.PasswordLoginRequest request
    ) {
        return ApiResponse.success("登录成功", userService.loginByPassword(request));
    }

    /**
     * 注册移动端用户。
     */
    @PostMapping("/register")
    @Operation(summary = "注册移动端用户", description = "校验注册验证码后创建租客账号，并返回访问令牌和刷新令牌。")
    public ApiResponse<AuthDtos.AuthResult> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request
    ) {
        return ApiResponse.success("注册成功", userService.register(request));
    }

    /**
     * 刷新用户访问令牌。
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新访问令牌", description = "使用有效的刷新令牌换取新的访问令牌和刷新令牌。")
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
    @Operation(summary = "退出登录", description = "注销当前用户提交的刷新令牌。")
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
