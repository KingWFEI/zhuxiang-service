package com.zhuxiang.service;

import com.zhuxiang.service.auth.TokenProvider;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.LandlordService;
import com.zhuxiang.service.service.ObjectStorageService;
import com.zhuxiang.service.service.RefreshTokenService;
import com.zhuxiang.service.service.RealNameVerificationQueryService;
import com.zhuxiang.service.service.SmsCodeService;
import com.zhuxiang.service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServicePasswordTests {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final RealNameVerificationQueryService realNameVerificationQueryService =
            mock(RealNameVerificationQueryService.class);
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                mock(SmsCodeService.class),
                mock(RefreshTokenService.class),
                mock(MessageService.class),
                mock(TokenProvider.class),
                objectStorageService,
                mock(LandlordService.class),
                realNameVerificationQueryService
        );
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    void userWithoutPasswordCanSetPasswordOnce() {
        User user = activeUser(null);
        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);

        assertThat(userService.getProfile(user.getId()).hasPassword()).isFalse();

        userService.setPassword(user.getId(), new ProfileDtos.SetPasswordRequest("Example123"));

        assertThat(user.getPasswordHash()).isNotBlank();
        assertThat(new BCryptPasswordEncoder().matches("Example123", user.getPasswordHash())).isTrue();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(userService.getProfile(user.getId()).hasPassword()).isTrue();
        verify(userMapper).updateById(user);
    }

    @Test
    void profileReadsVerificationFromRealNameAuthRecords() {
        User user = activeUser(null);
        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(realNameVerificationQueryService.isVerified(user.getId())).thenReturn(true);

        assertThat(userService.getProfile(user.getId()).isVerified()).isTrue();
        verify(realNameVerificationQueryService).isVerified(user.getId());
    }

    @Test
    void userWithPasswordMustUseChangePasswordEndpoint() {
        User user = activeUser(new BCryptPasswordEncoder().encode("OldPassword"));
        when(userMapper.selectById(user.getId())).thenReturn(user);

        assertThatThrownBy(() -> userService.setPassword(
                user.getId(),
                new ProfileDtos.SetPasswordRequest("NewPassword")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(409);
            assertThat(exception.getMessage()).isEqualTo("当前账号已设置密码，请使用修改密码功能");
        });
    }

    @Test
    void uploadsAvatarThroughConfiguredObjectStorage() {
        User user = activeUser(null);
        MockMultipartFile avatar = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3}
        );
        String cosUrl = "https://example.cos.ap-guangzhou.myqcloud.com/zhuxiang/avatars/avatar.png";
        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        when(objectStorageService.store(
                anyString(), any(InputStream.class), anyLong(), eq("image/png")
        )).thenReturn(cosUrl);

        ProfileDtos.AvatarResult result = userService.uploadAvatar(user.getId(), avatar);

        assertThat(result.avatarUrl()).isEqualTo(cosUrl);
        assertThat(user.getAvatarUrl()).isEqualTo(cosUrl);
        verify(objectStorageService).store(
                startsWith("avatars/"), any(InputStream.class), eq(3L), eq("image/png")
        );
        verify(userMapper).updateById(user);
    }

    private User activeUser(String passwordHash) {
        User user = new User();
        user.setId("user-1");
        user.setPhone("13800138000");
        user.setNickname("测试用户");
        user.setAvatarUrl("");
        user.setRole("TENANT");
        user.setStatus("active");
        user.setPasswordHash(passwordHash);
        return user;
    }
}
