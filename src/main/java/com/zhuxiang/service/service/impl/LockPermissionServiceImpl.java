package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.common.BusinessException;
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
import com.zhuxiang.service.service.LockPermissionService;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 租客智能门锁权限服务实现。
 */
@Service
public class LockPermissionServiceImpl extends ServiceImpl<LockPermissionMapper, LockPermission>
        implements LockPermissionService {

    private static final String PERMISSION_TYPE_EKEY = "EKEY";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_REVOKE_FAILED = "REVOKE_FAILED";
    private static final String SMART_LOCK_STATUS_BOUND = "BOUND";
    private static final Set<String> EFFECTIVE_LEASE_STATUSES = Set.of("ACTIVE", "EFFECTIVE");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final LeaseMapper leaseMapper;
    private final UserService userService;
    private final HouseService houseService;
    private final SmartLockMapper smartLockMapper;
    private final TtLockTokenService tokenService;
    private final TtLockOpenApiClient openApiClient;
    private final TtLockProperties properties;

    public LockPermissionServiceImpl(
            LeaseMapper leaseMapper,
            UserService userService,
            HouseService houseService,
            SmartLockMapper smartLockMapper,
            TtLockTokenService tokenService,
            TtLockOpenApiClient openApiClient,
            TtLockProperties properties
    ) {
        this.leaseMapper = leaseMapper;
        this.userService = userService;
        this.houseService = houseService;
        this.smartLockMapper = smartLockMapper;
        this.tokenService = tokenService;
        this.openApiClient = openApiClient;
        this.properties = properties;
    }

    /**
     * 在独立事务中为生效租约下发eKey，平台失败只记录FAILED，不影响租约事务。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LockPermission grantTenantEKeyForLease(String leaseId) {
        Lease lease = requireEffectiveLease(leaseId);
        User tenant = userService.requireActiveUser(lease.getUserId());
        String receiverUsername = requireReceiverUsername(tenant);
        SmartLock smartLock = requireBoundSmartLock(lease.getHouseId());

        LockPermission placeholder = new LockPermission();
        placeholder.setId(UUID.randomUUID().toString());
        placeholder.setCreatedAt(LocalDateTime.now());
        populatePermission(placeholder, lease, tenant, smartLock, receiverUsername);
        placeholder.setStatus(STATUS_FAILED);
        placeholder.setErrorMessage("TTLock eKey等待下发");
        placeholder.setUpdatedAt(LocalDateTime.now());
        baseMapper.insertIfAbsent(placeholder);

        LockPermission permission = baseMapper.selectForUpdate(
                lease.getId(), tenant.getId(), smartLock.getId()
        );
        if (permission == null) {
            throw new IllegalStateException("eKey权限占位记录创建失败");
        }
        if (STATUS_ACTIVE.equalsIgnoreCase(permission.getStatus())) {
            return permission;
        }
        populatePermission(permission, lease, tenant, smartLock, receiverUsername);

        try {
            TtLockSendEKeyResponse response = openApiClient.sendEKey(
                    requireClientId(),
                    tokenService.getAccessToken(),
                    smartLock.getLockId(),
                    receiverUsername,
                    buildKeyName(lease, tenant, smartLock),
                    toStartTimestamp(lease),
                    toEndTimestamp(lease)
            );
            if (!response.success()) {
                markFailed(permission, platformError(response));
            } else {
                permission.setTtlockKeyId(response.getKeyId());
                permission.setStatus(STATUS_ACTIVE);
                permission.setErrorMessage(null);
            }
        } catch (RuntimeException exception) {
            markFailed(permission, safeErrorMessage(exception));
        }

        permission.setUpdatedAt(LocalDateTime.now());
        updateById(permission);
        return permission;
    }

    /**
     * 在独立事务中撤销该租约的 eKey。平台失败会落库为 REVOKE_FAILED，避免影响退租主流程。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeTenantEKeyForLease(String leaseId) {
        if (!StringUtils.hasText(leaseId)) {
            throw BusinessException.badRequest("leaseId不能为空");
        }
        list(Wrappers.<LockPermission>lambdaQuery()
                .eq(LockPermission::getLeaseId, leaseId)
                .eq(LockPermission::getPermissionType, PERMISSION_TYPE_EKEY)
                .in(LockPermission::getStatus, STATUS_ACTIVE, STATUS_FAILED, STATUS_REVOKE_FAILED)
        ).forEach(permission -> {
            if (permission.getTtlockKeyId() == null || !STATUS_ACTIVE.equalsIgnoreCase(permission.getStatus())) {
                permission.setStatus(STATUS_REVOKED);
                permission.setErrorMessage(null);
            } else {
                try {
                    TtLockOperationResponse response = openApiClient.deleteEKey(
                            requireClientId(), tokenService.getAccessToken(), permission.getTtlockKeyId()
                    );
                    if (response.success()) {
                        permission.setStatus(STATUS_REVOKED);
                        permission.setErrorMessage(null);
                    } else {
                        permission.setStatus(STATUS_REVOKE_FAILED);
                        permission.setErrorMessage(truncate(platformRevokeError(response), 500));
                    }
                } catch (RuntimeException exception) {
                    permission.setStatus(STATUS_REVOKE_FAILED);
                    permission.setErrorMessage(truncate(safeErrorMessage(exception), 500));
                }
            }
            permission.setUpdatedAt(LocalDateTime.now());
            updateById(permission);
        });
    }

    /**
     * 查询并校验租约已经生效。
     */
    private Lease requireEffectiveLease(String leaseId) {
        if (!StringUtils.hasText(leaseId)) {
            throw BusinessException.badRequest("leaseId不能为空");
        }
        Lease lease = leaseMapper.selectById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        String status = lease.getStatus() == null ? "" : lease.getStatus().toUpperCase();
        if (!EFFECTIVE_LEASE_STATUSES.contains(status)) {
            throw BusinessException.conflict("租约尚未生效，不能下发门锁eKey");
        }
        if (lease.getStartDate() == null || lease.getEndDate() == null) {
            throw BusinessException.badRequest("租约有效期不完整");
        }
        return lease;
    }

    /**
     * 获取并校验TTLock接收账号，当前用户表优先使用手机号。
     */
    private String requireReceiverUsername(User tenant) {
        String receiverUsername = tenant.getPhone();
        if (!StringUtils.hasText(receiverUsername)
                || (!PHONE_PATTERN.matcher(receiverUsername).matches()
                && !EMAIL_PATTERN.matcher(receiverUsername).matches())) {
            throw BusinessException.badRequest("租客手机号或邮箱格式不正确，无法接收TTLock eKey");
        }
        return receiverUsername;
    }

    /**
     * 查询房源当前已完成平台绑定的智能门锁。
     */
    private SmartLock requireBoundSmartLock(String houseId) {
        SmartLock smartLock = smartLockMapper.selectOne(
                Wrappers.<SmartLock>lambdaQuery()
                        .eq(SmartLock::getHouseId, houseId)
                        .eq(SmartLock::getStatus, SMART_LOCK_STATUS_BOUND)
                        .orderByDesc(SmartLock::getUpdatedAt)
                        .last("LIMIT 1")
        );
        if (smartLock == null) {
            throw BusinessException.notFound("房源未绑定状态为BOUND的智能门锁");
        }
        if (smartLock.getLockId() == null) {
            throw BusinessException.badRequest("智能门锁缺少TTLock lockId");
        }
        return smartLock;
    }

    /**
     * 填充本地权限记录的公共字段。
     */
    private void populatePermission(
            LockPermission permission,
            Lease lease,
            User tenant,
            SmartLock smartLock,
            String receiverUsername
    ) {
        permission.setLeaseId(lease.getId());
        permission.setTenantId(tenant.getId());
        permission.setHouseId(lease.getHouseId());
        permission.setSmartLockId(smartLock.getId());
        permission.setTtlockLockId(smartLock.getLockId());
        permission.setReceiverUsername(receiverUsername);
        permission.setPermissionType(PERMISSION_TYPE_EKEY);
        permission.setStartTime(lease.getStartDate().atStartOfDay());
        permission.setEndTime(lease.getEndDate().atTime(23, 59, 59));
    }

    /**
     * 生成可辨识的租客eKey名称。
     */
    private String buildKeyName(Lease lease, User tenant, SmartLock smartLock) {
        House house = houseService.getById(lease.getHouseId());
        String roomName = buildRoomName(house, smartLock);
        String tenantName = StringUtils.hasText(tenant.getNickname())
                ? tenant.getNickname().trim()
                : tenant.getPhone();
        return roomName + tenantName + "门锁钥匙";
    }

    private String buildRoomName(House house, SmartLock smartLock) {
        if (house == null) {
            return smartLock.getLockName();
        }
        StringBuilder name = new StringBuilder();
        appendText(name, house.getBuilding());
        appendText(name, house.getUnit());
        appendText(name, house.getRoom());
        return name.isEmpty() ? smartLock.getLockName() : name.toString();
    }

    private void appendText(StringBuilder target, String value) {
        if (StringUtils.hasText(value)) {
            target.append(value.trim());
        }
    }

    private long toStartTimestamp(Lease lease) {
        return lease.getStartDate().atStartOfDay(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    private long toEndTimestamp(Lease lease) {
        return lease.getEndDate().plusDays(1).atStartOfDay(BUSINESS_ZONE)
                .toInstant().toEpochMilli() - 1;
    }

    private String requireClientId() {
        if (!StringUtils.hasText(properties.getClientId())) {
            throw BusinessException.badRequest("TTLock clientId未配置");
        }
        return properties.getClientId();
    }

    private void markFailed(LockPermission permission, String message) {
        permission.setTtlockKeyId(null);
        permission.setStatus(STATUS_FAILED);
        permission.setErrorMessage(truncate(message, 500));
    }

    private String platformError(TtLockSendEKeyResponse response) {
        String code = response.getErrcode() == null ? "UNKNOWN" : response.getErrcode().toString();
        String message = StringUtils.hasText(response.getErrmsg()) ? response.getErrmsg() : "未返回错误信息";
        return "TTLock eKey下发失败[" + code + "]: " + message;
    }

    private String platformRevokeError(TtLockOperationResponse response) {
        String code = response.getErrcode() == null ? "UNKNOWN" : response.getErrcode().toString();
        String message = StringUtils.hasText(response.getErrmsg()) ? response.getErrmsg() : "未返回错误信息";
        return "TTLock eKey撤销失败[" + code + "]: " + message;
    }

    private String safeErrorMessage(RuntimeException exception) {
        return StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : "TTLock eKey下发发生未知错误";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
