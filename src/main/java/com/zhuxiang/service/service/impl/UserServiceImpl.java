package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.auth.TokenProvider;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.AdminAuthDtos;
import com.zhuxiang.service.dto.AuthDtos;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.entity.RefreshToken;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.service.SmsCodeService;
import com.zhuxiang.service.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
* @author king-wang
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-06-12 19:55:54
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final SmsCodeService smsCodeService;
    private final RefreshTokenService refreshTokenService;
    private final MessageService messageService;
    private final TokenProvider tokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Path uploadDirectory;
    private final String contextPath;

    public UserServiceImpl(
            SmsCodeService smsCodeService,
            RefreshTokenService refreshTokenService,
            MessageService messageService,
            TokenProvider tokenProvider,
            @Value("${app.upload.directory}") String uploadDirectory,
            @Value("${server.servlet.context-path:/api}") String contextPath
    ) {
        this.smsCodeService = smsCodeService;
        this.refreshTokenService = refreshTokenService;
        this.messageService = messageService;
        this.tokenProvider = tokenProvider;
        this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.contextPath = contextPath;
    }

    /**
     * 使用短信验证码完成用户登录。
     */
    @Override
    @Transactional
    public AuthDtos.AuthResult loginByCode(AuthDtos.CodeLoginRequest request) {
        smsCodeService.consumeSmsCode(request.phone(), "login", request.code());
        User user = findByPhone(request.phone());
        if (user == null) {
            user = createUser(request.phone(), null, "住享用户");
        }
        requireActiveUser(user.getId());
        updateLoginTime(user);
        return createSession(user);
    }

    /**
     * 校验账号密码并完成用户登录。
     */
    @Override
    @Transactional
    public AuthDtos.AuthResult loginByPassword(AuthDtos.PasswordLoginRequest request) {
        User user = findByPhone(request.phone());
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("手机号或密码错误");
        }
        requireActiveUser(user.getId());
        updateLoginTime(user);
        return createSession(user);
    }

    /**
     * 创建用户并返回登录会话。
     */
    @Override
    @Transactional
    public AuthDtos.AuthResult register(AuthDtos.RegisterRequest request) {
        if (findByPhone(request.phone()) != null) {
            throw BusinessException.conflict("该手机号已注册");
        }
        smsCodeService.consumeSmsCode(request.phone(), "register", request.code());
        User user = createUser(request.phone(), request.password(), request.nickname());
        updateLoginTime(user);
        return createSession(user);
    }

    /**
     * 获取指定用户资料视图。
     */
    @Override
    public AuthDtos.UserView getProfile(String userId) {
        return toUserView(requireActiveUser(userId));
    }

    /**
     * 更新指定用户的基础资料。
     */
    @Override
    @Transactional
    public AuthDtos.UserView updateProfile(
            String userId,
            ProfileDtos.UpdateProfileRequest request
    ) {
        if (!StringUtils.hasText(request.nickname()) && !StringUtils.hasText(request.avatarUrl())) {
            throw BusinessException.badRequest("至少提供一个需要修改的字段");
        }
        User user = requireActiveUser(userId);
        if (StringUtils.hasText(request.nickname())) {
            user.setNickname(request.nickname().trim());
        }
        if (StringUtils.hasText(request.avatarUrl())) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
        return toUserView(user);
    }

    /**
     * 保存头像文件并更新用户头像地址。
     */
    @Override
    @Transactional
    public ProfileDtos.AvatarResult uploadAvatar(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("头像文件不能为空");
        }
        String extension = CONTENT_TYPE_EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw BusinessException.badRequest("头像仅支持 JPG、PNG 或 WebP");
        }
        try {
            Path avatarDirectory = uploadDirectory.resolve("avatars");
            Files.createDirectories(avatarDirectory);
            String filename = UUID.randomUUID() + extension;
            Path target = avatarDirectory.resolve(filename).normalize();
            if (!target.startsWith(avatarDirectory)) {
                throw BusinessException.badRequest("文件名无效");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String avatarUrl = contextPath + "/uploads/avatars/" + filename;
            User user = requireActiveUser(userId);
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(LocalDateTime.now());
            updateById(user);
            return new ProfileDtos.AvatarResult(avatarUrl);
        } catch (IOException exception) {
            throw new IllegalStateException("头像保存失败", exception);
        }
    }

    private static final String[] ADMIN_ROLES = {"ADMIN", "HOUSEKEEPER", "LANDLORD"};

    /**
     * 管理端账号密码登录，校验角色为非 TENANT。
     */
    @Override
    @Transactional
    public AuthDtos.AuthResult adminLogin(AuthDtos.PasswordLoginRequest request) {
        User user = findByPhone(request.phone());
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("手机号或密码错误");
        }
        requireActiveUser(user.getId());
        if ("TENANT".equals(user.getRole())) {
            throw BusinessException.forbidden("该账号无权登录管理端");
        }
        updateLoginTime(user);
        return createSession(user);
    }

    /**
     * 管理端注册新用户，允许指定 ADMIN/HOUSEKEEPER/LANDLORD 角色。
     */
    @Override
    @Transactional
    public AuthDtos.AuthResult adminRegister(AdminAuthDtos.AdminRegisterRequest request) {
        if (findByPhone(request.phone()) != null) {
            throw BusinessException.conflict("该手机号已注册");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setAvatarUrl("");
        user.setRole(request.role());
        user.setIsVerified(0);
        user.setStatus("active");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        save(user);
        messageService.createWelcomeMessage(user.getId());
        updateLoginTime(user);
        return createSession(user);
    }

    /**
     * 查询并校验状态正常的用户。
     */
    @Override
    public User requireActiveUser(String userId) {
        User user = getById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("用户不存在");
        }
        if (!"active".equals(user.getStatus())) {
            throw BusinessException.forbidden("用户状态不可用");
        }
        return user;
    }

    /**
     * 根据手机号查询用户。
     */
    private User findByPhone(String phone) {
        return getOne(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getPhone, phone)
                        .last("LIMIT 1"),
                false
        );
    }

    /**
     * 创建新的移动端用户。
     */
    private User createUser(String phone, String password, String nickname) {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setPhone(phone);
        user.setPasswordHash(password == null ? null : passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : "住享用户");
        user.setAvatarUrl("");
        user.setRole("TENANT");
        user.setIsVerified(0);
        user.setStatus("active");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        save(user);
        messageService.createWelcomeMessage(user.getId());
        return user;
    }

    /**
     * 更新用户最近登录时间。
     */
    private void updateLoginTime(User user) {
        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        updateById(user);
    }

    /**
     * 为用户创建访问令牌和刷新令牌。
     */
    private AuthDtos.AuthResult createSession(User user) {
        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user.getId(), LocalDateTime.now());
        return new AuthDtos.AuthResult(
                tokenProvider.createAccessToken(user.getId()),
                refreshToken.getRefreshToken(),
                tokenProvider.accessTokenSeconds(),
                toUserView(user)
        );
    }

    /**
     * 将用户实体转换为用户视图。
     */
    private AuthDtos.UserView toUserView(User user) {
        return new AuthDtos.UserView(
                user.getId(),
                user.getPhone(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                Integer.valueOf(1).equals(user.getIsVerified())
        );
    }
}
