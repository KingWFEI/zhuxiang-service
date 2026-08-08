package com.zhuxiang.service;

import com.zhuxiang.service.common.AppointmentStatus;
import com.zhuxiang.service.common.BusinessException;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    @Test
    void selfServiceSlotsExposeServerControlledLockValidityRange() {
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
        when(appointmentMapper.selectList(any())).thenReturn(List.of());
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }

        AppointmentDtos.ViewingSlotResult result =
                service.getViewingSlots(house.getId(), date, 1, false);
        AppointmentDtos.ViewingSlot slot = result.dates().getFirst().slots().getFirst();

        assertThat(result.viewingMode()).isEqualTo(ViewingMode.SELF_SERVICE_LOCK.name());
        assertThat(result.requiresConfirmation()).isFalse();
        assertThat(slot.accessValidFrom()).isEqualTo(slot.startAt().minusMinutes(10));
        assertThat(slot.accessValidTo()).isEqualTo(slot.endAt().plusMinutes(10));
        assertThat(slot.testSlot()).isFalse();
    }

    @Test
    void developmentTestSlotCreatesAppointmentAtNextWholeHour() {
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
        when(appointmentMapper.selectList(any())).thenReturn(List.of());
        ReflectionTestUtils.setField(service, "testSlotEnabled", true);

        AppointmentDtos.ViewingSlot testSlot = service.getViewingSlots(
                        house.getId(), LocalDate.now(ZoneOffset.ofHours(8)), 2, true
                ).dates().stream()
                .flatMap(day -> day.slots().stream())
                .filter(AppointmentDtos.ViewingSlot::testSlot)
                .findFirst()
                .orElseThrow();
        assertThat(testSlot.available()).isTrue();
        assertThat(testSlot.startAt().getMinute()).isZero();
        assertThat(testSlot.startAt().getSecond()).isZero();
        assertThat(testSlot.startAt()).isAfter(OffsetDateTime.now(ZoneOffset.ofHours(8)));

        AppointmentDtos.CreateRequest base = request(house.getId());
        AppointmentDtos.CreateResult result = service.createAppointment(
                "tenant-1",
                "test-slot-request",
                new AppointmentDtos.CreateRequest(
                        base.houseId(),
                        base.appointmentStartAt(),
                        base.appointmentDate(),
                        base.timeSlot(),
                        base.contactName(),
                        base.contactPhone(),
                        base.remark(),
                        true
                )
        );
        assertThat(result.appointmentStartAt().getMinute()).isZero();
        assertThat(result.appointmentStartAt().getSecond()).isZero();
        assertThat(result.appointmentStartAt())
                .isAfter(OffsetDateTime.now(ZoneOffset.ofHours(8)));
        assertThat(result.viewingMode()).isEqualTo(ViewingMode.SELF_SERVICE_LOCK.name());
    }

    @Test
    void userCannotCreateAnotherActiveAppointmentForTheSameHouse() {
        House house = house("LANDLORD");
        house.setLandlordId("landlord-1");
        when(houseService.requireAvailableHouse(house.getId())).thenReturn(house);
        when(appointmentMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() ->
                service.createAppointment("tenant-1", null, request(house.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("请先取消已有预约");
        verify(appointmentMapper, never()).insert(any(Appointment.class));
    }

    @Test
    void terminalAppointmentReleasesUserHouseUniquenessKey() {
        Appointment appointment = new Appointment();
        appointment.setId("appointment-1");
        appointment.setUserId("tenant-1");
        appointment.setHouseId("house-1");
        appointment.setStatus(AppointmentStatus.CONFIRMED.name());
        appointment.setVersion(0);
        appointment.setActiveSlotKey("house-1|slot");
        appointment.setActiveUserHouseKey("tenant-1|house-1");
        when(appointmentMapper.updateWithStatusAndVersion(
                any(Appointment.class), anyString(), anyInt()
        )).thenReturn(1);

        ReflectionTestUtils.invokeMethod(
                service,
                "transition",
                appointment,
                AppointmentStatus.CANCELLED,
                "tenant-1",
                "TENANT",
                "用户取消"
        );

        assertThat(appointment.getActiveSlotKey()).isNull();
        assertThat(appointment.getActiveUserHouseKey()).isNull();
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
                "希望准时看房",
                false
        );
    }
}
