package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.LockPasscodeProperties;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.LeaseLockPasscodeResponse;
import com.zhuxiang.service.dto.TtLockDetailResponse;
import com.zhuxiang.service.dto.TtLockPeriodPasscodeResponse;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.LockPasscodePermission;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.LockPasscodePermissionMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.security.LockPasscodeCrypto;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.PasscodeQueryRateLimiter;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.LockPasscodePermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LockPasscodePermissionServiceTests {

    private static final Instant NOW = Instant.parse("2026-07-10T00:00:00Z");
    private final LeaseMapper leaseMapper = mock(LeaseMapper.class);
    private final UserService userService = mock(UserService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private final LockPasscodePermissionMapper permissionMapper = mock(LockPasscodePermissionMapper.class);
    private final TtLockTokenService tokenService = mock(TtLockTokenService.class);
    private final TtLockOpenApiClient openApiClient = mock(TtLockOpenApiClient.class);
    private final PasscodeQueryRateLimiter rateLimiter = mock(PasscodeQueryRateLimiter.class);
    private final TtLockProperties ttLockProperties = new TtLockProperties();
    private final LockPasscodeProperties passcodeProperties = new LockPasscodeProperties();
    private final AtomicReference<LockPasscodePermission> storedPermission = new AtomicReference<>();
    private LockPasscodeCrypto crypto;
    private LockPasscodePermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        ttLockProperties.setClientId("client-id");
        passcodeProperties.setEncryptionKey(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
        ));
        passcodeProperties.setKeyVersion("7");
        crypto = new LockPasscodeCrypto(passcodeProperties);
        service = new LockPasscodePermissionServiceImpl(
                leaseMapper, userService, houseService, smartLockMapper, tokenService,
                openApiClient, ttLockProperties, crypto, rateLimiter
        );
        ReflectionTestUtils.setField(service, "baseMapper", permissionMapper);
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(NOW, ZoneOffset.UTC));
        storedPermission.set(null);
        when(permissionMapper.insertIfAbsent(any(LockPasscodePermission.class))).thenAnswer(invocation -> {
            storedPermission.compareAndSet(null, invocation.getArgument(0));
            return 1;
        });
        when(permissionMapper.selectForUpdate(anyString(), anyString()))
                .thenAnswer(invocation -> storedPermission.get());
        when(permissionMapper.updateById(any(LockPasscodePermission.class))).thenReturn(1);
        when(tokenService.getAccessToken()).thenReturn("access-token");
    }

    @Test
    void generatesV4PeriodPasscodeAtWholeHoursInLockTimezoneAndStoresOnlyCiphertext() {
        stubContext(activeLease(), tenant(), boundLock());
        when(openApiClient.getLockDetail("client-id", "access-token", 12345L)).thenReturn(v4Detail());
        when(openApiClient.getPeriodPasscode(
                eq("client-id"), eq("access-token"), eq(12345L), eq(4), eq(3),
                anyString(), anyLong(), anyLong()
        )).thenReturn(successPasscode("839204", 9001L));

        LockPasscodePermission result = service.grantTenantPeriodPasscodeForLease("lease-1");

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getKeyboardPwdCiphertext()).startsWith("v7:").doesNotContain("839204");
        assertThat(result.toString()).doesNotContain("839204").doesNotContain(result.getKeyboardPwdCiphertext());
        assertThat(crypto.decrypt(result.getKeyboardPwdCiphertext(), "lease-1:tenant-1:smart-lock-1"))
                .isEqualTo("839204");
        assertThat(result.getStartTime()).isEqualTo(Instant.parse("2026-06-30T16:00:00Z"));
        assertThat(result.getEndTime()).isEqualTo(Instant.parse("2027-06-30T16:00:00Z"));
        verify(openApiClient).getPeriodPasscode(
                "client-id", "access-token", 12345L, 4, 3,
                "租约-lease-1-1508门锁",
                Instant.parse("2026-06-30T16:00:00Z").toEpochMilli(),
                Instant.parse("2027-06-30T16:00:00Z").toEpochMilli()
        );
    }

    @Test
    void repeatedGrantReturnsExistingActivePermissionWithoutCallingPlatformAgain() {
        stubSuccessfulGeneration();
        LockPasscodePermission first = service.grantTenantPeriodPasscodeForLease("lease-1");
        LockPasscodePermission second = service.grantTenantPeriodPasscodeForLease("lease-1");

        assertThat(second).isSameAs(first);
        verify(openApiClient, times(1)).getPeriodPasscode(
                anyString(), anyString(), any(Long.class), anyInt(), anyInt(), anyString(), anyLong(), anyLong()
        );
    }

    @Test
    void concurrentGrantIsSerializedAndGeneratesOnlyOnce() throws Exception {
        stubSuccessfulGeneration();
        Semaphore rowLock = new Semaphore(1);
        when(permissionMapper.selectForUpdate(anyString(), anyString())).thenAnswer(invocation -> {
            rowLock.acquire();
            return storedPermission.get();
        });
        when(permissionMapper.updateById(any(LockPasscodePermission.class))).thenAnswer(invocation -> {
            rowLock.release();
            return 1;
        });
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<LockPasscodePermission> one = executor.submit(() -> {
                start.await();
                return service.grantTenantPeriodPasscodeForLease("lease-1");
            });
            Future<LockPasscodePermission> two = executor.submit(() -> {
                start.await();
                return service.grantTenantPeriodPasscodeForLease("lease-1");
            });
            start.countDown();
            assertThat(one.get(3, TimeUnit.SECONDS).getStatus()).isEqualTo("ACTIVE");
            assertThat(two.get(3, TimeUnit.SECONDS).getStatus()).isEqualTo("ACTIVE");
            verify(openApiClient, times(1)).getPeriodPasscode(
                    anyString(), anyString(), any(Long.class), anyInt(), anyInt(), anyString(), anyLong(), anyLong()
            );
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsNonV4WithoutTreatingTypeThreeAsPeriodPasscode() {
        stubContext(activeLease(), tenant(), boundLock());
        TtLockDetailResponse detail = v4Detail();
        detail.setKeyboardPwdVersion(3);
        when(openApiClient.getLockDetail("client-id", "access-token", 12345L)).thenReturn(detail);

        LockPasscodePermission result = service.grantTenantPeriodPasscodeForLease("lease-1");

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).contains("仅支持 V4");
        verify(openApiClient, never()).getPeriodPasscode(
                anyString(), anyString(), any(Long.class), anyInt(), anyInt(), anyString(), anyLong(), anyLong()
        );
    }

    @Test
    void rejectsPeriodLongerThanOneCalendarYearWithoutTruncating() {
        Lease lease = activeLease();
        lease.setEndDate(LocalDate.of(2027, 7, 1));
        stubContext(lease, tenant(), boundLock());
        when(openApiClient.getLockDetail("client-id", "access-token", 12345L)).thenReturn(v4Detail());

        LockPasscodePermission result = service.grantTenantPeriodPasscodeForLease("lease-1");

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).contains("不能超过一年");
        verify(openApiClient, never()).getPeriodPasscode(
                anyString(), anyString(), any(Long.class), anyInt(), anyInt(), anyString(), anyLong(), anyLong()
        );
    }

    @Test
    void platformFailureIsPersistedAndDoesNotEscapeGrantMethod() {
        stubContext(activeLease(), tenant(), boundLock());
        when(openApiClient.getLockDetail("client-id", "access-token", 12345L)).thenReturn(v4Detail());
        TtLockPeriodPasscodeResponse failure = new TtLockPeriodPasscodeResponse();
        failure.setErrcode(-3002);
        failure.setErrmsg("unsupported period");
        when(openApiClient.getPeriodPasscode(
                anyString(), anyString(), any(Long.class), anyInt(), anyInt(), anyString(), anyLong(), anyLong()
        )).thenReturn(failure);

        LockPasscodePermission result = service.grantTenantPeriodPasscodeForLease("lease-1");

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).contains("-3002").contains("unsupported period");
        assertThat(result.getKeyboardPwdCiphertext()).isNull();
    }

    @Test
    void ownerCanViewButOtherUserAndInvalidOrRevokedLeaseCannotView() {
        Lease lease = activeLease();
        SmartLock smartLock = boundLock();
        smartLock.setTimezoneRawOffset(28_800_000L);
        stubContext(lease, tenant(), smartLock);
        House house = new House();
        house.setId("house-1");
        house.setBuilding("8栋");
        house.setRoom("1508");
        when(houseService.getById("house-1")).thenReturn(house);
        LockPasscodePermission active = activePermission("839204");
        when(permissionMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(active);

        LeaseLockPasscodeResponse response = service.getTenantPasscode("lease-1", "tenant-1");
        assertThat(response.passcode()).isEqualTo("839204");
        assertThat(response.firstUseNotice()).isEqualTo("请在密码生效后的24小时内至少使用一次");
        assertThat(response.toString()).doesNotContain("839204");

        assertThatThrownBy(() -> service.getTenantPasscode("lease-1", "other-user"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode()).isEqualTo(403);

        lease.setStatus("terminated");
        assertThatThrownBy(() -> service.getTenantPasscode("lease-1", "tenant-1"))
                .isInstanceOf(BusinessException.class);

        lease.setStatus("active");
        lease.setEndDate(LocalDate.of(2026, 7, 8));
        assertThatThrownBy(() -> service.getTenantPasscode("lease-1", "tenant-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode()).isEqualTo(403);

        lease.setEndDate(LocalDate.of(2027, 6, 30));
        active.setStatus("REVOKED");
        assertThatThrownBy(() -> service.getTenantPasscode("lease-1", "tenant-1"))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getCode()).isEqualTo(403);
        active.setStatus("EXPIRED");
        assertThatThrownBy(() -> service.getTenantPasscode("lease-1", "tenant-1"))
                .isInstanceOf(BusinessException.class);
    }

    private void stubSuccessfulGeneration() {
        stubContext(activeLease(), tenant(), boundLock());
        when(openApiClient.getLockDetail("client-id", "access-token", 12345L)).thenReturn(v4Detail());
        when(openApiClient.getPeriodPasscode(
                anyString(), anyString(), any(Long.class), anyInt(), anyInt(), anyString(), anyLong(), anyLong()
        )).thenReturn(successPasscode("839204", 9001L));
    }

    private void stubContext(Lease lease, User user, SmartLock smartLock) {
        when(leaseMapper.selectById("lease-1")).thenReturn(lease);
        when(userService.requireActiveUser("tenant-1")).thenReturn(user);
        when(smartLockMapper.selectOne(any(Wrapper.class))).thenReturn(smartLock);
        when(smartLockMapper.updateById(any(SmartLock.class))).thenReturn(1);
    }

    private Lease activeLease() {
        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setUserId("tenant-1");
        lease.setHouseId("house-1");
        lease.setStatus("active");
        lease.setStartDate(LocalDate.of(2026, 7, 1));
        lease.setEndDate(LocalDate.of(2027, 6, 30));
        return lease;
    }

    private User tenant() {
        User user = new User();
        user.setId("tenant-1");
        user.setStatus("active");
        return user;
    }

    private SmartLock boundLock() {
        SmartLock lock = new SmartLock();
        lock.setId("smart-lock-1");
        lock.setHouseId("house-1");
        lock.setLockId(12345L);
        lock.setLockName("1508门锁");
        lock.setStatus("BOUND");
        return lock;
    }

    private TtLockDetailResponse v4Detail() {
        TtLockDetailResponse detail = new TtLockDetailResponse();
        detail.setKeyboardPwdVersion(4);
        detail.setTimezoneRawOffset(28_800_000L);
        return detail;
    }

    private TtLockPeriodPasscodeResponse successPasscode(String passcode, long id) {
        TtLockPeriodPasscodeResponse response = new TtLockPeriodPasscodeResponse();
        response.setKeyboardPwd(passcode);
        response.setKeyboardPwdId(id);
        return response;
    }

    private LockPasscodePermission activePermission(String passcode) {
        LockPasscodePermission permission = new LockPasscodePermission();
        permission.setId("passcode-permission-1");
        permission.setLeaseId("lease-1");
        permission.setTenantId("tenant-1");
        permission.setSmartLockId("smart-lock-1");
        permission.setStatus("ACTIVE");
        permission.setStartTime(Instant.parse("2026-06-30T16:00:00Z"));
        permission.setEndTime(Instant.parse("2027-06-30T16:00:00Z"));
        permission.setKeyboardPwdCiphertext(crypto.encrypt(passcode, "lease-1:tenant-1:smart-lock-1"));
        return permission;
    }
}
