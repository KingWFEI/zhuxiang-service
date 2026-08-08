package com.zhuxiang.service;

import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.TtLockPeriodPasscodeResponse;
import com.zhuxiang.service.dto.TtLockSendEKeyResponse;
import com.zhuxiang.service.entity.Appointment;
import com.zhuxiang.service.entity.AppointmentAccessGrant;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.AppointmentAccessGrantMapper;
import com.zhuxiang.service.mapper.AppointmentMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.security.LockPasscodeCrypto;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.AppointmentAccessGrantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentAccessGrantServiceTests {

    private final AppointmentMapper appointmentMapper = mock(AppointmentMapper.class);
    private final AppointmentAccessGrantMapper grantMapper = mock(AppointmentAccessGrantMapper.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private final UserService userService = mock(UserService.class);
    private final MessageService messageService = mock(MessageService.class);
    private final TtLockTokenService tokenService = mock(TtLockTokenService.class);
    private final TtLockOpenApiClient openApiClient = mock(TtLockOpenApiClient.class);
    private final LockPasscodeCrypto passcodeCrypto = mock(LockPasscodeCrypto.class);
    private final TtLockProperties properties = new TtLockProperties();
    private AppointmentAccessGrantServiceImpl service;

    @BeforeEach
    void setUp() {
        properties.setClientId("client-id");
        service = new AppointmentAccessGrantServiceImpl(
                appointmentMapper,
                grantMapper,
                smartLockMapper,
                userService,
                messageService,
                tokenService,
                openApiClient,
                properties,
                passcodeCrypto
        );
    }

    @Test
    void bluetoothKeepsBufferWhilePeriodPasscodeUsesWholeHours() {
        Appointment appointment = new Appointment();
        appointment.setId("appointment-1");
        appointment.setUserId("tenant-1");
        appointment.setHouseId("house-1");
        appointment.setViewingMode("SELF_SERVICE_LOCK");
        appointment.setStatus("CONFIRMED");
        appointment.setAppointmentStartAt(LocalDateTime.of(2026, 8, 3, 10, 0));
        appointment.setAppointmentEndAt(LocalDateTime.of(2026, 8, 3, 11, 0));

        SmartLock smartLock = new SmartLock();
        smartLock.setId("smart-lock-1");
        smartLock.setHouseId("house-1");
        smartLock.setStatus("BOUND");
        smartLock.setLockId(12345L);
        smartLock.setLockData("lock-data");
        smartLock.setKeyboardPwdVersion(4);

        User tenant = new User();
        tenant.setId("tenant-1");
        tenant.setPhone("13800138000");

        TtLockSendEKeyResponse ekey = new TtLockSendEKeyResponse();
        ekey.setKeyId(7001L);
        TtLockPeriodPasscodeResponse passcode = new TtLockPeriodPasscodeResponse();
        passcode.setKeyboardPwd("839204");
        passcode.setKeyboardPwdId(9001L);

        when(appointmentMapper.selectById("appointment-1")).thenReturn(appointment);
        when(smartLockMapper.selectOne(any())).thenReturn(smartLock);
        when(userService.requireActiveUser("tenant-1")).thenReturn(tenant);
        when(grantMapper.selectOne(any())).thenReturn(null);
        when(grantMapper.insert(any(AppointmentAccessGrant.class))).thenReturn(1);
        when(grantMapper.updateById(any(AppointmentAccessGrant.class))).thenReturn(1);
        when(tokenService.getAccessToken()).thenReturn("access-token");
        when(openApiClient.sendEKey(
                anyString(), anyString(), anyLong(), anyString(), anyString(), anyLong(), anyLong()
        )).thenReturn(ekey);
        when(openApiClient.getPeriodPasscode(
                anyString(), anyString(), anyLong(), eq(4), eq(3), anyString(), anyLong(), anyLong()
        )).thenReturn(passcode);
        when(passcodeCrypto.encrypt(eq("839204"), anyString())).thenReturn("encrypted-passcode");

        AppointmentAccessGrant grant = service.grantForAppointment("appointment-1");

        ZoneId zone = ZoneId.of("Asia/Shanghai");
        long bluetoothFrom = LocalDateTime.of(2026, 8, 3, 9, 50)
                .atZone(zone).toInstant().toEpochMilli();
        long bluetoothTo = LocalDateTime.of(2026, 8, 3, 11, 10)
                .atZone(zone).toInstant().toEpochMilli();
        long passcodeFrom = LocalDateTime.of(2026, 8, 3, 10, 0)
                .atZone(zone).toInstant().toEpochMilli();
        long passcodeTo = LocalDateTime.of(2026, 8, 3, 11, 0)
                .atZone(zone).toInstant().toEpochMilli();

        verify(openApiClient).sendEKey(
                eq("client-id"), eq("access-token"), eq(12345L),
                eq("13800138000"), anyString(), eq(bluetoothFrom), eq(bluetoothTo)
        );
        verify(openApiClient).getPeriodPasscode(
                eq("client-id"), eq("access-token"), eq(12345L),
                eq(4), eq(3), anyString(), eq(passcodeFrom), eq(passcodeTo)
        );
        assertThat(grant.getValidFrom()).isEqualTo(LocalDateTime.of(2026, 8, 3, 9, 50));
        assertThat(grant.getValidTo()).isEqualTo(LocalDateTime.of(2026, 8, 3, 11, 10));
        assertThat(grant.getPasscodeValidFrom()).isEqualTo(LocalDateTime.of(2026, 8, 3, 10, 0));
        assertThat(grant.getPasscodeValidTo()).isEqualTo(LocalDateTime.of(2026, 8, 3, 11, 0));
        assertThat(grant.getStatus()).isEqualTo("ACTIVE");
    }
}
