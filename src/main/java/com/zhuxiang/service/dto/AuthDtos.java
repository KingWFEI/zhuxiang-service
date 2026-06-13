package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record SmsCodeRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            @NotBlank(message = "验证码场景不能为空")
            @Pattern(
                    regexp = "login|register|reset_password|real_name",
                    message = "验证码场景不支持"
            ) String scene
    ) {
    }

    public record CodeLoginRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            @NotBlank(message = "验证码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误") String code
    ) {
    }

    public record PasswordLoginRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            @NotBlank(message = "密码不能为空") String password
    ) {
    }

    public record RegisterRequest(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            @NotBlank(message = "验证码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "验证码格式错误") String code,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 32, message = "密码长度应为 6-32 位") String password,
            @NotBlank(message = "昵称不能为空")
            @Size(min = 1, max = 30, message = "昵称长度应为 1-30 位") String nickname
    ) {
    }

    public record RefreshRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {
    }

    public record LogoutRequest(@NotBlank(message = "refreshToken 不能为空") String refreshToken) {
    }

    public record SmsCodeResult(long expiresIn) {
    }

    public record UserView(
            String id,
            String phone,
            String nickname,
            String avatarUrl,
            boolean isVerified
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
