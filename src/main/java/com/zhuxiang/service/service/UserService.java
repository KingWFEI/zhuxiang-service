package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.dto.AdminAuthDtos;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.dto.ProfileDtos;
import org.springframework.web.multipart.MultipartFile;

/**
* @author king-wang
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-06-12 19:55:54
*/
public interface UserService extends IService<User> {

    /**
     * 使用短信验证码登录。
     */
    AuthDtos.AuthResult loginByCode(AuthDtos.CodeLoginRequest request);

    /**
     * 使用账号密码登录。
     */
    AuthDtos.AuthResult loginByPassword(AuthDtos.PasswordLoginRequest request);

    /**
     * 注册移动端用户。
     */
    AuthDtos.AuthResult register(AuthDtos.RegisterRequest request);

    /**
     * 获取指定用户资料。
     */
    AuthDtos.UserView getProfile(String userId);

    /**
     * 更新指定用户资料。
     */
    AuthDtos.UserView updateProfile(
            String userId,
            ProfileDtos.UpdateProfileRequest request
    );

    /**
     * 上传并更新用户头像。
     */
    ProfileDtos.AvatarResult uploadAvatar(String userId, MultipartFile file);

    /**
     * 获取状态正常的用户实体。
     */
    User requireActiveUser(String userId);

    /**
     * 管理端账号密码登录（仅限 ADMIN/HOUSEKEEPER/LANDLORD 角色）。
     */
    AuthDtos.AuthResult adminLogin(AuthDtos.PasswordLoginRequest request);

    /**
     * 管理端注册新用户（可指定 ADMIN/HOUSEKEEPER/LANDLORD 角色）。
     */
    AuthDtos.AuthResult adminRegister(AdminAuthDtos.AdminRegisterRequest request);
}
