package com.zhuxiang.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AutoUnlockProperties;
import com.zhuxiang.service.dto.UnlockRecordDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.LockPermission;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.UnlockRecord;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.HouseMapper;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.mapper.UnlockRecordMapper;
import com.zhuxiang.service.mapper.UserMapper;
import com.zhuxiang.service.service.LockPermissionService;
import com.zhuxiang.service.service.impl.UnlockRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UnlockRecordServiceTests {

    private final LeaseMapper leaseMapper = mock(LeaseMapper.class);
    private final SmartLockMapper smartLockMapper = mock(SmartLockMapper.class);
    private final LockPermissionService permissionService = mock(LockPermissionService.class);
    private final UnlockRecordMapper recordMapper = mock(UnlockRecordMapper.class);
    private final AutoUnlockProperties properties = new AutoUnlockProperties();
    private final UserMapper userMapper = mock(UserMapper.class);
    private final HouseMapper houseMapper = mock(HouseMapper.class);
    private UnlockRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UnlockRecordServiceImpl(
                leaseMapper,
                smartLockMapper,
                permissionService,
                properties,
                userMapper,
                houseMapper
        );
        ReflectionTestUtils.setField(service, "baseMapper", recordMapper);
        when(recordMapper.insert(any(UnlockRecord.class))).thenReturn(1);
    }

    // ==================== record() tests ====================

    @Test
    void recordsAutoNearbyWithoutSensitiveData() {
        stubValidContext();
        UnlockRecordDtos.UnlockRecordRequest request = autoNearbyRequest("smart-lock-1", 123456L, "SUCCESS", null);

        UnlockRecordDtos.UnlockRecordResponse response = service.record("lease-1", "tenant-1", request);

        ArgumentCaptor<UnlockRecord> captor = ArgumentCaptor.forClass(UnlockRecord.class);
        verify(recordMapper).insert(captor.capture());
        UnlockRecord stored = captor.getValue();
        assertThat(response.id()).isEqualTo(stored.getId());
        assertThat(stored.getUserId()).isEqualTo("tenant-1");
        assertThat(stored.getLeaseId()).isEqualTo("lease-1");
        assertThat(stored.getSmartLockId()).isEqualTo("smart-lock-1");
        assertThat(stored.getTtlockLockId()).isEqualTo(123456L);
        assertThat(stored.getTriggerType()).isEqualTo("AUTO_NEARBY");
        assertThat(stored.getResult()).isEqualTo("SUCCESS");
        assertThat(stored.getFailureReason()).isNull();
        assertThat(stored.getRssi()).isEqualTo(-55);
        assertThat(stored.getStableMillis()).isEqualTo(2300);
        assertThat(stored.getDeviceInfo()).isEqualTo("android test-device");
    }

    @Test
    void recordsManualBluetoothWithoutRssiAndStableMillis() {
        stubValidContext();
        UnlockRecordDtos.UnlockRecordRequest request = manualBluetoothRequest("smart-lock-1", 123456L, "SUCCESS", null);

        UnlockRecordDtos.UnlockRecordResponse response = service.record("lease-1", "tenant-1", request);

        ArgumentCaptor<UnlockRecord> captor = ArgumentCaptor.forClass(UnlockRecord.class);
        verify(recordMapper).insert(captor.capture());
        UnlockRecord stored = captor.getValue();
        assertThat(response.id()).isEqualTo(stored.getId());
        assertThat(stored.getTriggerType()).isEqualTo("MANUAL_BLUETOOTH");
        assertThat(stored.getResult()).isEqualTo("SUCCESS");
        assertThat(stored.getRssi()).isNull();
        assertThat(stored.getStableMillis()).isNull();
        assertThat(stored.getDeviceInfo()).isEqualTo("android test-device");
    }

    @Test
    void manualBluetoothNotAffectedByAutoUnlockDisabledFlag() {
        stubValidContext();
        properties.setEnabled(false);

        UnlockRecordDtos.UnlockRecordRequest request = manualBluetoothRequest("smart-lock-1", 123456L, "SUCCESS", null);
        service.record("lease-1", "tenant-1", request);
        verify(recordMapper).insert(any(UnlockRecord.class));
    }

    @Test
    void autoNearbyRejectedWhenDisabled() {
        stubValidContext();
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.record(
                "lease-1",
                "tenant-1",
                autoNearbyRequest("smart-lock-1", 123456L, "SUCCESS", null)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(403));
    }

    @Test
    void autoNearbyRejectedWhenRssiMissing() {
        stubValidContext();
        UnlockRecordDtos.UnlockRecordRequest request = new UnlockRecordDtos.UnlockRecordRequest(
                "smart-lock-1", 123456L, "AUTO_NEARBY", null, 2300,
                "SUCCESS", null, "android test-device", "1.0.0+1"
        );

        assertThatThrownBy(() -> service.record("lease-1", "tenant-1", request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getMessage()).isEqualTo("无感开锁必须提供 rssi"));
    }

    @Test
    void autoNearbyRejectedWhenStableMillisMissing() {
        stubValidContext();
        UnlockRecordDtos.UnlockRecordRequest request = new UnlockRecordDtos.UnlockRecordRequest(
                "smart-lock-1", 123456L, "AUTO_NEARBY", -55, null,
                "SUCCESS", null, "android test-device", "1.0.0+1"
        );

        assertThatThrownBy(() -> service.record("lease-1", "tenant-1", request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getMessage()).isEqualTo("无感开锁必须提供 stableMillis"));
    }

    @Test
    void rejectsClientLockIdentityThatDoesNotMatchLease() {
        stubValidContext();

        assertThatThrownBy(() -> service.record(
                "lease-1",
                "tenant-1",
                autoNearbyRequest("other-lock", 999L, "FAILED", "LOCK_BUSY")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getCode()).isEqualTo(403));

        verify(recordMapper, never()).insert(any(UnlockRecord.class));
    }

    @Test
    void rejectsExpiredLeaseBeforeLookingUpLock() {
        Lease lease = lease();
        lease.setStatus("terminated");
        when(leaseMapper.selectById("lease-1")).thenReturn(lease);

        assertThatThrownBy(() -> service.record(
                "lease-1",
                "tenant-1",
                autoNearbyRequest("smart-lock-1", 123456L, "FAILED", "BLE_UNLOCK_FAILED")
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(403);
            assertThat(exception.getMessage()).isEqualTo("LEASE_INVALID");
        });

        verify(smartLockMapper, never()).selectLatestByHouseId(any());
        verify(recordMapper, never()).insert(any(UnlockRecord.class));
    }

    @Test
    void rejectsInvalidTriggerType() {
        stubValidContext();
        UnlockRecordDtos.UnlockRecordRequest request = new UnlockRecordDtos.UnlockRecordRequest(
                "smart-lock-1", 123456L, "INVALID_TYPE", -55, 2300,
                "SUCCESS", null, "android test-device", "1.0.0+1"
        );

        assertThatThrownBy(() -> service.record("lease-1", "tenant-1", request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getMessage()).isEqualTo("开锁日志类型不正确"));
    }

    @Test
    void rejectsInvalidResult() {
        stubValidContext();
        UnlockRecordDtos.UnlockRecordRequest request = new UnlockRecordDtos.UnlockRecordRequest(
                "smart-lock-1", 123456L, "AUTO_NEARBY", -55, 2300,
                "UNKNOWN", null, "android test-device", "1.0.0+1"
        );

        assertThatThrownBy(() -> service.record("lease-1", "tenant-1", request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getMessage()).isEqualTo("开锁结果不正确"));
    }

    // ==================== listMyRecords() tests ====================

    @Test
    void listMyRecordsReturnsEmptyWhenNoRecords() {
        when(recordMapper.selectList(any())).thenReturn(Collections.emptyList());

        UnlockRecordDtos.UnlockRecordListResponse result = service.listMyRecords("tenant-1");

        assertThat(result.total()).isEqualTo(0);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void listMyRecordsReturnsMappedItems() {
        UnlockRecord record = new UnlockRecord();
        record.setId("rec-1");
        record.setUserId("tenant-1");
        record.setLeaseId("lease-1");
        record.setSmartLockId("lock-1");
        record.setTriggerType("AUTO_NEARBY");
        record.setResult("SUCCESS");
        record.setFailureReason(null);
        record.setDeviceInfo("android 16 Xiaomi-14");
        record.setCreatedAt(LocalDateTime.of(2026, 7, 3, 14, 32));

        when(recordMapper.selectList(any())).thenReturn(List.of(record));

        User user = new User();
        user.setId("tenant-1");
        user.setNickname("王小明");
        when(userMapper.selectById("tenant-1")).thenReturn(user);

        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setHouseId("house-1");
        when(leaseMapper.selectBatchIds(any())).thenReturn(List.of(lease));

        House house = new House();
        house.setId("house-1");
        house.setTitle("3栋2单元1201");
        when(houseMapper.selectBatchIds(any())).thenReturn(List.of(house));

        SmartLock lock = new SmartLock();
        lock.setId("lock-1");
        lock.setLockName("客厅智能门锁");
        when(smartLockMapper.selectBatchIds(any())).thenReturn(List.of(lock));

        UnlockRecordDtos.UnlockRecordListResponse result = service.listMyRecords("tenant-1");

        assertThat(result.total()).isEqualTo(1);
        UnlockRecordDtos.UnlockRecordItem item = result.items().get(0);
        assertThat(item.id()).isEqualTo("rec-1");
        assertThat(item.houseId()).isEqualTo("house-1");
        assertThat(item.houseName()).isEqualTo("3栋2单元1201");
        assertThat(item.lockId()).isEqualTo("lock-1");
        assertThat(item.lockName()).isEqualTo("客厅智能门锁");
        assertThat(item.unlockMethod()).isEqualTo("bluetooth");
        assertThat(item.unlockResult()).isEqualTo("success");
        assertThat(item.unlockTime()).isEqualTo(LocalDateTime.of(2026, 7, 3, 14, 32));
        assertThat(item.operatorName()).isEqualTo("王小明");
        assertThat(item.operatorType()).isEqualTo("tenant");
        assertThat(item.failureReason()).isNull();
        assertThat(item.deviceName()).isEqualTo("Xiaomi-14");
        assertThat(item.remark()).isEqualTo("开锁指令执行完成");
    }

    @Test
    void listMyRecordsMapsManualBluetoothToBluetooth() {
        UnlockRecord record = new UnlockRecord();
        record.setId("rec-2");
        record.setUserId("tenant-1");
        record.setLeaseId("lease-1");
        record.setSmartLockId("lock-1");
        record.setTriggerType("MANUAL_BLUETOOTH");
        record.setResult("FAILED");
        record.setFailureReason("BLE_UNLOCK_FAILED");
        record.setDeviceInfo("iPhone 16");
        record.setCreatedAt(LocalDateTime.now());

        when(recordMapper.selectList(any())).thenReturn(List.of(record));
        when(userMapper.selectById("tenant-1")).thenReturn(null);
        when(leaseMapper.selectBatchIds(any())).thenReturn(List.of());
        when(smartLockMapper.selectBatchIds(any())).thenReturn(List.of());

        UnlockRecordDtos.UnlockRecordListResponse result = service.listMyRecords("tenant-1");

        assertThat(result.total()).isEqualTo(1);
        UnlockRecordDtos.UnlockRecordItem item = result.items().get(0);
        assertThat(item.unlockMethod()).isEqualTo("bluetooth");
        assertThat(item.unlockResult()).isEqualTo("failed");
        assertThat(item.failureReason()).isEqualTo("BLE_UNLOCK_FAILED");
        assertThat(item.deviceName()).isEqualTo("iPhone 16");
        assertThat(item.remark()).isEqualTo("BLE_UNLOCK_FAILED");
    }

    // ==================== getMyPermission() tests ====================

    @Test
    void getMyPermissionReturnsNullWhenNoLease() {
        when(leaseMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(null);

        UnlockRecordDtos.LockPermissionResponse result = service.getMyPermission("tenant-1");

        assertThat(result).isNull();
    }

    @Test
    void getMyPermissionReturnsNullWhenNoLock() {
        Lease lease = lease();
        when(leaseMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(lease);
        when(smartLockMapper.selectLatestByHouseId("house-1")).thenReturn(null);

        UnlockRecordDtos.LockPermissionResponse result = service.getMyPermission("tenant-1");

        assertThat(result).isNull();
    }

    @Test
    void getMyPermissionReturnsActiveStatus() {
        Lease lease = lease();
        when(leaseMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(lease);

        SmartLock lock = new SmartLock();
        lock.setId("lock-1");
        lock.setHouseId("house-1");
        lock.setLockName("客厅智能门锁");
        when(smartLockMapper.selectLatestByHouseId("house-1")).thenReturn(lock);

        House house = new House();
        house.setId("house-1");
        house.setTitle("3栋2单元1201");
        when(houseMapper.selectById("house-1")).thenReturn(house);

        LockPermission permission = new LockPermission();
        permission.setStatus("ACTIVE");
        permission.setStartTime(LocalDateTime.now().minusDays(1));
        permission.setEndTime(LocalDateTime.now().plusDays(1));
        when(permissionService.getOne(any(Wrapper.class), eq(false))).thenReturn(permission);

        UnlockRecord lastRecord = new UnlockRecord();
        lastRecord.setCreatedAt(LocalDateTime.of(2026, 7, 3, 14, 32));
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(lastRecord);

        UnlockRecordDtos.LockPermissionResponse result = service.getMyPermission("tenant-1");

        assertThat(result.houseName()).isEqualTo("3栋2单元1201");
        assertThat(result.lockName()).isEqualTo("客厅智能门锁");
        assertThat(result.permissionStatus()).isEqualTo("active");
        assertThat(result.lastUnlockTime()).isEqualTo(LocalDateTime.of(2026, 7, 3, 14, 32));
        assertThat(result.supportedMethods()).containsExactly("bluetooth", "password");
    }

    @Test
    void getMyPermissionReturnsPendingWhenNoPermission() {
        Lease lease = lease();
        when(leaseMapper.selectOne(any(Wrapper.class), eq(false))).thenReturn(lease);

        SmartLock lock = new SmartLock();
        lock.setId("lock-1");
        lock.setHouseId("house-1");
        lock.setLockName("门锁");
        when(smartLockMapper.selectLatestByHouseId("house-1")).thenReturn(lock);
        when(houseMapper.selectById("house-1")).thenReturn(null);
        when(permissionService.getOne(any(Wrapper.class), eq(false))).thenReturn(null);
        when(recordMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        UnlockRecordDtos.LockPermissionResponse result = service.getMyPermission("tenant-1");

        assertThat(result.permissionStatus()).isEqualTo("pending");
        assertThat(result.lastUnlockTime()).isNull();
    }

    // ==================== helpers ====================

    private void stubValidContext() {
        Lease lease = lease();
        SmartLock lock = new SmartLock();
        lock.setId("smart-lock-1");
        lock.setHouseId("house-1");
        lock.setLockId(123456L);
        lock.setStatus("BOUND");
        LockPermission permission = new LockPermission();
        permission.setLeaseId("lease-1");
        permission.setTenantId("tenant-1");
        permission.setSmartLockId("smart-lock-1");
        permission.setTtlockLockId(123456L);
        permission.setPermissionType("EKEY");
        permission.setStatus("ACTIVE");
        permission.setStartTime(LocalDateTime.now().minusDays(1));
        permission.setEndTime(LocalDateTime.now().plusDays(1));

        when(leaseMapper.selectById("lease-1")).thenReturn(lease);
        when(smartLockMapper.selectLatestByHouseId("house-1")).thenReturn(lock);
        when(permissionService.getOne(any(Wrapper.class), eq(false))).thenReturn(permission);
    }

    private Lease lease() {
        Lease lease = new Lease();
        lease.setId("lease-1");
        lease.setUserId("tenant-1");
        lease.setHouseId("house-1");
        lease.setStatus("active");
        return lease;
    }

    private UnlockRecordDtos.UnlockRecordRequest autoNearbyRequest(
            String smartLockId,
            Long ttlockLockId,
            String result,
            String failureReason
    ) {
        return new UnlockRecordDtos.UnlockRecordRequest(
                smartLockId,
                ttlockLockId,
                "AUTO_NEARBY",
                -55,
                2300,
                result,
                failureReason,
                "android\ntest-device",
                "1.0.0+1"
        );
    }

    private UnlockRecordDtos.UnlockRecordRequest manualBluetoothRequest(
            String smartLockId,
            Long ttlockLockId,
            String result,
            String failureReason
    ) {
        return new UnlockRecordDtos.UnlockRecordRequest(
                smartLockId,
                ttlockLockId,
                "MANUAL_BLUETOOTH",
                null,
                null,
                result,
                failureReason,
                "android\ntest-device",
                "1.0.0+1"
        );
    }
}
