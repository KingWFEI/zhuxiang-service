package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.common.AppointmentAccessStatus;
import com.zhuxiang.service.common.AppointmentStatus;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.ViewingMode;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.dto.TtLockDetailResponse;
import com.zhuxiang.service.dto.TtLockOperationResponse;
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
import com.zhuxiang.service.service.AppointmentAccessGrantService;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import com.zhuxiang.service.service.MessageService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

@Service
public class AppointmentAccessGrantServiceImpl implements AppointmentAccessGrantService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset BUSINESS_OFFSET = ZoneOffset.ofHours(8);
    private static final int PERIOD_PASSCODE_TYPE = 3;
    private static final int SUPPORTED_KEYBOARD_VERSION = 4;

    private final AppointmentMapper appointmentMapper;
    private final AppointmentAccessGrantMapper grantMapper;
    private final SmartLockMapper smartLockMapper;
    private final UserService userService;
    private final MessageService messageService;
    private final TtLockTokenService tokenService;
    private final TtLockOpenApiClient openApiClient;
    private final TtLockProperties properties;
    private final LockPasscodeCrypto passcodeCrypto;

    public AppointmentAccessGrantServiceImpl(
            AppointmentMapper appointmentMapper,
            AppointmentAccessGrantMapper grantMapper,
            SmartLockMapper smartLockMapper,
            UserService userService,
            MessageService messageService,
            TtLockTokenService tokenService,
            TtLockOpenApiClient openApiClient,
            TtLockProperties properties,
            LockPasscodeCrypto passcodeCrypto
    ) {
        this.appointmentMapper = appointmentMapper;
        this.grantMapper = grantMapper;
        this.smartLockMapper = smartLockMapper;
        this.userService = userService;
        this.messageService = messageService;
        this.tokenService = tokenService;
        this.openApiClient = openApiClient;
        this.properties = properties;
        this.passcodeCrypto = passcodeCrypto;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AppointmentAccessGrant grantForAppointment(String appointmentId) {
        Appointment appointment = requireSelfServiceAppointment(appointmentId);
        AppointmentStatus status = AppointmentStatus.from(appointment.getStatus());
        if (status != AppointmentStatus.CONFIRMED
                && status != AppointmentStatus.READY
                && status != AppointmentStatus.IN_PROGRESS) {
            throw BusinessException.conflict("预约尚未确认，不能生成开门权限");
        }
        SmartLock smartLock = requireBoundSmartLock(appointment);
        User tenant = userService.requireActiveUser(appointment.getUserId());
        String receiver = requireReceiver(tenant);
        AppointmentAccessGrant grant = findGrant(appointmentId);
        if (grant == null) {
            grant = createPlaceholder(appointment, smartLock, receiver);
        }
        if (AppointmentAccessStatus.ACTIVE.name().equals(grant.getStatus())) {
            return grant;
        }

        String previousStatus = grant.getStatus();
        String accessToken = tokenService.getAccessToken();
        grantEKey(grant, appointment, smartLock, receiver, accessToken);
        grantPasscode(grant, appointment, smartLock, accessToken);
        updateOverallStatus(grant);
        grant.setRetryCount((grant.getRetryCount() == null ? 0 : grant.getRetryCount()) + 1);
        grant.setNextRetryAt(
                AppointmentAccessStatus.FAILED.name().equals(grant.getStatus())
                        || AppointmentAccessStatus.PARTIAL.name().equals(grant.getStatus())
                        ? LocalDateTime.now().plusMinutes(retryDelay(grant.getRetryCount()))
                        : null
        );
        grant.setGrantedAt(
                AppointmentAccessStatus.ACTIVE.name().equals(grant.getStatus())
                        || AppointmentAccessStatus.PARTIAL.name().equals(grant.getStatus())
                        ? LocalDateTime.now()
                        : null
        );
        grant.setUpdatedAt(LocalDateTime.now());
        grantMapper.updateById(grant);
        notifyAccessResult(appointment, previousStatus, grant.getStatus());
        return grant;
    }

    @Override
    public AppointmentDtos.AccessView getTenantAccess(
            String userId, String appointmentId
    ) {
        Appointment appointment = appointmentMapper.selectOne(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getId, appointmentId)
                        .eq(Appointment::getUserId, userId)
        );
        if (appointment == null) {
            throw BusinessException.notFound("预约不存在");
        }
        if (!ViewingMode.SELF_SERVICE_LOCK.name().equals(appointment.getViewingMode())) {
            throw BusinessException.forbidden("该预约不是智能锁自助看房");
        }
        AppointmentStatus status = AppointmentStatus.from(appointment.getStatus());
        if (status != AppointmentStatus.READY && status != AppointmentStatus.IN_PROGRESS) {
            throw BusinessException.conflict("开门凭证尚未进入可用时间");
        }
        AppointmentAccessGrant grant = findGrant(appointmentId);
        if (grant == null) {
            throw BusinessException.conflict("开门凭证尚未准备完成");
        }
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (now.isBefore(grant.getValidFrom()) || now.isAfter(grant.getValidTo())) {
            throw BusinessException.forbidden("不在预约开门授权时间内");
        }
        SmartLock smartLock = smartLockMapper.selectById(grant.getSmartLockId());
        if (smartLock == null || !"BOUND".equalsIgnoreCase(smartLock.getStatus())) {
            throw BusinessException.conflict("房源门锁当前不可用");
        }
        boolean bluetoothEnabled = "ACTIVE".equals(grant.getEkeyStatus())
                && StringUtils.hasText(smartLock.getLockData());
        boolean passcodeAvailable = "ACTIVE".equals(grant.getPasscodeStatus())
                && StringUtils.hasText(grant.getKeyboardPwdCiphertext());
        String passcode = passcodeAvailable
                ? passcodeCrypto.decrypt(
                        grant.getKeyboardPwdCiphertext(),
                        cryptoContext(grant.getId())
                )
                : null;
        return new AppointmentDtos.AccessView(
                grant.getStatus(),
                grant.getValidFrom().atOffset(BUSINESS_OFFSET),
                grant.getValidTo().atOffset(BUSINESS_OFFSET),
                new AppointmentDtos.BluetoothAccess(
                        bluetoothEnabled,
                        bluetoothEnabled ? smartLock.getLockMac() : null,
                        bluetoothEnabled ? smartLock.getLockData() : null
                ),
                new AppointmentDtos.PasscodeAccess(passcodeAvailable, passcode)
        );
    }

    @Override
    public AppointmentAccessGrant retry(String appointmentId) {
        AppointmentAccessGrant grant = findGrant(appointmentId);
        if (grant != null && AppointmentAccessStatus.ACTIVE.name().equals(grant.getStatus())) {
            return grant;
        }
        return grantForAppointment(appointmentId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(String appointmentId) {
        AppointmentAccessGrant grant = findGrant(appointmentId);
        if (grant == null
                || AppointmentAccessStatus.REVOKED.name().equals(grant.getStatus())
                || AppointmentAccessStatus.EXPIRED.name().equals(grant.getStatus())) {
            return;
        }
        grant.setStatus(AppointmentAccessStatus.REVOKING.name());
        grant.setUpdatedAt(LocalDateTime.now());
        grantMapper.updateById(grant);
        if (grant.getTtlockKeyId() != null && "ACTIVE".equals(grant.getEkeyStatus())) {
            try {
                TtLockOperationResponse response = openApiClient.deleteEKey(
                        requireClientId(),
                        tokenService.getAccessToken(),
                        grant.getTtlockKeyId()
                );
                if (response.success()) {
                    grant.setEkeyStatus("REVOKED");
                    grant.setEkeyErrorMessage(null);
                } else {
                    grant.setEkeyStatus("REVOKE_FAILED");
                    grant.setEkeyErrorMessage(safePlatformError(
                            response.getErrcode(), response.getErrmsg()
                    ));
                }
            } catch (RuntimeException exception) {
                grant.setEkeyStatus("REVOKE_FAILED");
                grant.setEkeyErrorMessage(safeExceptionMessage(exception));
            }
        } else {
            grant.setEkeyStatus("REVOKED");
        }

        // TTLock period passcodes cannot currently be deleted through the existing
        // project client. Hide the credential immediately and rely on its fixed
        // device-side end time; retain an explicit state for operational follow-up.
        grant.setPasscodeStatus(
                grant.getTtlockKeyboardPwdId() == null ? "REVOKED" : "PENDING_DEVICE_EXPIRY"
        );
        grant.setKeyboardPwdCiphertext(null);
        if ("REVOKE_FAILED".equals(grant.getEkeyStatus())) {
            grant.setStatus(AppointmentAccessStatus.REVOKING.name());
            grant.setNextRetryAt(LocalDateTime.now().plusMinutes(3));
        } else {
            grant.setStatus(AppointmentAccessStatus.REVOKED.name());
            grant.setRevokedAt(LocalDateTime.now());
            grant.setNextRetryAt(null);
        }
        grant.setUpdatedAt(LocalDateTime.now());
        grantMapper.updateById(grant);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(String appointmentId) {
        AppointmentAccessGrant grant = findGrant(appointmentId);
        if (grant == null) {
            return;
        }
        grant.setStatus(AppointmentAccessStatus.EXPIRED.name());
        grant.setEkeyStatus(
                "ACTIVE".equals(grant.getEkeyStatus()) ? "EXPIRED" : grant.getEkeyStatus()
        );
        grant.setPasscodeStatus(
                "ACTIVE".equals(grant.getPasscodeStatus()) ? "EXPIRED" : grant.getPasscodeStatus()
        );
        grant.setKeyboardPwdCiphertext(null);
        grant.setUpdatedAt(LocalDateTime.now());
        grantMapper.updateById(grant);
    }

    private AppointmentAccessGrant createPlaceholder(
            Appointment appointment, SmartLock smartLock, String receiver
    ) {
        AppointmentAccessGrant grant = new AppointmentAccessGrant();
        grant.setId(UUID.randomUUID().toString());
        grant.setAppointmentId(appointment.getId());
        grant.setTenantId(appointment.getUserId());
        grant.setHouseId(appointment.getHouseId());
        grant.setSmartLockId(smartLock.getId());
        grant.setTtlockLockId(smartLock.getLockId());
        grant.setValidFrom(appointment.getAppointmentStartAt().minusMinutes(10));
        grant.setValidTo(appointment.getAppointmentEndAt().plusMinutes(10));
        grant.setStatus(AppointmentAccessStatus.PENDING.name());
        grant.setEkeyStatus("PENDING");
        grant.setPasscodeStatus("PENDING");
        grant.setReceiverUsername(receiver);
        grant.setRetryCount(0);
        grant.setCreatedAt(LocalDateTime.now());
        grant.setUpdatedAt(LocalDateTime.now());
        try {
            grantMapper.insert(grant);
            return grant;
        } catch (DuplicateKeyException exception) {
            AppointmentAccessGrant existing = findGrant(appointment.getId());
            if (existing == null) {
                throw exception;
            }
            return existing;
        }
    }

    private void grantEKey(
            AppointmentAccessGrant grant,
            Appointment appointment,
            SmartLock smartLock,
            String receiver,
            String accessToken
    ) {
        if ("ACTIVE".equals(grant.getEkeyStatus())) {
            return;
        }
        try {
            TtLockSendEKeyResponse response = openApiClient.sendEKey(
                    requireClientId(),
                    accessToken,
                    smartLock.getLockId(),
                    receiver,
                    "勿忧管家-预约看房-" + shortId(appointment.getId()),
                    toEpochMillis(grant.getValidFrom()),
                    toEpochMillis(grant.getValidTo())
            );
            if (response.success()) {
                grant.setTtlockKeyId(response.getKeyId());
                grant.setEkeyStatus("ACTIVE");
                grant.setEkeyErrorMessage(null);
            } else {
                grant.setEkeyStatus("FAILED");
                grant.setEkeyErrorMessage(safePlatformError(
                        response.getErrcode(), response.getErrmsg()
                ));
            }
        } catch (RuntimeException exception) {
            grant.setEkeyStatus("FAILED");
            grant.setEkeyErrorMessage(safeExceptionMessage(exception));
        }
    }

    private void grantPasscode(
            AppointmentAccessGrant grant,
            Appointment appointment,
            SmartLock smartLock,
            String accessToken
    ) {
        if ("ACTIVE".equals(grant.getPasscodeStatus())) {
            return;
        }
        try {
            passcodeCrypto.validateConfiguration();
            int version = resolveKeyboardVersion(smartLock, accessToken);
            if (version != SUPPORTED_KEYBOARD_VERSION) {
                throw BusinessException.conflict("当前门锁密码版本不支持期限密码");
            }
            TtLockPeriodPasscodeResponse response = openApiClient.getPeriodPasscode(
                    requireClientId(),
                    accessToken,
                    smartLock.getLockId(),
                    version,
                    PERIOD_PASSCODE_TYPE,
                    "勿忧管家预约" + shortId(appointment.getId()),
                    toEpochMillis(grant.getValidFrom()),
                    toEpochMillis(grant.getValidTo())
            );
            if (response.success()) {
                grant.setTtlockKeyboardPwdId(response.getKeyboardPwdId());
                grant.setKeyboardPwdType(PERIOD_PASSCODE_TYPE);
                grant.setKeyboardPwdCiphertext(
                        passcodeCrypto.encrypt(
                                response.getKeyboardPwd(),
                                cryptoContext(grant.getId())
                        )
                );
                grant.setPasscodeStatus("ACTIVE");
                grant.setPasscodeErrorMessage(null);
            } else {
                grant.setPasscodeStatus("FAILED");
                grant.setPasscodeErrorMessage(safePlatformError(
                        response.getErrcode(), response.getErrmsg()
                ));
            }
        } catch (RuntimeException exception) {
            grant.setPasscodeStatus("FAILED");
            grant.setPasscodeErrorMessage(safeExceptionMessage(exception));
        }
    }

    private int resolveKeyboardVersion(SmartLock smartLock, String accessToken) {
        if (smartLock.getKeyboardPwdVersion() != null) {
            return smartLock.getKeyboardPwdVersion();
        }
        TtLockDetailResponse detail = openApiClient.getLockDetail(
                requireClientId(), accessToken, smartLock.getLockId()
        );
        if (!detail.success()) {
            throw BusinessException.conflict("无法读取门锁密码版本");
        }
        smartLock.setKeyboardPwdVersion(detail.getKeyboardPwdVersion());
        smartLock.setTimezoneRawOffset(detail.getTimezoneRawOffset());
        smartLock.setUpdatedAt(LocalDateTime.now());
        smartLockMapper.updateById(smartLock);
        return detail.getKeyboardPwdVersion();
    }

    private void updateOverallStatus(AppointmentAccessGrant grant) {
        boolean ekey = "ACTIVE".equals(grant.getEkeyStatus());
        boolean passcode = "ACTIVE".equals(grant.getPasscodeStatus());
        if (ekey && passcode) {
            grant.setStatus(AppointmentAccessStatus.ACTIVE.name());
        } else if (ekey || passcode) {
            grant.setStatus(AppointmentAccessStatus.PARTIAL.name());
        } else {
            grant.setStatus(AppointmentAccessStatus.FAILED.name());
        }
    }

    private void notifyAccessResult(
            Appointment appointment,
            String previousStatus,
            String currentStatus
    ) {
        if (currentStatus.equals(previousStatus)) {
            return;
        }
        if (AppointmentAccessStatus.ACTIVE.name().equals(currentStatus)
                || AppointmentAccessStatus.PARTIAL.name().equals(currentStatus)) {
            messageService.sendMessage(
                    appointment.getUserId(),
                    "appointment",
                    "自助看房凭证已准备",
                    "本次预约的开门凭证已准备完成，请在有效时间内使用",
                    "appointment",
                    appointment.getId()
            );
            return;
        }
        if (AppointmentAccessStatus.FAILED.name().equals(currentStatus)) {
            messageService.sendMessage(
                    appointment.getUserId(),
                    "appointment",
                    "自助看房凭证准备失败",
                    "开门凭证暂未准备成功，请联系客服处理",
                    "appointment",
                    appointment.getId()
            );
            userService.list(
                    Wrappers.<User>lambdaQuery()
                            .in(User::getRole, "ADMIN", "HOUSEKEEPER")
                            .eq(User::getStatus, "active")
            ).forEach(user -> messageService.sendMessage(
                    user.getId(),
                    "appointment",
                    "预约门锁授权失败",
                    "有一笔预约未能生成开门凭证，请进入管理端处理",
                    "appointment",
                    appointment.getId()
            ));
        }
    }

    private Appointment requireSelfServiceAppointment(String appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw BusinessException.notFound("预约不存在");
        }
        if (!ViewingMode.SELF_SERVICE_LOCK.name().equals(appointment.getViewingMode())) {
            throw BusinessException.conflict("该预约不需要智能锁权限");
        }
        return appointment;
    }

    private SmartLock requireBoundSmartLock(Appointment appointment) {
        SmartLock smartLock = smartLockMapper.selectOne(
                Wrappers.<SmartLock>lambdaQuery()
                        .eq(SmartLock::getHouseId, appointment.getHouseId())
                        .eq(SmartLock::getStatus, "BOUND")
                        .orderByDesc(SmartLock::getUpdatedAt)
                        .last("LIMIT 1")
        );
        if (smartLock == null
                || smartLock.getLockId() == null
                || !StringUtils.hasText(smartLock.getLockData())) {
            throw BusinessException.conflict("房源未绑定可用的智能门锁");
        }
        return smartLock;
    }

    private String requireReceiver(User user) {
        if (!StringUtils.hasText(user.getPhone())) {
            throw BusinessException.badRequest("用户手机号为空，无法接收蓝牙钥匙");
        }
        return user.getPhone().trim();
    }

    private AppointmentAccessGrant findGrant(String appointmentId) {
        return grantMapper.selectOne(
                Wrappers.<AppointmentAccessGrant>lambdaQuery()
                        .eq(AppointmentAccessGrant::getAppointmentId, appointmentId)
                        .last("LIMIT 1")
        );
    }

    private String requireClientId() {
        if (!StringUtils.hasText(properties.getClientId())) {
            throw BusinessException.conflict("TTLock clientId 未配置");
        }
        return properties.getClientId();
    }

    private long toEpochMillis(LocalDateTime value) {
        return value.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    private int retryDelay(int retryCount) {
        return switch (retryCount) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 10;
            default -> 30;
        };
    }

    private String shortId(String id) {
        return id.length() <= 8 ? id : id.substring(id.length() - 8);
    }

    private String cryptoContext(String grantId) {
        return "appointment-access:" + grantId;
    }

    private String safePlatformError(Integer code, String message) {
        String safeCode = code == null ? "UNKNOWN" : code.toString();
        String safeMessage = StringUtils.hasText(message) ? message : "未返回错误信息";
        return truncate("TTLock[" + safeCode + "]: " + safeMessage);
    }

    private String safeExceptionMessage(RuntimeException exception) {
        String message = StringUtils.hasText(exception.getMessage())
                ? exception.getMessage()
                : "TTLock 请求失败";
        return truncate(message.replaceAll(
                "(?i)(accessToken|token|password|lockData|keyboardPwd)=?[^,\\s]*",
                "$1=***"
        ));
    }

    private String truncate(String value) {
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
