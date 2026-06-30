package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.TtLockSendEKeyResponse;
import com.zhuxiang.service.dto.TtLockOperationResponse;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.LockPermission;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.LockPermissionMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.impl.LockPermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LockPermissionServiceTests {

    private final LeaseMapper leaseMapper = mock(LeaseMapper.class);
    private final UserService userService = mock(UserService.class);
    private final HouseService houseService = mock(HouseService.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private final LockPermissionMapper permissionMapper = mock(LockPermissionMapper.class);
    private final TtLockTokenService tokenService = mock(TtLockTokenService.class);
    private final TtLockOpenApiClient openApiClient = mock(TtLockOpenApiClient.class);
    private final TtLockProperties properties = new TtLockProperties();
    private final AtomicReference<LockPermission> storedPermission = new AtomicReference<>();
    private LockPermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        properties.setClientId("client-id");
        service = new LockPermissionServiceImpl(
                leaseMapper,
                userService,
                houseService,
                smartLockMapper,
                tokenService,
                openApiClient,
                properties
        );
        ReflectionTestUtils.setField(service, "baseMapper", permissionMapper);
        storedPermission.set(null);
        when(permissionMapper.insertIfAbsent(any(LockPermission.class))).thenAnswer(invocation -> {
            storedPermission.compareAndSet(null, invocation.getArgument(0));
            return 1;
        });
        when(permissionMapper.selectForUpdate(any(String.class), any(String.class), any(String.class)))
                .thenAnswer(invocation -> storedPermission.get());
    }

    @Test
    void grantsTenantEKeyAndPersistsActivePermission() {
        Lease lease = activeLease();
        User tenant = tenant();
        SmartLock smartLock = boundSmartLock();
        stubGrantContext(lease, tenant, smartLock);
        when(permissionMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());
        when(permissionMapper.updateById(any(LockPermission.class))).thenReturn(1);
        when(tokenService.getAccessToken()).thenReturn("access-token");

        TtLockSendEKeyResponse response = new TtLockSendEKeyResponse();
        response.setKeyId(9876L);
        response.setErrcode(0);
        when(openApiClient.sendEKey(
                eq("client-id"),
                eq("access-token"),
                eq(12345L),
                eq("13800138000"),
                eq("8栋1508King门锁钥匙"),
                anyLong(),
                anyLong()
        )).thenReturn(response);

        LockPermission permission = service.grantTenantEKeyForLease(lease.getId());

        assertThat(permission.getStatus()).isEqualTo("ACTIVE");
        assertThat(permission.getLeaseId()).isEqualTo("lease-1");
        assertThat(permission.getTenantId()).isEqualTo("tenant-1");
        assertThat(permission.getHouseId()).isEqualTo("house_006");
        assertThat(permission.getSmartLockId()).isEqualTo("smart-lock-1");
        assertThat(permission.getTtlockLockId()).isEqualTo(12345L);
        assertThat(permission.getTtlockKeyId()).isEqualTo(9876L);
        assertThat(permission.getReceiverUsername()).isEqualTo("13800138000");
        assertThat(permission.getPermissionType()).isEqualTo("EKEY");
        assertThat(permission.getErrorMessage()).isNull();

        long expectedStart = LocalDate.of(2026, 7, 1)
                .atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        long expectedEnd = LocalDate.of(2027, 7, 1)
                .atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli() - 1;
        verify(openApiClient).sendEKey(
                "client-id",
                "access-token",
                12345L,
                "13800138000",
                "8栋1508King门锁钥匙",
                expectedStart,
                expectedEnd
        );
        verify(permissionMapper).insertIfAbsent(permission);
        verify(permissionMapper).updateById(permission);
    }

    @Test
    void platformFailureIsPersistedWithoutThrowing() {
        Lease lease = activeLease();
        stubGrantContext(lease, tenant(), boundSmartLock());
        when(permissionMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());
        when(permissionMapper.updateById(any(LockPermission.class))).thenReturn(1);
        when(tokenService.getAccessToken()).thenReturn("access-token");

        TtLockSendEKeyResponse response = new TtLockSendEKeyResponse();
        response.setErrcode(-1003);
        response.setErrmsg("receiver account invalid");
        when(openApiClient.sendEKey(
                eq("client-id"), eq("access-token"), eq(12345L),
                eq("13800138000"), any(String.class), anyLong(), anyLong()
        )).thenReturn(response);

        LockPermission permission = service.grantTenantEKeyForLease(lease.getId());

        assertThat(permission.getStatus()).isEqualTo("FAILED");
        assertThat(permission.getTtlockKeyId()).isNull();
        assertThat(permission.getErrorMessage()).contains("-1003").contains("receiver account invalid");
        verify(permissionMapper).insertIfAbsent(permission);
        verify(permissionMapper).updateById(permission);
    }

    @Test
    void existingActivePermissionPreventsDuplicatePlatformCall() {
        Lease lease = activeLease();
        stubGrantContext(lease, tenant(), boundSmartLock());
        LockPermission existing = new LockPermission();
        existing.setId("permission-1");
        existing.setStatus("ACTIVE");
        when(permissionMapper.insertIfAbsent(any(LockPermission.class))).thenReturn(0);
        when(permissionMapper.selectForUpdate("lease-1", "tenant-1", "smart-lock-1")).thenReturn(existing);

        LockPermission result = service.grantTenantEKeyForLease(lease.getId());

        assertThat(result).isSameAs(existing);
        verify(openApiClient, never()).sendEKey(
                any(String.class), any(String.class), any(Long.class), any(String.class),
                any(String.class), anyLong(), anyLong()
        );
        verify(permissionMapper).insertIfAbsent(any(LockPermission.class));
        verify(permissionMapper, never()).updateById(any(LockPermission.class));
    }

    @Test
    void revokesActiveEKeyFromPlatformAndUpdatesLocalPermission() {
        LockPermission permission = new LockPermission();
        permission.setId("permission-1");
        permission.setLeaseId("lease-1");
        permission.setPermissionType("EKEY");
        permission.setStatus("ACTIVE");
        permission.setTtlockKeyId(9876L);
        when(permissionMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(permission));
        when(permissionMapper.updateById(permission)).thenReturn(1);
        when(tokenService.getAccessToken()).thenReturn("access-token");
        TtLockOperationResponse response = new TtLockOperationResponse();
        response.setErrcode(0);
        when(openApiClient.deleteEKey("client-id", "access-token", 9876L)).thenReturn(response);

        service.revokeTenantEKeyForLease("lease-1");

        assertThat(permission.getStatus()).isEqualTo("REVOKED");
        assertThat(permission.getErrorMessage()).isNull();
        verify(openApiClient).deleteEKey("client-id", "access-token", 9876L);
        verify(permissionMapper).updateById(permission);
    }

    private void stubGrantContext(Lease lease, User tenant, SmartLock smartLock) {
        House house = new House();
        house.setId("house_006");
        house.setBuilding("8栋");
        house.setRoom("1508");
        when(leaseMapper.selectById(lease.getId())).thenReturn(lease);
        when(userService.requireActiveUser(tenant.getId())).thenReturn(tenant);
        when(smartLockMapper.selectOne(any(Wrapper.class))).thenReturn(smartLock);
        when(houseService.getById(lease.getHouseId())).thenReturn(house);
    }

    private Lease activeLease() {
        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setUserId("tenant-1");
        lease.setHouseId("house_006");
        lease.setStatus("active");
        lease.setStartDate(LocalDate.of(2026, 7, 1));
        lease.setEndDate(LocalDate.of(2027, 6, 30));
        return lease;
    }

    private User tenant() {
        User user = new User();
        user.setId("tenant-1");
        user.setPhone("13800138000");
        user.setNickname("King");
        user.setStatus("active");
        return user;
    }

    private SmartLock boundSmartLock() {
        SmartLock smartLock = new SmartLock();
        smartLock.setId("smart-lock-1");
        smartLock.setHouseId("house_006");
        smartLock.setLockId(12345L);
        smartLock.setLockName("1508门锁");
        smartLock.setStatus("BOUND");
        return smartLock;
    }
}
