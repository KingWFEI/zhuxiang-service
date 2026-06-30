package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.LeaseLockPasscodeResponse;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.service.*;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/**
* @author king-wang
* @description 针对表【lease(租约表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:39
*/
@Service
public class LeaseServiceImpl extends ServiceImpl<LeaseMapper, Lease>
    implements LeaseService{

    private final HouseService houseService;
    private final CommunityService communityService;
    private final SmartLockMapper smartLockMapper;
    private final LockPermissionService lockPermissionService;
    private final LockPasscodePermissionService lockPasscodePermissionService;
    private final RentContractMapper rentContractMapper;
    private final LandlordService landlordService;

    public LeaseServiceImpl(
            HouseService houseService,
            CommunityService communityService,
            SmartLockMapper smartLockMapper,
            LockPermissionService lockPermissionService,
            LockPasscodePermissionService lockPasscodePermissionService,
            RentContractMapper rentContractMapper,
            LandlordService landlordService
    ) {
        this.houseService = houseService;
        this.communityService = communityService;
        this.smartLockMapper = smartLockMapper;
        this.lockPermissionService = lockPermissionService;
        this.lockPasscodePermissionService = lockPasscodePermissionService;
        this.rentContractMapper = rentContractMapper;
        this.landlordService = landlordService;
    }

    /**
     * 查询用户当前租约及关联住房信息。
     */
    @Override
    public ProfileDtos.CurrentHome getCurrentHome(String userId) {
        Lease lease = getOne(
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
        House house = houseService.getById(lease.getHouseId());
        if (house == null) {
            return null;
        }
        Community community = communityService.getById(house.getCommunityId());
        SmartLock lock = smartLockMapper.selectLatestByHouseId(house.getId());
        return new ProfileDtos.CurrentHome(
                house.getId(),
                community == null ? "" : community.getName(),
                house.getBuilding(),
                house.getUnit(),
                house.getRoom(),
                lease.getId(),
                lease.getStatus(),
                lock == null ? null : lock.getId(),
                lock == null ? "unknown" : lock.getStatus()
        );
    }

    @Override
    public ProfileDtos.LockInfo getLockInfo(String userId) {
        Lease lease = getOne(
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
        LockPermission permission = lockPermissionService.getOne(
                Wrappers.<LockPermission>lambdaQuery()
                        .eq(LockPermission::getTenantId, userId)
                        .eq(LockPermission::getLeaseId, lease.getId())
                        .eq(LockPermission::getSmartLockId, lock.getId())
                        .last("LIMIT 1"),
                false
        );
        return new ProfileDtos.LockInfo(
                lock.getId(),
                lock.getLockName(),
                "TTLock",
                lock.getStatus(),
                lock.getBattery(),
                lease.getId(),
                lease.getStatus(),
                lease.getStartDate() == null ? null : lease.getStartDate().toString(),
                lease.getEndDate() == null ? null : lease.getEndDate().toString(),
                permission == null ? null : permission.getStatus(),
                permission == null || permission.getStartTime() == null ? null : permission.getStartTime().toString(),
                permission == null || permission.getEndTime() == null ? null : permission.getEndTime().toString()
        );
    }

    @Override
    public LeaseDtos.LeaseListResponse getUserLeases(String userId) {
        List<Lease> leases = list(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getUserId, userId)
                        .orderByDesc(Lease::getCreatedAt)
        );
        List<LeaseDtos.LeaseItem> currentLeases = leases.stream()
                .filter(l -> "active".equals(l.getStatus()) || "pending".equals(l.getStatus()))
                .map(this::toLeaseItem)
                .toList();
        List<LeaseDtos.LeaseItem> historyLeases = leases.stream()
                .filter(l -> !"active".equals(l.getStatus()) && !"pending".equals(l.getStatus()))
                .map(this::toLeaseItem)
                .toList();
        return new LeaseDtos.LeaseListResponse(currentLeases, historyLeases);
    }

    @Override
    public LeaseDtos.UnlockDataResponse getUnlockData(String leaseId, String currentUserId) {
        Lease lease = getById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        if (!currentUserId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权查看该租约的门锁权限");
        }
        House house = houseService.getById(lease.getHouseId());
        if (house == null) {
            throw BusinessException.notFound("租约关联房间不存在");
        }
        SmartLock smartLock = smartLockMapper.selectLatestByHouseId(house.getId());
        if (smartLock == null) {
            throw BusinessException.notFound("租约关联门锁不存在");
        }
        LockPermission permission = lockPermissionService.getOne(
                Wrappers.<LockPermission>lambdaQuery()
                        .eq(LockPermission::getLeaseId, leaseId)
                        .eq(LockPermission::getTenantId, currentUserId)
                        .eq(LockPermission::getSmartLockId, smartLock.getId())
                        .eq(LockPermission::getPermissionType, "EKEY")
                        .last("LIMIT 1"),
                false
        );
        LockPasscodePermission passcodePermission = lockPasscodePermissionService.getOne(
                Wrappers.<LockPasscodePermission>lambdaQuery()
                        .eq(LockPasscodePermission::getLeaseId, leaseId)
                        .eq(LockPasscodePermission::getTenantId, currentUserId)
                        .eq(LockPasscodePermission::getSmartLockId, smartLock.getId())
                        .last("LIMIT 1"),
                false
        );
        Instant now = Instant.now();
        LocalDateTime businessNow = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        boolean leaseEffective = "active".equalsIgnoreCase(lease.getStatus())
                || "effective".equalsIgnoreCase(lease.getStatus());
        boolean bluetoothAvailable = leaseEffective
                && permission != null
                && "ACTIVE".equalsIgnoreCase(permission.getStatus())
                && permission.getStartTime() != null
                && permission.getEndTime() != null
                && !businessNow.isBefore(permission.getStartTime())
                && !businessNow.isAfter(permission.getEndTime());
        boolean passcodeAvailable = leaseEffective
                && passcodePermission != null
                && "ACTIVE".equalsIgnoreCase(passcodePermission.getStatus())
                && passcodePermission.getStartTime() != null
                && passcodePermission.getEndTime() != null
                && !now.isBefore(passcodePermission.getStartTime())
                && now.isBefore(passcodePermission.getEndTime());
        String roomName = (house.getBuilding() != null ? house.getBuilding() : "")
                + (house.getUnit() != null ? house.getUnit() : "")
                + (house.getRoom() != null ? house.getRoom() : "");
        return new LeaseDtos.UnlockDataResponse(
                lease.getId(),
                smartLock.getId(),
                roomName,
                house.getTitle(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                permission != null ? permission.getTtlockKeyId() : null,
                permission != null && permission.getStartTime() != null ? permission.getStartTime().toString() : null,
                permission != null && permission.getEndTime() != null ? permission.getEndTime().toString() : null,
                permission != null ? permission.getStatus() : null,
                bluetoothAvailable,
                passcodeAvailable,
                passcodePermission != null ? passcodePermission.getStatus() : null,
                formatPasscodeTime(passcodePermission == null ? null : passcodePermission.getStartTime(), smartLock),
                formatPasscodeTime(passcodePermission == null ? null : passcodePermission.getEndTime(), smartLock)
        );
    }

    /** 校验并返回明文期限密码。 */
    @Override
    public LeaseLockPasscodeResponse getLockPasscode(String leaseId, String currentUserId) {
        return lockPasscodePermissionService.getTenantPasscode(leaseId, currentUserId);
    }

    /** 重试生成期限密码，成功后按既有安全校验解密返回。 */
    @Override
    public LeaseLockPasscodeResponse retryLockPasscode(String leaseId, String currentUserId) {
        LockPasscodePermission permission = lockPasscodePermissionService
                .retryTenantPeriodPasscodeForLease(leaseId, currentUserId);
        if (!"ACTIVE".equalsIgnoreCase(permission.getStatus())) {
            throw BusinessException.badRequest("开锁密码生成失败，请稍后重试或联系管理员");
        }
        return lockPasscodePermissionService.getTenantPasscode(leaseId, currentUserId);
    }

    /** 按门锁时区格式化期限密码时刻。 */
    private String formatPasscodeTime(Instant instant, SmartLock smartLock) {
        if (instant == null) {
            return null;
        }
        Long rawOffset = smartLock.getTimezoneRawOffset();
        if (rawOffset == null || rawOffset % 1000 != 0) {
            return instant.toString();
        }
        try {
            ZoneOffset offset = ZoneOffset.ofTotalSeconds(Math.toIntExact(rawOffset / 1000));
            return instant.atOffset(offset).toString();
        } catch (ArithmeticException | DateTimeException exception) {
            return instant.toString();
        }
    }

    private LeaseDtos.LeaseItem toLeaseItem(Lease lease) {
        House house = houseService.getById(lease.getHouseId());

        String houseName = "";
        String houseAddress = "";
        String houseSummary = "";
        String houseImageUrl = "";
        String paymentMethod = "";
        String keeperName = "";
        String keeperPhone = "";

        if (house != null) {
            Community community = communityService.getById(house.getCommunityId());
            String communityName = community == null ? "" : community.getName();
            houseName = (communityName.isEmpty() ? "" : communityName + " ")
                    + (house.getBuilding() != null ? house.getBuilding() + " " : "")
                    + (house.getUnit() != null ? house.getUnit() + " " : "")
                    + (house.getRoom() != null ? house.getRoom() : "");
            houseAddress = house.getAddress() != null ? house.getAddress() : "";
            houseSummary = (house.getRoomType() != null ? house.getRoomType() + " · " : "")
                    + (house.getArea() != null ? house.getArea().intValue() + "m² · " : "")
                    + (house.getOrientation() != null ? house.getOrientation() : "");
            houseImageUrl = house.getCoverImage() != null ? house.getCoverImage() : "";
            paymentMethod = house.getPaymentMethod() != null ? house.getPaymentMethod() : "";

            Landlord landlord = landlordService.getById(house.getLandlordId());
            if (landlord != null) {
                keeperName = landlord.getName() != null ? landlord.getName() : "";
                keeperPhone = landlord.getPhone() != null ? landlord.getPhone() : "";
            }
        }

        String contractStatus = "unsigned";
        if (lease.getContractId() != null) {
            RentContract contract = rentContractMapper.selectById(lease.getContractId());
            if (contract != null && contract.getStatus() != null) {
                contractStatus = contract.getStatus();
            }
        }

        SmartLock lock = smartLockMapper.selectLatestByHouseId(lease.getHouseId());
        String lockId = lock != null ? lock.getId() : null;

        String lockPermissionStatus = null;
        if (lock != null) {
            LockPermission permission = lockPermissionService.getOne(
                    Wrappers.<LockPermission>lambdaQuery()
                            .eq(LockPermission::getTenantId, lease.getUserId())
                            .eq(LockPermission::getLeaseId, lease.getId())
                            .eq(LockPermission::getSmartLockId, lock.getId())
                            .last("LIMIT 1"),
                    false
            );
            lockPermissionStatus = permission != null ? permission.getStatus() : "inactive";
        }

        return new LeaseDtos.LeaseItem(
                lease.getId(),
                lease.getContractId(),
                house != null ? house.getId() : null,
                houseName,
                houseAddress,
                houseSummary,
                houseImageUrl,
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getMonthlyRent(),
                lease.getDeposit(),
                paymentMethod,
                5,
                lease.getStatus(),
                contractStatus,
                "unpaid",
                lockPermissionStatus,
                lockId,
                keeperName,
                keeperPhone
        );
    }
}




