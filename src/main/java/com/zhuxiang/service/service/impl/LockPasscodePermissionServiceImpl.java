package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.common.BusinessException;
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
import com.zhuxiang.service.service.LockPasscodePermissionService;
import com.zhuxiang.service.service.PasscodeQueryRateLimiter;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * TTLock 期限密码权限服务实现。
 */
@Service
public class LockPasscodePermissionServiceImpl
        extends ServiceImpl<LockPasscodePermissionMapper, LockPasscodePermission>
        implements LockPasscodePermissionService {

    static final int KEYBOARD_PWD_VERSION = 4;
    static final int KEYBOARD_PWD_TYPE_PERIOD = 3;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String SYNC_NOT_REQUIRED = "NOT_REQUIRED";
    private static final String SYNC_PENDING_BLUETOOTH_DELETE = "PENDING_BLUETOOTH_DELETE";
    private static final String SMART_LOCK_STATUS_BOUND = "BOUND";
    private static final String FIRST_USE_NOTICE = "请在密码生效后的24小时内至少使用一次";
    private static final Set<String> EFFECTIVE_LEASE_STATUSES = Set.of("ACTIVE", "EFFECTIVE");
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i)(accessToken|clientSecret|clientId|password|keyboardPwd)\\s*[:=]\\s*[^,\\s&]+"
    );

    private final LeaseMapper leaseMapper;
    private final UserService userService;
    private final HouseService houseService;
    private final SmartLockMapper smartLockMapper;
    private final TtLockTokenService tokenService;
    private final TtLockOpenApiClient openApiClient;
    private final TtLockProperties properties;
    private final LockPasscodeCrypto crypto;
    private final PasscodeQueryRateLimiter rateLimiter;
    private Clock clock = Clock.systemUTC();

    public LockPasscodePermissionServiceImpl(
            LeaseMapper leaseMapper,
            UserService userService,
            HouseService houseService,
            SmartLockMapper smartLockMapper,
            TtLockTokenService tokenService,
            TtLockOpenApiClient openApiClient,
            TtLockProperties properties,
            LockPasscodeCrypto crypto,
            PasscodeQueryRateLimiter rateLimiter
    ) {
        this.leaseMapper = leaseMapper;
        this.userService = userService;
        this.houseService = houseService;
        this.smartLockMapper = smartLockMapper;
        this.tokenService = tokenService;
        this.openApiClient = openApiClient;
        this.properties = properties;
        this.crypto = crypto;
        this.rateLimiter = rateLimiter;
    }

    /**
     * 在独立事务中串行化生成期限密码；所有可识别失败都会更新同一条 FAILED 记录。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LockPasscodePermission grantTenantPeriodPasscodeForLease(String leaseId) {
        Lease lease = requireEffectiveLease(leaseId);
        return generatePeriodPasscode(lease);
    }

    /**
     * 租客主动重试生成期限密码。使用独立限流键，避免频繁调用 TTLock 平台。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LockPasscodePermission retryTenantPeriodPasscodeForLease(String leaseId, String currentUserId) {
        if (!StringUtils.hasText(currentUserId)) {
            throw BusinessException.unauthorized("未登录或 Token 失效");
        }
        Lease lease = requireEffectiveLease(leaseId);
        if (!currentUserId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权重新生成该租约的门锁密码");
        }
        rateLimiter.check(currentUserId, leaseId + ":retry");
        return generatePeriodPasscode(lease);
    }

    private LockPasscodePermission generatePeriodPasscode(Lease lease) {
        User tenant = userService.requireActiveUser(lease.getUserId());
        SmartLock smartLock = requireBoundSmartLock(lease.getHouseId());

        LockPasscodePermission placeholder = newPlaceholder(lease, tenant, smartLock);
        baseMapper.insertIfAbsent(placeholder);
        LockPasscodePermission permission = baseMapper.selectForUpdate(lease.getId(), smartLock.getId());
        if (permission == null) {
            throw new IllegalStateException("期限密码权限占位记录创建失败");
        }
        if (STATUS_ACTIVE.equalsIgnoreCase(permission.getStatus())) {
            return permission;
        }

        populateOwnership(permission, lease, tenant, smartLock);
        try {
            TtLockDetailResponse detail = openApiClient.getLockDetail(
                    requireClientId(), tokenService.getAccessToken(), smartLock.getLockId()
            );
            requireSuccessfulDetail(detail);
            syncLockCapability(smartLock, detail);
            permission.setKeyboardPwdVersion(detail.getKeyboardPwdVersion());
            ZoneOffset lockOffset = requireLockOffset(detail.getTimezoneRawOffset());
            TimeWindow window = buildTimeWindow(lease, lockOffset);
            permission.setStartTime(window.start());
            permission.setEndTime(window.end());
            if (detail.getKeyboardPwdVersion() != KEYBOARD_PWD_VERSION) {
                throw BusinessException.conflict(
                        "门锁键盘密码版本为 " + detail.getKeyboardPwdVersion() + "，本期仅支持 V4 期限密码"
                );
            }

            crypto.validateConfiguration();

            TtLockPeriodPasscodeResponse response = openApiClient.getPeriodPasscode(
                    requireClientId(),
                    tokenService.getAccessToken(),
                    smartLock.getLockId(),
                    KEYBOARD_PWD_VERSION,
                    KEYBOARD_PWD_TYPE_PERIOD,
                    buildPasscodeName(lease, smartLock),
                    window.start().toEpochMilli(),
                    window.end().toEpochMilli()
            );
            if (!response.success()) {
                throw BusinessException.badRequest(platformError(response));
            }

            String ciphertext = crypto.encrypt(
                    response.getKeyboardPwd(), encryptionContext(lease.getId(), tenant.getId(), smartLock.getId())
            );
            permission.setTtlockKeyboardPwdId(response.getKeyboardPwdId());
            permission.setKeyboardPwdCiphertext(ciphertext);
            permission.setStatus(STATUS_ACTIVE);
            permission.setDeviceSyncStatus(SYNC_NOT_REQUIRED);
            permission.setErrorMessage(null);
        } catch (RuntimeException exception) {
            markFailed(permission, safeErrorMessage(exception));
        }
        permission.setUpdatedAt(LocalDateTime.now(clock));
        updateById(permission);
        return permission;
    }

    /**
     * 完成全部授权和时效校验后才解密明文密码。
     */
    @Override
    public LeaseLockPasscodeResponse getTenantPasscode(String leaseId, String currentUserId) {
        if (!StringUtils.hasText(currentUserId)) {
            throw BusinessException.unauthorized("未登录或 Token 失效");
        }
        rateLimiter.check(currentUserId, leaseId);
        Lease lease = requireLease(leaseId);
        if (!currentUserId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权查看该租约的门锁密码");
        }
        requireEffectiveStatus(lease, "当前租约状态不可查看门锁密码");
        SmartLock smartLock = requireBoundSmartLock(lease.getHouseId());
        ZoneOffset lockOffset = requireLockOffset(smartLock.getTimezoneRawOffset());
        Instant now = clock.instant();
        java.time.LocalDate lockDate = OffsetDateTime.ofInstant(now, lockOffset).toLocalDate();
        if (lease.getStartDate() == null || lease.getEndDate() == null
                || lockDate.isBefore(lease.getStartDate()) || lockDate.isAfter(lease.getEndDate())) {
            throw BusinessException.forbidden("当前时间不在租约有效期内");
        }

        LockPasscodePermission permission = getOne(
                Wrappers.<LockPasscodePermission>lambdaQuery()
                        .eq(LockPasscodePermission::getLeaseId, lease.getId())
                        .eq(LockPasscodePermission::getTenantId, currentUserId)
                        .eq(LockPasscodePermission::getSmartLockId, smartLock.getId())
                        .last("LIMIT 1"),
                false
        );
        if (permission == null) {
            throw BusinessException.notFound("租约门锁密码权限不存在");
        }
        if (!STATUS_ACTIVE.equalsIgnoreCase(permission.getStatus())) {
            throw BusinessException.forbidden("租约门锁密码权限当前不可用");
        }
        if (permission.getStartTime() == null || permission.getEndTime() == null
                || now.isBefore(permission.getStartTime()) || !now.isBefore(permission.getEndTime())) {
            throw BusinessException.forbidden("门锁密码不在有效期内");
        }

        String passcode = crypto.decrypt(
                permission.getKeyboardPwdCiphertext(),
                encryptionContext(lease.getId(), currentUserId, smartLock.getId())
        );
        House house = houseService.getById(lease.getHouseId());
        return new LeaseLockPasscodeResponse(
                lease.getId(),
                smartLock.getId(),
                buildRoomName(house, smartLock),
                passcode,
                "PERIOD",
                OffsetDateTime.ofInstant(permission.getStartTime(), lockOffset).toString(),
                OffsetDateTime.ofInstant(permission.getEndTime(), lockOffset).toString(),
                permission.getStatus(),
                FIRST_USE_NOTICE
        );
    }

    /** 正常到期只更新本地权限，不声明门锁侧执行了删除。 */
    @Override
    @Transactional
    public void expirePasscodesForLease(String leaseId) {
        lambdaUpdate()
                .eq(LockPasscodePermission::getLeaseId, leaseId)
                .in(LockPasscodePermission::getStatus, STATUS_ACTIVE, STATUS_FAILED)
                .set(LockPasscodePermission::getStatus, STATUS_EXPIRED)
                .set(LockPasscodePermission::getDeviceSyncStatus, SYNC_NOT_REQUIRED)
                .set(LockPasscodePermission::getUpdatedAt, LocalDateTime.now(clock))
                .update();
    }

    /** 提前结束仅标记待蓝牙删除，当前无网关时不调用远程删除接口。 */
    @Override
    @Transactional
    public void revokePasscodesForLease(String leaseId) {
        lambdaUpdate()
                .eq(LockPasscodePermission::getLeaseId, leaseId)
                .in(LockPasscodePermission::getStatus, STATUS_ACTIVE, STATUS_FAILED)
                .set(LockPasscodePermission::getStatus, STATUS_REVOKED)
                .set(LockPasscodePermission::getDeviceSyncStatus, SYNC_PENDING_BLUETOOTH_DELETE)
                .set(LockPasscodePermission::getUpdatedAt, LocalDateTime.now(clock))
                .update();
    }

    /** 扫描并标记所有已经自然到期的权限。 */
    @Override
    @Transactional
    public void expireDuePasscodes() {
        lambdaUpdate()
                .in(LockPasscodePermission::getStatus, STATUS_ACTIVE, STATUS_FAILED)
                .le(LockPasscodePermission::getEndTime, clock.instant())
                .set(LockPasscodePermission::getStatus, STATUS_EXPIRED)
                .set(LockPasscodePermission::getDeviceSyncStatus, SYNC_NOT_REQUIRED)
                .set(LockPasscodePermission::getUpdatedAt, LocalDateTime.now(clock))
                .update();
    }

    /** 构造并发安全生成所需的失败占位记录。 */
    private LockPasscodePermission newPlaceholder(Lease lease, User tenant, SmartLock smartLock) {
        LockPasscodePermission permission = new LockPasscodePermission();
        permission.setId(UUID.randomUUID().toString());
        populateOwnership(permission, lease, tenant, smartLock);
        permission.setKeyboardPwdType(KEYBOARD_PWD_TYPE_PERIOD);
        permission.setKeyboardPwdVersion(smartLock.getKeyboardPwdVersion());
        permission.setStatus(STATUS_FAILED);
        permission.setDeviceSyncStatus(SYNC_NOT_REQUIRED);
        permission.setErrorMessage("TTLock 期限密码等待生成");
        permission.setCreatedAt(LocalDateTime.now(clock));
        permission.setUpdatedAt(LocalDateTime.now(clock));
        return permission;
    }

    /** 填充权限归属和平台门锁字段。 */
    private void populateOwnership(
            LockPasscodePermission permission,
            Lease lease,
            User tenant,
            SmartLock smartLock
    ) {
        permission.setLeaseId(lease.getId());
        permission.setTenantId(tenant.getId());
        permission.setSmartLockId(smartLock.getId());
        permission.setTtlockLockId(smartLock.getLockId());
        permission.setKeyboardPwdType(KEYBOARD_PWD_TYPE_PERIOD);
    }

    /** 查询并校验可授权的生效租约。 */
    private Lease requireEffectiveLease(String leaseId) {
        Lease lease = requireLease(leaseId);
        requireEffectiveStatus(lease, "租约尚未生效，不能生成门锁密码");
        if (lease.getStartDate() == null || lease.getEndDate() == null) {
            throw BusinessException.badRequest("租约有效期不完整");
        }
        return lease;
    }

    /** 查询租约，不存在时返回统一 404。 */
    private Lease requireLease(String leaseId) {
        if (!StringUtils.hasText(leaseId)) {
            throw BusinessException.badRequest("leaseId 不能为空");
        }
        Lease lease = leaseMapper.selectById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        return lease;
    }

    /** 校验租约状态为 ACTIVE 或 EFFECTIVE。 */
    private void requireEffectiveStatus(Lease lease, String message) {
        String status = lease.getStatus() == null ? "" : lease.getStatus().toUpperCase(Locale.ROOT);
        if (!EFFECTIVE_LEASE_STATUSES.contains(status)) {
            throw BusinessException.conflict(message);
        }
    }

    /** 查询房间当前绑定的 TTLock 门锁。 */
    private SmartLock requireBoundSmartLock(String houseId) {
        SmartLock smartLock = smartLockMapper.selectOne(
                Wrappers.<SmartLock>lambdaQuery()
                        .eq(SmartLock::getHouseId, houseId)
                        .eq(SmartLock::getStatus, SMART_LOCK_STATUS_BOUND)
                        .orderByDesc(SmartLock::getUpdatedAt)
                        .last("LIMIT 1")
        );
        if (smartLock == null) {
            throw BusinessException.notFound("房间未绑定状态为 BOUND 的智能门锁");
        }
        if (smartLock.getLockId() == null) {
            throw BusinessException.badRequest("智能门锁缺少 TTLock lockId");
        }
        return smartLock;
    }

    /** 校验平台门锁详情包含密码版本和时区。 */
    private void requireSuccessfulDetail(TtLockDetailResponse detail) {
        if (detail == null) {
            throw BusinessException.badRequest("TTLock 门锁详情为空");
        }
        if (detail.getErrcode() != null && detail.getErrcode() != 0) {
            String message = StringUtils.hasText(detail.getErrmsg()) ? detail.getErrmsg() : "未知平台错误";
            throw BusinessException.badRequest("TTLock 门锁详情查询失败[" + detail.getErrcode() + "]: " + message);
        }
        if (detail.getKeyboardPwdVersion() == null) {
            throw BusinessException.conflict("TTLock 未返回门锁键盘密码版本，禁止按 V4 生成");
        }
        if (detail.getTimezoneRawOffset() == null) {
            throw BusinessException.conflict("TTLock 未返回门锁时区，无法计算密码有效期");
        }
    }

    /** 保存平台返回的门锁密码能力元数据。 */
    private void syncLockCapability(SmartLock smartLock, TtLockDetailResponse detail) {
        smartLock.setKeyboardPwdVersion(detail.getKeyboardPwdVersion());
        smartLock.setTimezoneRawOffset(detail.getTimezoneRawOffset());
        smartLock.setUpdatedAt(LocalDateTime.now(clock));
        smartLockMapper.updateById(smartLock);
    }

    /** 将平台毫秒偏移转换为受校验的门锁时区偏移。 */
    private ZoneOffset requireLockOffset(Long rawOffsetMillis) {
        if (rawOffsetMillis == null || rawOffsetMillis % 1000 != 0) {
            throw BusinessException.conflict("门锁时区偏移量无效");
        }
        try {
            return ZoneOffset.ofTotalSeconds(Math.toIntExact(rawOffsetMillis / 1000));
        } catch (ArithmeticException | DateTimeException exception) {
            throw BusinessException.conflict("门锁时区偏移量超出有效范围");
        }
    }

    /** 按门锁时区构造整点且不超过一年的有效期。 */
    private TimeWindow buildTimeWindow(Lease lease, ZoneOffset lockOffset) {
        LocalDateTime startLocal = lease.getStartDate().atStartOfDay();
        LocalDateTime endLocal = lease.getEndDate().plusDays(1).atStartOfDay();
        if (!endLocal.isAfter(startLocal)) {
            throw BusinessException.conflict("租约结束时间必须晚于开始时间");
        }
        if (endLocal.isAfter(startLocal.plusYears(1))) {
            throw BusinessException.conflict("V4 期限密码有效期不能超过一年");
        }
        return new TimeWindow(startLocal.toInstant(lockOffset), endLocal.toInstant(lockOffset));
    }

    /** 读取后端管理的 TTLock clientId。 */
    private String requireClientId() {
        if (!StringUtils.hasText(properties.getClientId())) {
            throw new IllegalStateException("TTLOCK_CLIENT_ID 未配置");
        }
        return properties.getClientId();
    }

    /** 生成不含密码内容的平台密码名称。 */
    private String buildPasscodeName(Lease lease, SmartLock smartLock) {
        return "租约-" + lease.getId() + "-" + smartLock.getLockName();
    }

    /** 构造租客可读的房间名称。 */
    private String buildRoomName(House house, SmartLock smartLock) {
        if (house == null) {
            return smartLock.getLockName();
        }
        StringBuilder name = new StringBuilder();
        appendText(name, house.getBuilding());
        appendText(name, house.getUnit());
        appendText(name, StringUtils.hasText(smartLock.getRoomId()) ? smartLock.getRoomId() : house.getRoom());
        return name.isEmpty() ? smartLock.getLockName() : name.toString();
    }

    /** 追加非空房间名称片段。 */
    private void appendText(StringBuilder target, String value) {
        if (StringUtils.hasText(value)) {
            target.append(value.trim());
        }
    }

    /** 生成防止密文跨租约替换的认证上下文。 */
    private String encryptionContext(String leaseId, String tenantId, String smartLockId) {
        return leaseId + ":" + tenantId + ":" + smartLockId;
    }

    /** 构造不含明文密码的平台失败原因。 */
    private String platformError(TtLockPeriodPasscodeResponse response) {
        String code = response.getErrcode() == null ? "INVALID_RESPONSE" : response.getErrcode().toString();
        String message = StringUtils.hasText(response.getErrmsg()) ? response.getErrmsg() : "平台未返回完整密码数据";
        return "TTLock 期限密码生成失败[" + code + "]: " + message;
    }

    /** 清除密码数据并标记本次生成失败。 */
    private void markFailed(LockPasscodePermission permission, String message) {
        permission.setTtlockKeyboardPwdId(null);
        permission.setKeyboardPwdCiphertext(null);
        permission.setStatus(STATUS_FAILED);
        permission.setDeviceSyncStatus(SYNC_NOT_REQUIRED);
        permission.setErrorMessage(truncate(message, 500));
    }

    /** 脱敏异常文本，禁止凭证进入数据库。 */
    private String safeErrorMessage(RuntimeException exception) {
        String message = StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : "TTLock 期限密码生成发生未知错误";
        return SENSITIVE_VALUE.matcher(message).replaceAll("$1=***");
    }

    /** 截断数据库错误文本。 */
    private String truncate(String value, int maxLength) {
        return value != null && value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private record TimeWindow(Instant start, Instant end) {
    }
}
