package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(description = "短信验证码发送请求")
    public record SmsCodeRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            @Schema(description = "接收验证码的手机号", example = "13800138000") String phone,
            @NotBlank(message = "验证码场景不能为空")
            @Pattern(
                    regexp = "login|register|reset_password|real_name",
                    message = "验证码场景不支持"
            )
            @Schema(description = "验证码用途", allowableValues = {"login", "register", "reset_password", "real_name"}, example = "login") String scene
    ) {
    }

    @Schema(description = "短信验证码登录请求")
    public record CodeLoginRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            @Schema(description = "登录手机号", example = "13800138000") String phone,
            @NotBlank(message = "验证码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误")
            @Schema(description = "6 位短信验证码", example = "123456") String code
    ) {
    }

    @Schema(description = "账号密码登录请求")
    public record PasswordLoginRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            @Schema(description = "登录手机号", example = "13800138000") String phone,
            @NotBlank(message = "密码不能为空")
            @Schema(description = "登录密码", example = "Example123") String password
    ) {
    }

    @Schema(description = "移动端用户注册请求")
    public record RegisterRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误")
            @Schema(description = "注册手机号", example = "13800138000") String phone,
            @NotBlank(message = "验证码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误")
            @Schema(description = "注册场景的 6 位短信验证码", example = "123456") String code,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度应为 6-32 位")
            @Schema(description = "登录密码，长度 6-32 位", example = "Example123") String password,
            @NotBlank(message = "昵称不能为空")
            @Size(min = 1, max = 30, message = "昵称长度应为 1-30 位")
            @Schema(description = "用户昵称，长度 1-30 位", example = "小筑") String nickname
    ) {
    }

    @Schema(description = "令牌刷新请求")
    public record RefreshRequest(
            @NotBlank(message = "refreshToken 不能为空")
            @Schema(description = "登录或上次刷新时签发的刷新令牌") String refreshToken
    ) {
    }

    @Schema(description = "退出登录请求")
    public record LogoutRequest(
            @NotBlank(message = "refreshToken 不能为空")
            @Schema(description = "需要注销的刷新令牌") String refreshToken
    ) {
    }

    public record SmsCodeResult(long expiresIn) {
    }

    public record UserView(
            String id,
            String phone,
            String nickname,
            String avatarUrl,
            String role,
            boolean isVerified,
            boolean hasPassword
    ) {
    }

    public record AuthResult(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserView user
    ) {
    }

    public record TokenResult(String accessToken, String refreshToken, long expiresIn) {
    }
}
