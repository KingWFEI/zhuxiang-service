package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.client.TtLockOpenApiClient;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.TtLockProperties;
import com.zhuxiang.service.dto.BindRoomRequest;
import com.zhuxiang.service.dto.BleStatusRequest;
import com.zhuxiang.service.dto.InitializeLockResponse;
import com.zhuxiang.service.dto.LocalInitializedLockRequest;
import com.zhuxiang.service.dto.LocalInitializedLockResponse;
import com.zhuxiang.service.dto.SmartLockByMacResponse;
import com.zhuxiang.service.dto.SmartLockDetailResponse;
import com.zhuxiang.service.dto.SmartLockUnlockDataResponse;
import com.zhuxiang.service.dto.TtLockInitializeResponse;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.service.AdminLockService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.TtLockTokenService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端门锁服务实现。
 */
@Service
public class AdminLockServiceImpl extends ServiceImpl<SmartLockMapper, SmartLock>
        implements AdminLockService {

    private static final String STATUS_INIT_LOCAL_SUCCESS = "INIT_LOCAL_SUCCESS";
    private static final String STATUS_ROOM_BOUND = "ROOM_BOUND";
    private static final String STATUS_PLATFORM_BINDING = "PLATFORM_BINDING";
    private static final String STATUS_PLATFORM_BOUND = "PLATFORM_BOUND";
    private static final String STATUS_PLATFORM_FAILED = "PLATFORM_FAILED";
    private static final String STATUS_BOUND = "BOUND";
    private static final String BATTERY_SOURCE_BLE_LAST_SYNC = "BLE_LAST_SYNC";
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");
    private static final Set<String> TARGET_BOUND_STATUSES = Set.of(
            STATUS_ROOM_BOUND,
            STATUS_PLATFORM_BINDING,
            STATUS_PLATFORM_BOUND,
            STATUS_BOUND
    );
    private static final Set<String> BINDABLE_STATUSES = Set.of(
            STATUS_INIT_LOCAL_SUCCESS,
            STATUS_PLATFORM_FAILED
    );

    private final HouseService houseService;
    private final UserService userService;
    private final TtLockTokenService tokenService;
    private final TtLockOpenApiClient openApiClient;
    private final TtLockProperties properties;

    public AdminLockServiceImpl(
            HouseService houseService,
            UserService userService,
            TtLockTokenService tokenService,
            TtLockOpenApiClient openApiClient,
            TtLockProperties properties
    ) {
        this.houseService = houseService;
        this.userService = userService;
        this.tokenService = tokenService;
        this.openApiClient = openApiClient;
        this.properties = properties;
    }

    /**
     * 保存App端SDK初始化成功后的lockData，暂不绑定房源。
     */
    @Override
    @Transactional
    public LocalInitializedLockResponse saveLocalInitializedLock(
            LocalInitializedLockRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        validateLocalRequest(request);

        SmartLock existingLock = findByLockMac(request.lockMac().trim());
        if (existingLock != null) {
            return handleExistingLocalRecord(existingLock, request, operatorId);
        }

        SmartLock smartLock = saveLocalSmartLock(request, operatorId);
        return toLocalResponse(smartLock);
    }

    /**
     * 将已录入的门锁绑定到指定房源或房间。
     */
    @Override
    @Transactional
    public InitializeLockResponse bindRoom(
            String smartLockId,
            BindRoomRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        if (!StringUtils.hasText(smartLockId)) {
            throw BusinessException.badRequest("smartLockId 不能为空");
        }
        if (request == null || !StringUtils.hasText(request.houseId())) {
            throw BusinessException.badRequest("houseId 不能为空");
        }

        SmartLock smartLock = requireSmartLock(smartLockId);
        if (smartLock.getHouseId() != null) {
            return handleExistingRoomBinding(smartLock, request);
        }
        if (!BINDABLE_STATUSES.contains(smartLock.getStatus())) {
            throw BusinessException.conflict("当前门锁状态不允许绑定房间");
        }

        House house = requireHouseForBinding(request.houseId().trim());
        String roomId = trimToNull(request.roomId());
        ensureTargetNotBound(house.getId(), roomId, smartLock.getId());

        smartLock.setHouseId(house.getId());
        smartLock.setRoomId(roomId);
        smartLock.setLockName(buildLockAlias(house, smartLock.getLockName()));
        smartLock.setStatus(STATUS_ROOM_BOUND);
        smartLock.setUpdatedAt(LocalDateTime.now());
        updateById(smartLock);
        updateHouseLockStatus(house, smartLock.getId(), STATUS_ROOM_BOUND);
        return toResponse(smartLock);
    }

    /**
     * 从数据库读取lockData并同步通通锁开放平台，失败后保留错误信息供重试。
     */
    @Override
    public InitializeLockResponse syncPlatform(String smartLockId, String operatorId) {
        requireAdminRole(operatorId);
        if (!StringUtils.hasText(smartLockId)) {
            throw BusinessException.badRequest("smartLockId 不能为空");
        }
        SmartLock smartLock = requireSmartLock(smartLockId);
        if (STATUS_BOUND.equals(smartLock.getStatus())) {
            clearPlatformErrorForBoundLock(smartLock);
            return toResponse(smartLock);
        }
        if (!StringUtils.hasText(smartLock.getLockData())) {
            throw BusinessException.badRequest("门锁初始化数据不存在，无法同步开放平台");
        }
        if (!StringUtils.hasText(smartLock.getHouseId())) {
            throw BusinessException.badRequest("请先绑定房间后再同步开放平台");
        }
        if (smartLock.getLockId() != null && smartLock.getKeyId() != null) {
            markPlatformBoundAndBindHouse(smartLock, smartLock.getLockId(), smartLock.getKeyId());
            return toResponse(smartLock);
        }

        markPlatformBinding(smartLock);
        try {
            TtLockInitializeResponse platformResponse = openApiClient.initializeLock(
                    requireClientId(),
                    tokenService.getAccessToken(),
                    smartLock.getLockData(),
                    smartLock.getLockName()
            );
            if (!platformResponse.success()) {
                markPlatformFailed(smartLock, platformResponse);
                throw BusinessException.badRequest(safePlatformMessage(platformResponse));
            }
            if (platformResponse.getLockId() == null || platformResponse.getKeyId() == null) {
                markPlatformFailed(smartLock, "MISSING_LOCK_ID_OR_KEY_ID", "通通锁开放平台未返回 lockId 或 keyId");
                throw BusinessException.badRequest("通通锁开放平台未返回 lockId 或 keyId");
            }
            markPlatformBoundAndBindHouse(smartLock, platformResponse.getLockId(), platformResponse.getKeyId());
            return toResponse(smartLock);
        } catch (BusinessException exception) {
            if (!STATUS_PLATFORM_FAILED.equals(smartLock.getStatus())) {
                markPlatformFailed(smartLock, "REQUEST_FAILED", exception.getMessage());
            }
            throw exception;
        }
    }

    /**
     * 删除门锁和房源之间的绑定关系，保留门锁初始化数据。
     */
    @Override
    @Transactional
    public InitializeLockResponse deleteRoomBinding(String smartLockId, String operatorId) {
        requireAdminRole(operatorId);
        if (!StringUtils.hasText(smartLockId)) {
            throw BusinessException.badRequest("smartLockId 不能为空");
        }
        SmartLock smartLock = requireSmartLock(smartLockId);
        if (STATUS_BOUND.equals(smartLock.getStatus())
                || STATUS_PLATFORM_BOUND.equals(smartLock.getStatus())) {
            throw BusinessException.conflict("门锁已同步开放平台，不能直接删除绑定记录");
        }
        clearHouseLockRelation(smartLock);
        smartLock.setHouseId(null);
        smartLock.setRoomId(null);
        smartLock.setStatus(STATUS_INIT_LOCAL_SUCCESS);
        smartLock.setPlatformErrorCode(null);
        smartLock.setPlatformErrorMessage(null);
        smartLock.setLastSyncTime(null);
        smartLock.setUpdatedAt(LocalDateTime.now());
        lambdaUpdate()
                .eq(SmartLock::getId, smartLock.getId())
                .set(SmartLock::getHouseId, null)
                .set(SmartLock::getRoomId, null)
                .set(SmartLock::getStatus, smartLock.getStatus())
                .set(SmartLock::getPlatformErrorCode, null)
                .set(SmartLock::getPlatformErrorMessage, null)
                .set(SmartLock::getLastSyncTime, null)
                .set(SmartLock::getUpdatedAt, smartLock.getUpdatedAt())
                .update();
        return toResponse(smartLock);
    }

    /**
     * 根据MAC查询门锁本地记录。
     */
    @Override
    public SmartLockByMacResponse getByLockMac(String lockMac, String operatorId) {
        requireAdminRole(operatorId);
        if (!StringUtils.hasText(lockMac)) {
            throw BusinessException.badRequest("lockMac 不能为空");
        }
        SmartLock smartLock = findByLockMac(lockMac.trim());
        if (smartLock == null) {
            throw BusinessException.notFound("当前系统未找到该门锁记录");
        }
        House house = StringUtils.hasText(smartLock.getHouseId())
                ? houseService.getById(smartLock.getHouseId())
                : null;
        return new SmartLockByMacResponse(
                smartLock.getId(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                smartLock.getStatus(),
                smartLock.getHouseId(),
                smartLock.getRoomId(),
                house == null ? null : house.getTitle(),
                buildRoomName(house, smartLock.getRoomId())
        );
    }

    /**
     * 查询门锁管理详情。
     */
    @Override
    public SmartLockDetailResponse getDetail(String smartLockId, String operatorId) {
        requireAdminRole(operatorId);
        if (!StringUtils.hasText(smartLockId)) {
            throw BusinessException.badRequest("smartLockId 不能为空");
        }
        SmartLock smartLock = requireSmartLock(smartLockId);
        House house = StringUtils.hasText(smartLock.getHouseId())
                ? houseService.getById(smartLock.getHouseId())
                : null;
        return new SmartLockDetailResponse(
                smartLock.getId(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                smartLock.getStatus(),
                smartLock.getLockId(),
                smartLock.getKeyId(),
                smartLock.getHouseId(),
                smartLock.getRoomId(),
                house == null ? null : house.getTitle(),
                buildRoomName(house, smartLock.getRoomId()),
                smartLock.getBattery(),
                smartLock.getRssi(),
                smartLock.getBatterySource(),
                smartLock.getLastBleSyncTime(),
                smartLock.getLastSyncTime(),
                smartLock.getPlatformErrorMessage()
        );
    }

    /**
     * Update lock battery and signal from a nearby BLE scan.
     */
    @Override
    @Transactional
    public SmartLockDetailResponse updateBleStatus(
            String smartLockId,
            BleStatusRequest request,
            String operatorId
    ) {
        requireAdminRole(operatorId);
        if (!StringUtils.hasText(smartLockId)) {
            throw BusinessException.badRequest("smartLockId 不能为空");
        }
        if (request == null) {
            throw BusinessException.badRequest("请求体不能为空");
        }

        SmartLock smartLock = requireSmartLock(smartLockId);
        LocalDateTime now = LocalDateTime.now();
        smartLock.setBattery(request.battery());
        smartLock.setRssi(request.rssi());
        smartLock.setBatterySource(BATTERY_SOURCE_BLE_LAST_SYNC);
        smartLock.setLastBleSyncTime(now);
        smartLock.setUpdatedAt(now);
        lambdaUpdate()
                .eq(SmartLock::getId, smartLock.getId())
                .set(SmartLock::getBattery, request.battery())
                .set(SmartLock::getRssi, request.rssi())
                .set(SmartLock::getBatterySource, BATTERY_SOURCE_BLE_LAST_SYNC)
                .set(SmartLock::getLastBleSyncTime, now)
                .set(SmartLock::getUpdatedAt, now)
                .update();

        House house = StringUtils.hasText(smartLock.getHouseId())
                ? houseService.getById(smartLock.getHouseId())
                : null;
        return new SmartLockDetailResponse(
                smartLock.getId(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                smartLock.getStatus(),
                smartLock.getLockId(),
                smartLock.getKeyId(),
                smartLock.getHouseId(),
                smartLock.getRoomId(),
                house == null ? null : house.getTitle(),
                buildRoomName(house, smartLock.getRoomId()),
                smartLock.getBattery(),
                smartLock.getRssi(),
                smartLock.getBatterySource(),
                smartLock.getLastBleSyncTime(),
                smartLock.getLastSyncTime(),
                smartLock.getPlatformErrorMessage()
        );
    }

    /**
     * 查询蓝牙开锁所需数据。
     */
    @Override
    public SmartLockUnlockDataResponse getUnlockData(String smartLockId, String operatorId) {
        requireAdminRole(operatorId);
        if (!StringUtils.hasText(smartLockId)) {
            throw BusinessException.badRequest("smartLockId 不能为空");
        }

        SmartLock smartLock = requireSmartLock(smartLockId);
        if (!StringUtils.hasText(smartLock.getLockData())) {
            throw BusinessException.badRequest("门锁蓝牙开锁数据不存在");
        }
        House house = StringUtils.hasText(smartLock.getHouseId())
                ? houseService.getById(smartLock.getHouseId())
                : null;
        return new SmartLockUnlockDataResponse(
                smartLock.getId(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                smartLock.getLockData(),
                buildRoomName(house, smartLock.getRoomId()),
                smartLock.getStatus()
        );
    }

    /**
     * 校验本地初始化数据保存请求。
     */
    private void validateLocalRequest(LocalInitializedLockRequest request) {
        if (request == null) {
            throw BusinessException.badRequest("请求体不能为空");
        }
        if (!StringUtils.hasText(request.lockData())) {
            throw BusinessException.badRequest("lockData 不能为空");
        }
        if (!StringUtils.hasText(request.lockMac())) {
            throw BusinessException.badRequest("lockMac 不能为空");
        }
    }

    /**
     * 校验操作者是否具备管理端权限。
     */
    private void requireAdminRole(String operatorId) {
        User user = userService.requireActiveUser(operatorId);
        if (!ADMIN_ROLES.contains(user.getRole())) {
            throw BusinessException.forbidden("当前账号无权初始化门锁");
        }
    }

    /**
     * 查询并校验门锁记录存在。
     */
    private SmartLock requireSmartLock(String smartLockId) {
        SmartLock smartLock = getById(smartLockId);
        if (smartLock == null) {
            throw BusinessException.notFound("门锁初始化记录不存在");
        }
        return smartLock;
    }

    /**
     * 查询并校验房源可绑定门锁。
     */
    private House requireHouseForBinding(String houseId) {
        House house = houseService.getById(houseId);
        if (house == null) {
            throw BusinessException.notFound("房源不存在");
        }
        if (StringUtils.hasText(house.getSmartLockId())
                || (StringUtils.hasText(house.getLockBindStatus())
                && !"UNBOUND".equals(house.getLockBindStatus()))) {
            throw BusinessException.conflict("房源已经绑定门锁");
        }
        return house;
    }

    /**
     * 校验目标房源或房间是否已经存在有效门锁记录。
     */
    private void ensureTargetNotBound(String houseId, String roomId, String excludeSmartLockId) {
        Long count = count(Wrappers.<SmartLock>lambdaQuery()
                .ne(StringUtils.hasText(excludeSmartLockId), SmartLock::getId, excludeSmartLockId)
                .eq(SmartLock::getHouseId, houseId)
                .eq(StringUtils.hasText(roomId), SmartLock::getRoomId, roomId)
                .isNull(!StringUtils.hasText(roomId), SmartLock::getRoomId)
                .in(SmartLock::getStatus, TARGET_BOUND_STATUSES));
        if (count != null && count > 0) {
            throw BusinessException.conflict(StringUtils.hasText(roomId)
                    ? "房间已经绑定其他门锁"
                    : "房源已经绑定其他门锁");
        }
    }

    /**
     * 根据MAC查询门锁记录。
     */
    private SmartLock findByLockMac(String lockMac) {
        return getOne(
                Wrappers.<SmartLock>lambdaQuery()
                        .eq(SmartLock::getLockMac, lockMac)
                        .last("LIMIT 1"),
                false
        );
    }

    /**
     * 处理App重试保存本地初始化结果的幂等返回。
     */
    private LocalInitializedLockResponse handleExistingLocalRecord(
            SmartLock existingLock,
            LocalInitializedLockRequest request,
            String operatorId
    ) {
        boolean lockDataChanged = !request.lockData().equals(existingLock.getLockData());
        if (lockDataChanged) {
            deleteExistingLockData(existingLock);
            SmartLock newLock = saveLocalSmartLock(request, operatorId);
            return toLocalResponse(newLock);
        }
        if (StringUtils.hasText(request.lockName())) {
            existingLock.setLockName(request.lockName().trim());
        }
        existingLock.setRssi(request.rssi());
        existingLock.setBattery(request.battery());
        existingLock.setUpdatedAt(LocalDateTime.now());
        updateById(existingLock);
        return toLocalResponse(existingLock);
    }

    /**
     * 删除同一物理锁旧初始化数据及房源绑定关系。
     */
    private void deleteExistingLockData(SmartLock existingLock) {
        clearHouseLockRelation(existingLock);
        remove(Wrappers.<SmartLock>lambdaQuery()
                .eq(SmartLock::getLockMac, existingLock.getLockMac()));
    }

    /**
     * 清理旧门锁关联到房源上的绑定字段。
     */
    private void clearHouseLockRelation(SmartLock existingLock) {
        if (!StringUtils.hasText(existingLock.getId())
                && !StringUtils.hasText(existingLock.getHouseId())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        House house = null;
        if (StringUtils.hasText(existingLock.getHouseId())) {
            house = houseService.getById(existingLock.getHouseId());
        }
        if (house == null && StringUtils.hasText(existingLock.getId())) {
            house = houseService.getOne(
                    Wrappers.<House>lambdaQuery()
                            .eq(House::getSmartLockId, existingLock.getId())
                            .last("LIMIT 1"),
                    false
            );
        }
        if (house == null) {
            return;
        }
        house.setSmartLockId(null);
        house.setLockBindStatus("UNBOUND");
        house.setUpdatedAt(now);
        houseService.lambdaUpdate()
                .eq(House::getId, house.getId())
                .set(House::getSmartLockId, null)
                .set(House::getLockBindStatus, "UNBOUND")
                .set(House::getUpdatedAt, now)
                .update();
    }

    /**
     * 处理绑定房间接口的幂等返回。
     */
    private InitializeLockResponse handleExistingRoomBinding(
            SmartLock smartLock,
            BindRoomRequest request
    ) {
        String roomId = trimToNull(request.roomId());
        boolean sameTarget = smartLock.getHouseId().equals(request.houseId().trim())
                && ((smartLock.getRoomId() == null && roomId == null)
                || (smartLock.getRoomId() != null && smartLock.getRoomId().equals(roomId)));
        if (!sameTarget) {
            throw BusinessException.conflict("门锁已经绑定到其他房源或房间");
        }
        return toResponse(smartLock);
    }

    /**
     * 获取已配置的通通锁clientId。
     */
    private String requireClientId() {
        if (!StringUtils.hasText(properties.getClientId())) {
            throw BusinessException.badRequest("通通锁 clientId 未配置");
        }
        return properties.getClientId();
    }

    /**
     * 生成通通锁平台门锁别名。
     */
    private String buildLockAlias(House house, String currentLockName) {
        StringBuilder alias = new StringBuilder();
        appendAliasPart(alias, house.getTitle());
        appendAliasPart(alias, house.getBuilding());
        appendAliasPart(alias, house.getUnit());
        appendAliasPart(alias, house.getRoom());
        if (alias.isEmpty() && StringUtils.hasText(currentLockName)) {
            return currentLockName.trim();
        }
        return alias.append("-门锁").toString();
    }

    /**
     * 构建房间展示名称。
     */
    private String buildRoomName(House house, String roomId) {
        if (house == null) {
            return null;
        }
        StringBuilder name = new StringBuilder();
        appendAliasPart(name, house.getBuilding());
        appendAliasPart(name, house.getUnit());
        appendAliasPart(name, StringUtils.hasText(roomId) ? roomId : house.getRoom());
        return name.isEmpty() ? null : name.toString();
    }

    /**
     * 追加门锁别名片段。
     */
    private void appendAliasPart(StringBuilder alias, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!alias.isEmpty()) {
            alias.append("-");
        }
        alias.append(value.trim());
    }

    /**
     * 保存本地智能门锁初始化记录。
     */
    private SmartLock saveLocalSmartLock(
            LocalInitializedLockRequest request,
            String operatorId
    ) {
        LocalDateTime now = LocalDateTime.now();
        SmartLock smartLock = new SmartLock();
        smartLock.setId(UUID.randomUUID().toString());
        smartLock.setHouseId(null);
        smartLock.setRoomId(null);
        smartLock.setLockId(null);
        smartLock.setKeyId(null);
        smartLock.setLockName(StringUtils.hasText(request.lockName())
                ? request.lockName().trim()
                : request.lockMac().trim());
        smartLock.setLockMac(request.lockMac().trim());
        smartLock.setLockData(request.lockData());
        smartLock.setStatus(STATUS_INIT_LOCAL_SUCCESS);
        smartLock.setPlatformErrorCode(null);
        smartLock.setPlatformErrorMessage(null);
        smartLock.setLastSyncTime(null);
        smartLock.setBattery(request.battery());
        smartLock.setRssi(request.rssi());
        smartLock.setBindTime(now);
        smartLock.setCreatedBy(operatorId);
        smartLock.setCreatedAt(now);
        smartLock.setUpdatedAt(now);
        if (!save(smartLock)) {
            throw BusinessException.badRequest("门锁本地初始化数据保存失败");
        }
        return smartLock;
    }

    /**
     * 标记门锁正在同步开放平台。
     */
    private void markPlatformBinding(SmartLock smartLock) {
        LocalDateTime now = LocalDateTime.now();
        smartLock.setStatus(STATUS_PLATFORM_BINDING);
        smartLock.setPlatformErrorCode(null);
        smartLock.setPlatformErrorMessage(null);
        smartLock.setLastSyncTime(now);
        smartLock.setUpdatedAt(now);
        lambdaUpdate()
                .eq(SmartLock::getId, smartLock.getId())
                .set(SmartLock::getStatus, STATUS_PLATFORM_BINDING)
                .set(SmartLock::getPlatformErrorCode, null)
                .set(SmartLock::getPlatformErrorMessage, null)
                .set(SmartLock::getLastSyncTime, now)
                .set(SmartLock::getUpdatedAt, now)
                .update();
    }

    /**
     * 标记开放平台同步失败。
     */
    private void markPlatformFailed(SmartLock smartLock, TtLockInitializeResponse response) {
        markPlatformFailed(
                smartLock,
                response.getErrcode() == null ? "PLATFORM_ERROR" : response.getErrcode().toString(),
                response.getErrmsg()
        );
    }

    /**
     * 标记开放平台同步失败并保存安全错误信息。
     */
    private void markPlatformFailed(SmartLock smartLock, String errorCode, String errorMessage) {
        smartLock.setStatus(STATUS_PLATFORM_FAILED);
        smartLock.setPlatformErrorCode(errorCode);
        smartLock.setPlatformErrorMessage(sanitizeErrorMessage(errorMessage));
        smartLock.setLastSyncTime(LocalDateTime.now());
        smartLock.setUpdatedAt(LocalDateTime.now());
        updateById(smartLock);
    }

    /**
     * 修复已绑定门锁上残留的平台错误字段。
     */
    private void clearPlatformErrorForBoundLock(SmartLock smartLock) {
        if (!StringUtils.hasText(smartLock.getPlatformErrorCode())
                && !StringUtils.hasText(smartLock.getPlatformErrorMessage())) {
            return;
        }
        smartLock.setPlatformErrorCode(null);
        smartLock.setPlatformErrorMessage(null);
        smartLock.setUpdatedAt(LocalDateTime.now());
        lambdaUpdate()
                .eq(SmartLock::getId, smartLock.getId())
                .set(SmartLock::getPlatformErrorCode, null)
                .set(SmartLock::getPlatformErrorMessage, null)
                .set(SmartLock::getUpdatedAt, smartLock.getUpdatedAt())
                .update();
    }

    /**
     * 保存开放平台返回信息并完成房源绑定。
     */
    private void markPlatformBoundAndBindHouse(SmartLock smartLock, Long lockId, Long keyId) {
        LocalDateTime now = LocalDateTime.now();
        smartLock.setLockId(lockId);
        smartLock.setKeyId(keyId);
        smartLock.setStatus(STATUS_BOUND);
        smartLock.setPlatformErrorCode(null);
        smartLock.setPlatformErrorMessage(null);
        smartLock.setLastSyncTime(now);
        smartLock.setUpdatedAt(now);
        lambdaUpdate()
                .eq(SmartLock::getId, smartLock.getId())
                .set(SmartLock::getLockId, lockId)
                .set(SmartLock::getKeyId, keyId)
                .set(SmartLock::getStatus, STATUS_BOUND)
                .set(SmartLock::getPlatformErrorCode, null)
                .set(SmartLock::getPlatformErrorMessage, null)
                .set(SmartLock::getLastSyncTime, now)
                .set(SmartLock::getUpdatedAt, now)
                .update();

        House house = houseService.getById(smartLock.getHouseId());
        if (house == null) {
            markPlatformFailed(smartLock, "HOUSE_NOT_FOUND", "房源不存在，无法完成绑定");
            throw BusinessException.notFound("房源不存在，无法完成绑定");
        }
        updateHouseLockStatus(house, smartLock.getId(), STATUS_BOUND);
    }

    /**
     * 更新房源的门锁绑定状态。
     */
    private void updateHouseLockStatus(House house, String smartLockId, String bindStatus) {
        house.setSmartLockId(smartLockId);
        house.setLockBindStatus(bindStatus);
        house.setIsSmartLockSupported(1);
        house.setUpdatedAt(LocalDateTime.now());
        if (!houseService.updateById(house)) {
            throw BusinessException.badRequest("房源门锁绑定状态更新失败");
        }
    }

    /**
     * 转换为本地初始化保存响应。
     */
    private LocalInitializedLockResponse toLocalResponse(SmartLock smartLock) {
        return new LocalInitializedLockResponse(
                smartLock.getId(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                smartLock.getStatus()
        );
    }

    /**
     * 转换为初始化绑定响应。
     */
    private InitializeLockResponse toResponse(SmartLock smartLock) {
        return new InitializeLockResponse(
                smartLock.getId(),
                smartLock.getHouseId(),
                smartLock.getRoomId(),
                smartLock.getLockId(),
                smartLock.getKeyId(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                smartLock.getStatus(),
                smartLock.getPlatformErrorCode(),
                smartLock.getPlatformErrorMessage()
        );
    }

    /**
     * 生成不包含敏感信息的平台错误描述。
     */
    private String safePlatformMessage(TtLockInitializeResponse response) {
        String message = StringUtils.hasText(response.getErrmsg())
                ? response.getErrmsg()
                : "通通锁开放平台返回失败";
        if (message.toLowerCase().contains("token")) {
            return "通通锁 accessToken 无效或已过期";
        }
        return "通通锁开放平台返回失败：" + message;
    }

    /**
     * 清理开放平台错误信息中的敏感文本。
     */
    private String sanitizeErrorMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "通通锁开放平台同步失败";
        }
        String result = message.trim();
        if (result.length() > 500) {
            result = result.substring(0, 500);
        }
        return result;
    }

    /**
     * 去除空白字符串。
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
