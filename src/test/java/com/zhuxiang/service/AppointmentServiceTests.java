package com.zhuxiang.service;

import com.zhuxiang.service.common.ViewingMode;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.entity.Appointment;
import com.zhuxiang.service.entity.AppointmentStatusLog;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.AppointmentAccessGrantMapper;
import com.zhuxiang.service.mapper.AppointmentMapper;
import com.zhuxiang.service.mapper.AppointmentStatusLogMapper;
import com.zhuxiang.service.mapper.HouseViewingConfigMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.security.AppointmentCheckinCodeService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentServiceTests {

    private final HouseService houseService = mock(HouseService.class);
    private final UserService userService = mock(UserService.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private final HouseViewingConfigMapper configMapper =
            mock(HouseViewingConfigMapper.class);
    private final AppointmentAccessGrantMapper accessGrantMapper =
            mock(AppointmentAccessGrantMapper.class);
    private final AppointmentStatusLogMapper statusLogMapper =
            mock(AppointmentStatusLogMapper.class);
    private final MessageService messageService = mock(MessageService.class);
    private final AppointmentCheckinCodeService checkinCodeService =
            mock(AppointmentCheckinCodeService.class);
    private final AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AppointmentServiceImpl(
                houseService,
                userService,
                smartLockMapper,
                configMapper,
                accessGrantMapper,
                statusLogMapper,
                messageService,
                checkinCodeService
        );
        ReflectionTestUtils.setField(service, "baseMapper", appointmentMapper);
        User tenant = new User();
        tenant.setId("tenant-1");
        tenant.setStatus("active");
        when(userService.requireActiveUser("tenant-1")).thenReturn(tenant);
        when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);
        when(statusLogMapper.insert(any(AppointmentStatusLog.class))).thenReturn(1);
    }

    @Test
    void platformHouseWithUsableLockCreatesAutoConfirmedSelfServiceAppointment() {
        House house = house("PLATFORM");
        house.setIsSmartLockSupported(1);
        house.setIsSelfViewingSupported(1);
        house.setSmartLockId("lock-1");
        house.setLockBindStatus("BOUND");
        SmartLock lock = new SmartLock();
        lock.setId("lock-1");
        lock.setHouseId(house.getId());
        lock.setStatus("BOUND");
        lock.setLockId(123L);
        lock.setLockData("encrypted-lock-data");
        when(houseService.requireAvailableHouse(house.getId())).thenReturn(house);
        when(smartLockMapper.selectById("lock-1")).thenReturn(lock);

        AppointmentDtos.CreateResult result =
                service.createAppointment("tenant-1", "request-1", request(house.getId()));

        assertThat(result.viewingMode()).isEqualTo(ViewingMode.SELF_SERVICE_LOCK.name());
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.requiresConfirmation()).isFalse();
    }

    @Test
    void platformHouseWithoutUsableLockFallsBackToPlatformHosted() {
        House house = house("PLATFORM");
        when(houseService.requireAvailableHouse(house.getId())).thenReturn(house);

        AppointmentDtos.CreateResult result =
                service.createAppointment("tenant-1", null, request(house.getId()));

        assertThat(result.viewingMode()).isEqualTo(ViewingMode.PLATFORM_HOSTED.name());
        assertThat(result.status()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(result.requiresConfirmation()).isTrue();
    }

    @Test
    void landlordHouseNeverUsesPlatformLockByDefault() {
        House house = house("LANDLORD");
        house.setLandlordId("landlord-1");
        when(houseService.requireAvailableHouse(house.getId())).thenReturn(house);

        AppointmentDtos.CreateResult result =
                service.createAppointment("tenant-1", null, request(house.getId()));

        assertThat(result.viewingMode()).isEqualTo(ViewingMode.LANDLORD_HOSTED.name());
        assertThat(result.status()).isEqualTo("PENDING_CONFIRMATION");
    }

    private static House house(String sourceType) {
        House house = new House();
        house.setId("house-" + sourceType.toLowerCase());
        house.setTitle("测试房源");
        house.setSourceType(sourceType);
        house.setStatus("available");
        house.setIsSmartLockSupported(0);
        house.setIsSelfViewingSupported(0);
        house.setLockBindStatus("UNBOUND");
        return house;
    }

    private static AppointmentDtos.CreateRequest request(String houseId) {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return new AppointmentDtos.CreateRequest(
                houseId,
                OffsetDateTime.of(date.atTime(10, 0), ZoneOffset.ofHours(8)),
                null,
                null,
                "张女士",
                "13800138000",
                "希望准时看房"
        );
    }
}
