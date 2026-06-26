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
    public ApiResponse<AuthDtos.UserView> getProfile(HttpServletRequest request) {
        return ApiResponse.success(userService.getProfile(CurrentUser.id(request)));
    }

    /**
     * 更新当前用户资料。
     */
    @PutMapping
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
    public ApiResponse<Void> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ProfileDtos.ChangePasswordRequest body
    ) {
        userService.changePassword(CurrentUser.id(request), body);
        return ApiResponse.success("密码修改成功", null);
    }

    /**
     * 修改当前用户手机号。
     */
    @PutMapping("/phone")
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
    public ApiResponse<ProfileDtos.AvatarResult> uploadAvatar(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file
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
    public ApiResponse<ProfileDtos.CurrentHome> currentHome(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getCurrentHome(CurrentUser.id(request)));
    }

    /**
     * 获取当前用户租约对应的门锁展示信息。
     */
    @GetMapping("/lock")
    public ApiResponse<ProfileDtos.LockInfo> lockInfo(HttpServletRequest request) {
        return ApiResponse.success(leaseService.getLockInfo(CurrentUser.id(request)));
    }

    /**
     * 分页获取当前用户收藏的房源。
     */
    @GetMapping("/favorite-houses")
    public ApiResponse<PageData<HouseDtos.HouseView>> favoriteHouses(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long pageSize,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                favoriteHouseService.getFavoriteHouses(CurrentUser.id(request), page, pageSize)
        );
    }
}
