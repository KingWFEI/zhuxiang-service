package com.zhuxiang.service.controller;

import com.zhuxiang.service.auth.CurrentUser;
import com.zhuxiang.service.auth.RequireAuth;
import com.zhuxiang.service.common.ApiResponse;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.dto.HouseDtos;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.UserFavoriteHouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前用户个人中心接口。
 */
@Validated
@RequireAuth
@RestController
@RequestMapping("/profile")
@Tag(name = "个人中心", description = "当前用户资料、安全设置、头像、住所、门锁和收藏")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserService userService;
    private final LeaseService leaseService;
    private final UserFavoriteHouseService favoriteHouseService;

    public ProfileController(
            UserService userService,
            LeaseService leaseService,
            UserFavoriteHouseService favoriteHouseService
    ) {
        this.userService = userService;
        this.leaseService = leaseService;
        this.favoriteHouseService = favoriteHouseService;
    }

    /**
     * 获取当前用户资料。
     */
    @GetMapping
    @Operation(summary = "获取个人资料", description = "返回当前登录用户的手机号、昵称、头像、角色、实名认证状态和密码设置状态。")
    public ApiResponse<AuthDtos.UserView> getProfile(HttpServletRequest request) {
        return ApiResponse.success(userService.getProfile(CurrentUser.id(request)));
    }

    /**
     * 更新当前用户资料。
     */
    @PutMapping
    @Operation(summary = "更新个人资料", description = "按需更新当前用户昵称和头像 URL；未传字段保持不变。")
    public ApiResponse<AuthDtos.UserView> updateProfile(
            HttpServletRequest servletRequest,
            @Valid @RequestBody ProfileDtos.UpdateProfileRequest request
    ) {
        return ApiResponse.success(
                userService.updateProfile(CurrentUser.id(servletRequest), request)
        );
    }

    /**
     * 修改当前用户密码。
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "校验旧密码后设置新密码，新密码长度为 6-32 位。")
    public ApiResponse<Void> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ProfileDtos.ChangePasswordRequest body
    ) {
        userService.changePassword(CurrentUser.id(request), body);
        return ApiResponse.success("密码修改成功", null);
    }

    /**
     * 为验证码注册且尚未设置密码的用户首次设置密码。
     */
    @PutMapping("/password/set")
    @Operation(summary = "首次设置密码", description = "当前账号尚无登录密码时直接设置新密码；已设置密码的账号需使用修改密码接口。")
    public ApiResponse<Void> setPassword(
            HttpServletRequest request,
            @Valid @RequestBody ProfileDtos.SetPasswordRequest body
    ) {
        userService.setPassword(CurrentUser.id(request), body);
        return ApiResponse.success("密码设置成功", null);
    }

    /**
     * 修改当前用户手机号。
     */
    @PutMapping("/phone")
    @Operation(summary = "修改手机号", description = "使用新手机号收到的短信验证码完成换绑。")
    public ApiResponse<AuthDtos.UserView> changePhone(
            HttpServletRequest request,
            @Valid @RequestBody ProfileDtos.ChangePhoneRequest body
    ) {
        userService.changePhone(CurrentUser.id(request), body);
        return ApiResponse.success("手机号修改成功", userService.getProfile(CurrentUser.id(request)));
    }

    /**
     * 上传并更新当前用户头像。
     */
    @PostMapping("/avatar")
    @Operation(summary = "上传头像", description = "上传不超过 5MB 的头像文件，并更新当前用户头像地址。")
    public ApiResponse<ProfileDtos.AvatarResult> uploadAvatar(
            HttpServletRequest request,
            @Parameter(description = "头像文件，最大 5MB", required = true) @RequestParam("file") MultipartFile file
    ) {
        return ApiResponse.success(
                "上传成功",
                userService.uploadAvatar(CurrentUser.id(request), file)
        );
    }

    /**
     * 获取当前用户正在履行的租约。
     */
    @GetMapping("/current-home")
    @Operation(summary = "获取当前住所", description = "返回当前生效租约对应的房源、房间和门锁摘要；无生效租约时数据可能为空。")
    public ApiResponse<ProfileDtos.CurrentHome> currentHome(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getCurrentHome(CurrentUser.id(request)));
    }

    /**
     * 获取当前用户租约对应的门锁展示信息。
     */
    @GetMapping("/lock")
    @Operation(summary = "获取当前门锁", description = "返回当前生效租约关联的门锁状态和开锁权限有效期。")
    public ApiResponse<ProfileDtos.LockInfo> lockInfo(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getLockInfo(CurrentUser.id(request)));
    }

    /**
     * 分页获取当前用户收藏的房源。
     */
    @GetMapping("/favorite-houses")
    @Operation(summary = "获取收藏房源", description = "分页返回当前用户收藏的房源。")
    public ApiResponse<PageData<HouseDtos.HouseView>> favoriteHouses(
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") @Min(1) long page,
            @Parameter(description = "每页条数，范围 1-100", example = "20") @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                favoriteHouseService.getFavoriteHouses(CurrentUser.id(request), page, pageSize)
        );
    }
}
