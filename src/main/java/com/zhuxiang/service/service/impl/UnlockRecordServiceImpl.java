package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.zhuxiang.service.service.UnlockRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 服务端校验门锁身份后写入开锁审计记录（手动蓝牙 + 无感）。 */
@Service
public class UnlockRecordServiceImpl
        extends ServiceImpl<UnlockRecordMapper, UnlockRecord>
        implements UnlockRecordService {

    private static final Set<String> USABLE_LOCK_STATUSES = Set.of("BOUND", "PLATFORM_BOUND");
    private static final Set<String> ALLOWED_TRIGGER_TYPES = Set.of("MANUAL_BLUETOOTH", "AUTO_NEARBY");

    private final LeaseMapper leaseMapper;
    private final SmartLockMapper smartLockMapper;
    private final LockPermissionService lockPermissionService;
    private final AutoUnlockProperties properties;
    private final UserMapper userMapper;
    private final HouseMapper houseMapper;

    public UnlockRecordServiceImpl(
            LeaseMapper leaseMapper,
            SmartLockMapper smartLockMapper,
            LockPermissionService lockPermissionService,
            AutoUnlockProperties properties,
            UserMapper userMapper,
            HouseMapper houseMapper
    ) {
        this.leaseMapper = leaseMapper;
        this.smartLockMapper = smartLockMapper;
        this.lockPermissionService = lockPermissionService;
        this.properties = properties;
        this.userMapper = userMapper;
        this.houseMapper = houseMapper;
    }

    @Override
    @Transactional
    public UnlockRecordDtos.UnlockRecordResponse record(
            String leaseId,
            String currentUserId,
            UnlockRecordDtos.UnlockRecordRequest request
    ) {
        String triggerType = request.triggerType();
        if (!ALLOWED_TRIGGER_TYPES.contains(triggerType)) {
            throw BusinessException.badRequest("开锁日志类型不正确");
        }
        if (!"SUCCESS".equals(request.result()) && !"FAILED".equals(request.result())) {
            throw BusinessException.badRequest("开锁结果不正确");
        }
        if ("AUTO_NEARBY".equals(triggerType)) {
            if (!properties.isEnabled()) {
                throw BusinessException.forbidden("AUTO_UNLOCK_DISABLED");
            }
            if (request.rssi() == null) {
                throw BusinessException.badRequest("无感开锁必须提供 rssi");
            }
            if (request.stableMillis() == null) {
                throw BusinessException.badRequest("无感开锁必须提供 stableMillis");
            }
        }

        Lease lease = leaseMapper.selectById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        if (!currentUserId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权记录该租约的开锁结果");
        }
        if (!isEffectiveLease(lease)) {
            throw BusinessException.forbidden("LEASE_INVALID");
        }

        SmartLock smartLock = smartLockMapper.selectLatestByHouseId(lease.getHouseId());
        if (smartLock == null || !USABLE_LOCK_STATUSES.contains(smartLock.getStatus())) {
            throw BusinessException.conflict("当前租约未绑定可用门锁");
        }
        if (!smartLock.getId().equals(request.smartLockId())
                || smartLock.getLockId() == null
                || !smartLock.getLockId().equals(request.ttlockLockId())) {
            throw BusinessException.forbidden("门锁身份与当前租约不匹配");
        }

        LockPermission permission = lockPermissionService.getOne(
                Wrappers.<LockPermission>lambdaQuery()
                        .eq(LockPermission::getLeaseId, leaseId)
                        .eq(LockPermission::getTenantId, currentUserId)
                        .eq(LockPermission::getSmartLockId, smartLock.getId())
                        .eq(LockPermission::getTtlockLockId, smartLock.getLockId())
                        .eq(LockPermission::getPermissionType, "EKEY")
                        .last("LIMIT 1"),
                false
        );
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        if (permission == null
                || !"ACTIVE".equalsIgnoreCase(permission.getStatus())
                || permission.getStartTime() == null
                || permission.getEndTime() == null
                || now.isBefore(permission.getStartTime())
                || now.isAfter(permission.getEndTime())) {
            throw BusinessException.forbidden("LEASE_INVALID");
        }

        UnlockRecord record = new UnlockRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(currentUserId);
        record.setLeaseId(leaseId);
        record.setSmartLockId(smartLock.getId());
        record.setTtlockLockId(smartLock.getLockId());
        record.setTriggerType(triggerType);
        record.setRssi(request.rssi());
        record.setStableMillis(request.stableMillis());
        record.setResult(request.result());
        record.setFailureReason(
                "SUCCESS".equals(request.result())
                        ? null
                        : defaultFailureReason(request.failureReason())
        );
        record.setDeviceInfo(normalizeText(request.deviceInfo()));
        record.setAppVersion(normalizeText(request.appVersion()));
        record.setCreatedAt(now);
        if (!save(record)) {
            throw new IllegalStateException("开锁日志写入失败");
        }
        return new UnlockRecordDtos.UnlockRecordResponse(record.getId(), record.getCreatedAt());
    }

    @Override
    public UnlockRecordDtos.UnlockRecordListResponse listMyRecords(String userId) {
        List<UnlockRecord> records = list(
                Wrappers.<UnlockRecord>lambdaQuery()
                        .eq(UnlockRecord::getUserId, userId)
                        .orderByDesc(UnlockRecord::getCreatedAt)
        );

        if (records.isEmpty()) {
            return new UnlockRecordDtos.UnlockRecordListResponse(0, Collections.emptyList());
        }

        // 当前用户
        User currentUser = userMapper.selectById(userId);
        String operatorName = currentUser != null && currentUser.getNickname() != null
                ? currentUser.getNickname()
                : "";

        // 批量加载关联数据
        List<String> leaseIds = records.stream()
                .map(UnlockRecord::getLeaseId)
                .distinct()
                .collect(Collectors.toList());
        List<String> lockIds = records.stream()
                .map(UnlockRecord::getSmartLockId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, Lease> leaseMap = leaseMapper.selectBatchIds(leaseIds).stream()
                .collect(Collectors.toMap(Lease::getId, Function.identity(), (a, b) -> a));
        Map<String, SmartLock> lockMap = smartLockMapper.selectBatchIds(lockIds).stream()
                .collect(Collectors.toMap(SmartLock::getId, Function.identity(), (a, b) -> a));

        List<String> houseIds = leaseMap.values().stream()
                .map(Lease::getHouseId)
                .distinct()
                .collect(Collectors.toList());
        Map<String, House> houseMap = houseIds.isEmpty()
                ? Collections.emptyMap()
                : houseMapper.selectBatchIds(houseIds).stream()
                        .collect(Collectors.toMap(House::getId, Function.identity(), (a, b) -> a));

        List<UnlockRecordDtos.UnlockRecordItem> items = new ArrayList<>();
        for (UnlockRecord record : records) {
            Lease lease = leaseMap.get(record.getLeaseId());
            String houseId = lease != null ? lease.getHouseId() : null;
            House house = houseId != null ? houseMap.get(houseId) : null;

            SmartLock lock = lockMap.get(record.getSmartLockId());

            items.add(new UnlockRecordDtos.UnlockRecordItem(
                    record.getId(),
                    houseId,
                    house != null ? house.getTitle() : "",
                    record.getSmartLockId(),
                    lock != null ? lock.getLockName() : "",
                    mapUnlockMethod(record.getTriggerType()),
                    mapUnlockResult(record.getResult()),
                    record.getCreatedAt(),
                    operatorName,
                    "tenant",
                    record.getFailureReason(),
                    extractDeviceName(record.getDeviceInfo()),
                    buildRemark(record.getResult(), record.getFailureReason())
            ));
        }

        return new UnlockRecordDtos.UnlockRecordListResponse(items.size(), items);
    }

    @Override
    public UnlockRecordDtos.LockPermissionResponse getMyPermission(String userId) {
        Lease lease = leaseMapper.selectOne(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getUserId, userId)
                        .in(Lease::getStatus, "active", "pending")
                        .orderByDesc(Lease::getCreatedAt)
                        .last("LIMIT 1"),
                false
        );
        if (lease == null) {
            return null;
        }

        SmartLock lock = smartLockMapper.selectLatestByHouseId(lease.getHouseId());
        if (lock == null) {
            return null;
        }

        House house = houseMapper.selectById(lease.getHouseId());

        LockPermission permission = lockPermissionService.getOne(
                Wrappers.<LockPermission>lambdaQuery()
                        .eq(LockPermission::getTenantId, userId)
                        .eq(LockPermission::getLeaseId, lease.getId())
                        .eq(LockPermission::getSmartLockId, lock.getId())
                        .last("LIMIT 1"),
                false
        );

        String permissionStatus = mapPermissionStatus(permission);

        UnlockRecord lastRecord = getBaseMapper().selectOne(
                Wrappers.<UnlockRecord>lambdaQuery()
                        .eq(UnlockRecord::getUserId, userId)
                        .orderByDesc(UnlockRecord::getCreatedAt)
                        .last("LIMIT 1")
        );
        LocalDateTime lastTime = lastRecord != null ? lastRecord.getCreatedAt() : null;

        List<String> supportedMethods = new ArrayList<>();
        supportedMethods.add("bluetooth");
        supportedMethods.add("password");

        return new UnlockRecordDtos.LockPermissionResponse(
                house != null ? house.getTitle() : "",
                lock.getLockName() != null ? lock.getLockName() : "",
                permissionStatus,
                lastTime,
                supportedMethods
        );
    }

    private String mapPermissionStatus(LockPermission permission) {
        if (permission == null) {
            return "pending";
        }
        if (!"ACTIVE".equalsIgnoreCase(permission.getStatus())) {
            return "pending";
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        if (permission.getEndTime() != null && now.isAfter(permission.getEndTime())) {
            return "expired";
        }
        if (permission.getStartTime() != null && now.isBefore(permission.getStartTime())) {
            return "pending";
        }
        return "active";
    }

    private String mapUnlockMethod(String triggerType) {
        return switch (triggerType) {
            case "MANUAL_BLUETOOTH", "AUTO_NEARBY" -> "bluetooth";
            default -> triggerType.toLowerCase();
        };
    }

    private String mapUnlockResult(String result) {
        return switch (result) {
            case "SUCCESS" -> "success";
            case "FAILED" -> "failed";
            default -> result.toLowerCase();
        };
    }

    private String extractDeviceName(String deviceInfo) {
        if (deviceInfo == null || deviceInfo.isBlank()) {
            return null;
        }
        // deviceInfo 格式如 "android 16 Xiaomi-14"，提取最后的设备名
        String[] parts = deviceInfo.trim().split("\\s+");
        if (parts.length >= 3) {
            return parts[parts.length - 1];
        }
        return deviceInfo.trim();
    }

    private String buildRemark(String result, String failureReason) {
        if ("SUCCESS".equals(result)) {
            return "开锁指令执行完成";
        }
        return failureReason != null ? failureReason : "开锁失败";
    }

    private boolean isEffectiveLease(Lease lease) {
        return "active".equalsIgnoreCase(lease.getStatus())
                || "effective".equalsIgnoreCase(lease.getStatus());
    }

    private String defaultFailureReason(String failureReason) {
        return failureReason == null || failureReason.isBlank()
                ? "BLE_UNLOCK_FAILED"
                : failureReason;
    }

    private String normalizeText(String value) {
        return value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
