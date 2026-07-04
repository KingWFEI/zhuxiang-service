package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.SmartLockDetailResponse;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AdminLockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminLockListServiceTests {

    private final HouseService houseService = mock(HouseService.class);
    private final UserService userService = mock(UserService.class);
    private final TtLockTokenService tokenService = mock(TtLockTokenService.class);
    private final TtLockOpenApiClient openApiClient = mock(TtLockOpenApiClient.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private AdminLockServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminLockServiceImpl(
                houseService, userService, tokenService, openApiClient, new TtLockProperties()
        );
        ReflectionTestUtils.setField(service, "baseMapper", smartLockMapper);
    }

    @Test
    void returnsLockListWithBoundHouseInformation() {
        User admin = new User();
        admin.setRole("ADMIN");
        SmartLock smartLock = new SmartLock();
        smartLock.setId("smart-lock-1");
        smartLock.setLockName("1201门锁");
        smartLock.setLockMac("AA:BB:CC:DD:EE:FF");
        smartLock.setStatus("BOUND");
        smartLock.setLockId(12345L);
        smartLock.setHouseId("house-1");
        smartLock.setRoomId("1201");
        smartLock.setBattery(88);
        smartLock.setRssi(-52);
        smartLock.setLockData("sensitive-lock-data");
        House house = new House();
        house.setId("house-1");
        house.setTitle("高新区精装一居室");
        house.setBuilding("3栋");
        house.setUnit("2单元");
        house.setRoom("1201");

        when(userService.requireActiveUser("admin-1")).thenReturn(admin);
        when(smartLockMapper.selectList(any(Wrapper.class))).thenReturn(List.of(smartLock));
        when(houseService.listByIds(anyCollection())).thenReturn(List.of(house));

        List<SmartLockDetailResponse> result = service.getLockList("admin-1");

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.smartLockId()).isEqualTo("smart-lock-1");
            assertThat(item.houseId()).isEqualTo("house-1");
            assertThat(item.houseName()).isEqualTo("高新区精装一居室");
            assertThat(item.roomName()).isEqualTo("3栋-2单元-1201");
            assertThat(item.battery()).isEqualTo(88);
        });
    }
}
