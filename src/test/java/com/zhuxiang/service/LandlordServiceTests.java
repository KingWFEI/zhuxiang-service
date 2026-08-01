package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.LandlordDtos;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LandlordMapper;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.impl.LandlordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LandlordServiceTests {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final LandlordMapper landlordMapper = mock(LandlordMapper.class);
    private LandlordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LandlordServiceImpl(userMapper);
        ReflectionTestUtils.setField(service, "baseMapper", landlordMapper);
    }

    @Test
    void publicProfileUsesUserIdAndHidesPrivateContacts() {
        User user = landlordUser("user-1");
        Landlord profile = profile("profile-9", user.getId());
        profile.setPhone("13800000000");
        profile.setWechat("private-wechat");
        profile.setEmail("public@example.com");
        profile.setShowPhone(0);
        profile.setShowWechat(0);
        profile.setShowEmail(1);
        when(userMapper.selectById("user-1")).thenReturn(user);
        when(landlordMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(profile);

        LandlordDtos.ProfileView view = service.getPublicProfile("user-1");

        assertThat(view.userId()).isEqualTo("user-1");
        assertThat(view.phone()).isNull();
        assertThat(view.wechat()).isNull();
        assertThat(view.email()).isEqualTo("public@example.com");
        assertThat(view.profileTags()).containsExactly("响应及时", "熟悉本地");
    }

    @Test
    void landlordCanUpdateOwnPresentationWithoutChangingServerMetrics() {
        User user = landlordUser("user-1");
        Landlord profile = profile("profile-9", user.getId());
        when(userMapper.selectById("user-1")).thenReturn(user);
        when(landlordMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(profile);
        when(landlordMapper.updateById(profile)).thenReturn(1);

        LandlordDtos.ProfileView view = service.updateMyProfile(
                "user-1",
                new LandlordDtos.UpdateLandlordProfileRequest(
                        "林先生",
                        null,
                        null,
                        null,
                        null,
                        "余杭区",
                        6,
                        List.of("可养宠", "近地铁"),
                        "13800000000",
                        null,
                        null,
                        "09:00-21:00",
                        "通常十分钟内回复",
                        true,
                        false,
                        false
                )
        );

        verify(landlordMapper).updateById(profile);
        assertThat(view.name()).isEqualTo("林先生");
        assertThat(view.showPhone()).isTrue();
        assertThat(view.rating()).isEqualByComparingTo("4.80");
        assertThat(view.rentedCount()).isEqualTo(12);
    }

    @Test
    void contactCannotBePublishedBeforeItIsFilled() {
        User user = landlordUser("user-1");
        Landlord profile = profile("profile-9", user.getId());
        profile.setPhone(null);
        when(userMapper.selectById("user-1")).thenReturn(user);
        when(landlordMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(profile);

        assertThatThrownBy(() -> service.updateMyProfile(
                "user-1",
                new LandlordDtos.UpdateLandlordProfileRequest(
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null,
                        true, null, null
                )
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("填写联系电话");
    }

    private User landlordUser(String id) {
        User user = new User();
        user.setId(id);
        user.setRole("LANDLORD");
        user.setNickname("测试房东");
        user.setPhone("13800000000");
        user.setIsVerified(1);
        return user;
    }

    private Landlord profile(String id, String userId) {
        Landlord profile = new Landlord();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setName("测试房东");
        profile.setAvatarUrl("");
        profile.setRating(new BigDecimal("4.80"));
        profile.setRentedCount(12);
        profile.setResponseDescription("回复及时");
        profile.setServiceYears(3);
        profile.setProfileTags("响应及时\n熟悉本地");
        profile.setShowPhone(0);
        profile.setShowWechat(0);
        profile.setShowEmail(0);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return profile;
    }
}
