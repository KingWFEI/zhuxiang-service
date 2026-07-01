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
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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
    private final RentBillService rentBillService;
    private final LandlordService landlordService;

    public LeaseServiceImpl(
            HouseService houseService,
            CommunityService communityService,
            SmartLockMapper smartLockMapper,
            LockPermissionService lockPermissionService,
            LockPasscodePermissionService lockPasscodePermissionService,
            RentContractMapper rentContractMapper,
            RentBillService rentBillService,
            LandlordService landlordService
    ) {
        this.houseService = houseService;
        this.communityService = communityService;
        this.smartLockMapper = smartLockMapper;
        this.lockPermissionService = lockPermissionService;
        this.lockPasscodePermissionService = lockPasscodePermissionService;
        this.rentContractMapper = rentContractMapper;
        this.rentBillService = rentBillService;
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
    public LeaseDtos.LeaseDetail getLeaseDetail(String leaseId, String currentUserId) {
        Lease lease = getById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        if (!currentUserId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权查看该租约");
        }

        House house = houseService.getById(lease.getHouseId());
        RentContract contract = lease.getContractId() == null
                ? null
                : rentContractMapper.selectById(lease.getContractId());
        Landlord keeper = house == null || house.getLandlordId() == null
                ? null
                : landlordService.getById(house.getLandlordId());

        RentBill pendingBill = rentBillService.getOne(
                Wrappers.<RentBill>lambdaQuery()
                        .eq(RentBill::getLeaseId, lease.getId())
                        .in(RentBill::getStatus, "pending", "overdue")
                        .orderByAsc(RentBill::getDueDate)
                        .last("LIMIT 1"),
                false
        );

        SmartLock smartLock = smartLockMapper.selectLatestByHouseId(lease.getHouseId());
        LockPermission permission = smartLock == null
                ? null
                : lockPermissionService.getOne(
                        Wrappers.<LockPermission>lambdaQuery()
                                .eq(LockPermission::getTenantId, currentUserId)
                                .eq(LockPermission::getLeaseId, lease.getId())
                                .eq(LockPermission::getSmartLockId, smartLock.getId())
                                .last("LIMIT 1"),
                        false
                );

        return new LeaseDtos.LeaseDetail(
                lease.getId(),
                lease.getContractId(),
                lease.getHouseId(),
                buildHouseName(house),
                house == null ? "" : textOrEmpty(house.getAddress()),
                buildHouseSummary(house),
                contract == null ? "" : textOrEmpty(contract.getTenantName()),
                contract == null ? "" : textOrEmpty(contract.getTenantPhone()),
                contract == null ? "" : textOrEmpty(contract.getTenantIdCard()),
                lease.getStartDate(),
                lease.getEndDate(),
                centsToYuan(lease.getMonthlyRent()),
                centsToYuan(lease.getDeposit()),
                house == null ? "" : textOrEmpty(house.getPaymentMethod()),
                pendingBill != null && pendingBill.getDueDate() != null
                        ? pendingBill.getDueDate().getDayOfMonth()
                        : 5,
                lease.getStatus(),
                contract == null ? "unsigned" : contract.getStatus(),
                pendingBill == null ? "paid" : "unpaid",
                permission == null || permission.getStatus() == null
                        ? "inactive"
                        : permission.getStatus().toLowerCase(Locale.ROOT),
                keeper == null ? "" : textOrEmpty(keeper.getName()),
                keeper == null ? "" : textOrEmpty(keeper.getPhone()),
                buildPendingBillTitle(pendingBill),
                pendingBillAmount(pendingBill),
                pendingBill == null ? null : pendingBill.getDueDate()
        );
    }

    private String buildHouseName(House house) {
        if (house == null) {
            return "";
        }
        return textOrEmpty(house.getBuilding())
                + textOrEmpty(house.getUnit())
                + textOrEmpty(house.getRoom());
    }

    private String buildHouseSummary(House house) {
        if (house == null) {
            return "";
        }
        List<String> parts = new java.util.ArrayList<>();
        if (StringUtils.hasText(house.getRoomType())) {
            parts.add(house.getRoomType());
        }
        if (house.getArea() != null) {
            parts.add(house.getArea().stripTrailingZeros().toPlainString() + "㎡");
        }
        if (StringUtils.hasText(house.getOrientation())) {
            parts.add(house.getOrientation());
        }
        return String.join(" · ", parts);
    }

    private String buildPendingBillTitle(RentBill bill) {
        if (bill == null || bill.getDueDate() == null) {
            return null;
        }
        return bill.getDueDate().getMonthValue() + "月租金待支付";
    }

    private BigDecimal centsToYuan(Integer cents) {
        return cents == null ? BigDecimal.ZERO.setScale(2) : BigDecimal.valueOf(cents, 2);
    }

    private BigDecimal pendingBillAmount(RentBill bill) {
        if (bill == null) {
            return null;
        }
        int amountDue = bill.getAmountDue() == null ? 0 : bill.getAmountDue();
        int amountPaid = bill.getAmountPaid() == null ? 0 : bill.getAmountPaid();
        return centsToYuan(Math.max(amountDue - amountPaid, 0));
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value;
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
                smartLock.getLockData(),
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




