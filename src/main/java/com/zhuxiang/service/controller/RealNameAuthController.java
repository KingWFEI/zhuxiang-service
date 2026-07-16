package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.dto.RealNameAuthDtos;
import com.zhuxiang.service.service.RealNameAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人实名认证接口。
 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/real-name-auth")
@Tag(name = "个人实名认证", description = "发起人脸认证、刷新认证结果、查询认证状态")
@SecurityRequirement(name = "bearerAuth")
public class RealNameAuthController {

    private final RealNameAuthService realNameAuthService;

    public RealNameAuthController(RealNameAuthService realNameAuthService) {
        this.realNameAuthService = realNameAuthService;
    }

    /**
     * 发起个人实名认证。
     */
    @PostMapping("/start")
    @Operation(summary = "发起个人实名认证", description = "提交姓名和身份证号，调用 e签宝人脸核身 H5，返回认证结果。已认证用户不可重复发起。")
    public ApiResponse<RealNameAuthDtos.StartResult> startAuth(
            @Valid @RequestBody RealNameAuthDtos.StartRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                realNameAuthService.startAuth(CurrentUser.id(servletRequest), request)
        );
    }

    /**
     * 重新发起个人实名认证（强制过期旧 VERIFYING 任务）。
     */
    @PostMapping("/restart")
    @Operation(summary = "重新发起个人实名认证", description = "强制将旧 VERIFYING 记录标记为 EXPIRED 后重新创建认证任务。适用于认证链接失效但状态仍为 VERIFYING 的场景。")
    public ApiResponse<RealNameAuthDtos.StartResult> restartAuth(
            @Valid @RequestBody RealNameAuthDtos.RestartRequest request,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                realNameAuthService.restartAuth(CurrentUser.id(servletRequest), request)
        );
    }

    /**
     * 主动刷新认证结果。
     */
    @PostMapping("/{realNameAuthNo}/refresh")
    @Operation(summary = "主动刷新认证结果", description = "根据业务流水号向 e签宝查询最新认证结果并更新本地状态。只能刷新本人的认证记录。")
    public ApiResponse<RealNameAuthDtos.RefreshResult> refreshAuth(
            @Parameter(description = "平台实名认证业务流水号", required = true)
            @PathVariable String realNameAuthNo,
            HttpServletRequest servletRequest
    ) {
        return ApiResponse.success(
                realNameAuthService.refreshAuth(CurrentUser.id(servletRequest), realNameAuthNo)
        );
    }

    /**
     * 查询当前用户实名认证状态。
     */
    @GetMapping("/status")
    @Operation(summary = "查询当前用户实名认证状态", description = "返回当前用户最近一条实名认证记录。从未发起时返回 UNVERIFIED。")
    public ApiResponse<RealNameAuthDtos.StatusResult> getStatus(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        servletResponse.setHeader("Cache-Control", "no-store");
        return ApiResponse.success(
                realNameAuthService.getStatus(CurrentUser.id(servletRequest))
        );
    }
}
