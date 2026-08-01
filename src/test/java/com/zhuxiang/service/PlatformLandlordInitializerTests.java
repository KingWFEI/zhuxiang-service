package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.config.PlatformLandlordInitializer;
import com.zhuxiang.service.config.PlatformLandlordProperties;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Landlord;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.LandlordMapper;
import com.zhuxiang.service.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformLandlordInitializerTests {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final LandlordMapper landlordMapper = mock(LandlordMapper.class);
    private final HouseMapper houseMapper = mock(HouseMapper.class);
    private final PlatformLandlordProperties properties = new PlatformLandlordProperties();
    private PlatformLandlordInitializer initializer;

    @BeforeEach
    void setUp() {
        properties.setId("platform-landlord-1");
        properties.setPhone("00000000000");
        properties.setName("勿忧管家");
        initializer = new PlatformLandlordInitializer(
                properties,
                userMapper,
                landlordMapper,
                houseMapper
        );
    }

    @Test
    void createsDisabledPlatformUserAndLandlordProfileThenRebindsPlatformHouses() {
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(houseMapper.update(any(House.class), any(Wrapper.class))).thenReturn(3);

        initializer.run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getId()).isEqualTo("platform-landlord-1");
        assertThat(userCaptor.getValue().getRole()).isEqualTo("LANDLORD");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("disabled");
        assertThat(userCaptor.getValue().getPasswordHash()).isNull();

        ArgumentCaptor<Landlord> landlordCaptor = ArgumentCaptor.forClass(Landlord.class);
        verify(landlordMapper).insert(landlordCaptor.capture());
        assertThat(landlordCaptor.getValue().getId()).isEqualTo("platform-landlord-1");
        assertThat(landlordCaptor.getValue().getUserId()).isEqualTo("platform-landlord-1");
        assertThat(landlordCaptor.getValue().getName()).isEqualTo("勿忧管家");

        ArgumentCaptor<House> houseCaptor = ArgumentCaptor.forClass(House.class);
        verify(houseMapper).update(houseCaptor.capture(), any(Wrapper.class));
        assertThat(houseCaptor.getValue().getLandlordId()).isEqualTo("platform-landlord-1");
    }

    @Test
    void existingSystemRecordsAreKeptAndInitializationRemainsIdempotent() {
        User user = new User();
        user.setId("platform-landlord-1");
        user.setPhone("00000000000");
        user.setRole("LANDLORD");
        user.setStatus("disabled");
        Landlord landlord = new Landlord();
        landlord.setId("platform-landlord-1");
        landlord.setUserId("platform-landlord-1");
        when(userMapper.selectById("platform-landlord-1")).thenReturn(user);
        when(landlordMapper.selectOne(any(Wrapper.class))).thenReturn(landlord);

        initializer.run(null);

        verify(userMapper, never()).insert(any(User.class));
        verify(landlordMapper, never()).insert(any(Landlord.class));
        verify(houseMapper).update(any(House.class), any(Wrapper.class));
    }

    @Test
    void refusesToReuseAnExistingRealUserId() {
        User user = new User();
        user.setId("platform-landlord-1");
        user.setPhone("13800000000");
        user.setRole("LANDLORD");
        user.setStatus("active");
        when(userMapper.selectById("platform-landlord-1")).thenReturn(user);

        assertThatThrownBy(() -> initializer.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("平台房东ID已被其他用户占用");

        verify(landlordMapper, never()).insert(any(Landlord.class));
        verify(houseMapper, never()).update(any(House.class), any(Wrapper.class));
    }
}
