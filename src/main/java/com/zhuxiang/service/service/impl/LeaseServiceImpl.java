package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.config.AutoUnlockProperties;
import com.zhuxiang.service.dto.LeaseLockPasscodeResponse;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.event.LeaseActivatedEvent;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
* @author king-wang
* @description 针对表【lease(租约表)】的数据库操作Service实现
* @createDate 2026-06-12 19:57:39
*/
@Service
public class LeaseServiceImpl extends ServiceImpl<LeaseMapper, Lease>
    implements LeaseService{

    private static final Logger log = LoggerFactory.getLogger(LeaseServiceImpl.class);

    private final HouseService houseService;
    private final CommunityService communityService;
    private final SmartLockMapper smartLockMapper;
    private final LockPermissionService lockPermissionService;
    private final LockPasscodePermissionService lockPasscodePermissionService;
    private final RentContractMapper rentContractMapper;
    private final RentBillService rentBillService;
    private final LandlordService landlordService;
    private final AutoUnlockProperties autoUnlockProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final EsignV3Client esignV3Client;
    private Clock clock = Clock.system(ZoneId.of("Asia/Shanghai"));

    public LeaseServiceImpl(
            HouseService houseService,
            CommunityService communityService,
            SmartLockMapper smartLockMapper,
            LockPermissionService lockPermissionService,
            LockPasscodePermissionService lockPasscodePermissionService,
            RentContractMapper rentContractMapper,
            RentBillService rentBillService,
            LandlordService landlordService,
            AutoUnlockProperties autoUnlockProperties,
            ApplicationEventPublisher eventPublisher,
            EsignV3Client esignV3Client
    ) {
        this.houseService = houseService;
        this.communityService = communityService;
        this.smartLockMapper = smartLockMapper;
        this.lockPermissionService = lockPermissionService;
        this.lockPasscodePermissionService = lockPasscodePermissionService;
        this.rentContractMapper = rentContractMapper;
        this.rentBillService = rentBillService;
        this.landlordService = landlordService;
        this.autoUnlockProperties = autoUnlockProperties;
        this.eventPublisher = eventPublisher;
        this.esignV3Client = esignV3Client;
    }

    /**
     * 查询用户当前租约及关联住房信息。
     */
    @Override
    public List<ProfileDtos.CurrentHome> getCurrentHome(String userId) {
        List<Lease> leases = list(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getUserId, userId)
                        .in(Lease::getStatus, "active", "pending")
                        .orderByDesc(Lease::getCreatedAt)
        );
        if (leases.isEmpty()) {
            return List.of();
        }

        List<ProfileDtos.CurrentHome> homes = new java.util.ArrayList<>();
        for (Lease lease : leases) {
            House house = houseService.getById(lease.getHouseId());
            if (house == null) continue;
            Community community = communityService.getById(house.getCommunityId());
            SmartLock lock = smartLockMapper.selectLatestByHouseId(house.getId());
            homes.add(new ProfileDtos.CurrentHome(
                    house.getId(),
                    textOrEmpty(house.getTitle()),
                    community == null ? "" : community.getName(),
                    textOrEmpty(house.getLocation()),
                    house.getBuilding(),
                    house.getUnit(),
                    house.getRoom(),
                    house.getAddress() != null ? house.getAddress() : "",
                    textOrEmpty(house.getRoomType()),
                    house.getArea() == null ? null : house.getArea().intValue(),
                    textOrEmpty(house.getFloor()),
                    textOrEmpty(house.getOrientation()),
                    lease.getMonthlyRent(),
                    lease.getDeposit(),
                    textOrEmpty(house.getPaymentMethod()),
                    lease.getStartDate(),
                    lease.getEndDate(),
                    lease.getId(),
                    lease.getStatus(),
                    lock == null ? null : lock.getId(),
                    lock == null ? "UNBOUND" : lock.getStatus(),
                    house.getCoverImage() != null ? house.getCoverImage() : "",
                    textOrEmpty(house.getSourceType())
            ));
        }
        return homes;
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
                : landlordService.findByUserId(house.getLandlordId());

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
                house == null ? "" : textOrEmpty(house.getCoverImage()),
                house == null ? "" : textOrEmpty(house.getAddress()),
                buildHouseSummary(house),
                contract == null ? "" : textOrEmpty(contract.getTenantName()),
                contract == null ? "" : textOrEmpty(contract.getTenantPhone()),
                contract == null ? "" : textOrEmpty(contract.getTenantIdCard()),
                lease.getStartDate(),
                lease.getEndDate(),
                lease.getMonthlyRent(),
                lease.getDeposit(),
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
                pendingBill == null ? null
                        : Math.max(
                                (pendingBill.getAmountDue() == null ? 0 : pendingBill.getAmountDue())
                                        - (pendingBill.getAmountPaid() == null ? 0 : pendingBill.getAmountPaid()),
                                0),
                pendingBill == null ? null : pendingBill.getDueDate()
        );
    }

    @Override
    public LeaseDtos.LeaseContractDocument getLeaseContract(String leaseId, String currentUserId) {
        Lease lease = getById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        if (!currentUserId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权查看该租约合同");
        }
        if (!StringUtils.hasText(lease.getContractId())) {
            throw BusinessException.notFound("租约尚未关联合同");
        }

        RentContract contract = rentContractMapper.selectById(lease.getContractId());
        if (contract == null) {
            throw BusinessException.notFound("电子合同不存在");
        }
        if (!"signed".equals(contract.getStatus())) {
            throw BusinessException.badRequest("电子合同尚未完成签署");
        }

        String fileUrl = textOrEmpty(contract.getPreviewUrl());
        if (StringUtils.hasText(contract.getSignFlowId())) {
            try {
                EsignV3Client.FileDownloadResponse response =
                        esignV3Client.getFileDownloadUrl(contract.getSignFlowId());
                if (response.getData() != null
                        && response.getData().getFiles() != null
                        && !response.getData().getFiles().isEmpty()
                        && StringUtils.hasText(
                        response.getData().getFiles().get(0).getDownloadUrl())) {
                    fileUrl = response.getData().getFiles().get(0).getDownloadUrl();
                }
            } catch (RuntimeException ex) {
                log.warn("获取 e签宝已签合同地址失败，使用合同预览地址兜底: leaseId={}, contractId={}",
                        leaseId, contract.getId(), ex);
            }
        }

        String houseName = textOrEmpty(contract.getHouseName());
        if (StringUtils.hasText(contract.getRoomName())) {
            houseName = houseName + contract.getRoomName();
        }
        List<String> clauses = List.of(
                "租赁房屋：" + textOrEmpty(contract.getHouseAddress()),
                "租赁期限：" + contract.getStartDate() + " 至 " + contract.getEndDate(),
                "月租金及押金以双方签署的电子合同文件为准。"
        );
        return new LeaseDtos.LeaseContractDocument(
                contract.getId(),
                textOrEmpty(contract.getContractNo()),
                houseName,
                textOrEmpty(contract.getTenantName()),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getMonthlyRent(),
                contract.getDeposit(),
                textOrEmpty(lease.getPaymentMethod()),
                "已签约",
                "",
                fileUrl,
                clauses,
                contract.getSignedAt()
        );
    }

    private String buildHouseName(House house) {
        if (house == null) {
            return "";
        }
        return textOrEmpty(house.getBuilding() == null ? null : house.getBuilding() + "栋")
                + textOrEmpty(house.getUnit() == null ? null : house.getUnit() + "单元")
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
        boolean leaseEffective = "active".equalsIgnoreCase(lease.getStatus())
                || "effective".equalsIgnoreCase(lease.getStatus());
        if (!leaseEffective) {
            return invalidLeaseUnlockData(lease);
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
        boolean bluetoothAvailable = permission != null
                && "ACTIVE".equalsIgnoreCase(permission.getStatus())
                && permission.getStartTime() != null
                && permission.getEndTime() != null
                && !businessNow.isBefore(permission.getStartTime())
                && !businessNow.isAfter(permission.getEndTime());
        boolean passcodeAvailable = passcodePermission != null
                && "ACTIVE".equalsIgnoreCase(passcodePermission.getStatus())
                && passcodePermission.getStartTime() != null
                && passcodePermission.getEndTime() != null
                && !now.isBefore(passcodePermission.getStartTime())
                && now.isBefore(passcodePermission.getEndTime());
        boolean autoUnlockAvailable = autoUnlockProperties.isEnabled()
                && bluetoothAvailable
                && permission.getTtlockLockId() != null
                && permission.getTtlockLockId().equals(smartLock.getLockId())
                && StringUtils.hasText(smartLock.getLockMac())
                && StringUtils.hasText(smartLock.getLockData())
                && Set.of("BOUND", "PLATFORM_BOUND").contains(smartLock.getStatus());
        return new LeaseDtos.UnlockDataResponse(
                lease.getId(),
                lease.getStatus(),
                true,
                smartLock.getId(),
                house.getBuilding() != null ? house.getBuilding() : "",
                house.getUnit() != null ? house.getUnit() : "",
                house.getRoom() != null ? house.getRoom() : "",
                house.getTitle(),
                smartLock.getLockName(),
                smartLock.getLockMac(),
                smartLock.getLockData(),
                permission != null ? permission.getTtlockKeyId() : null,
                permission != null ? permission.getTtlockLockId() : null,
                permission != null && permission.getStartTime() != null ? permission.getStartTime().toString() : null,
                permission != null && permission.getEndTime() != null ? permission.getEndTime().toString() : null,
                permission != null ? permission.getStatus() : null,
                bluetoothAvailable,
                passcodeAvailable,
                passcodePermission != null ? passcodePermission.getStatus() : null,
                formatPasscodeTime(passcodePermission == null ? null : passcodePermission.getStartTime(), smartLock),
                formatPasscodeTime(passcodePermission == null ? null : passcodePermission.getEndTime(), smartLock),
                autoUnlockAvailable,
                autoUnlockProperties.getMinRssi(),
                autoUnlockProperties.getStableMillis(),
                autoUnlockProperties.getCooldownSeconds()
        );
    }

    /** 失效租约只返回状态标识，禁止继续查询或泄露任何开锁数据。 */
    private LeaseDtos.UnlockDataResponse invalidLeaseUnlockData(Lease lease) {
        return new LeaseDtos.UnlockDataResponse(
                lease.getId(),
                lease.getStatus(),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "LEASE_INVALID",
                false,
                false,
                "LEASE_INVALID",
                null,
                null,
                false,
                autoUnlockProperties.getMinRssi(),
                autoUnlockProperties.getStableMillis(),
                autoUnlockProperties.getCooldownSeconds()
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

    /** 租约结束日期次日起标记到期，下架房源并撤销租客门锁权限。 */
    @Override
    @Transactional
    public int expireDueLeases() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Lease> dueLeases = list(
                Wrappers.<Lease>lambdaQuery()
                        .in(Lease::getStatus, "active", "effective")
                        .lt(Lease::getEndDate, today)
        );
        for (Lease lease : dueLeases) {
            lease.setStatus("expired");
            lease.setUpdatedAt(now);
            updateById(lease);

            if (StringUtils.hasText(lease.getContractId())) {
                RentContract contract = rentContractMapper.selectById(lease.getContractId());
                if (contract != null && !"terminated".equalsIgnoreCase(contract.getStatus())) {
                    contract.setStatus("expired");
                    contract.setUpdatedAt(now);
                    rentContractMapper.updateById(contract);
                }
            }

            House house = houseService.getById(lease.getHouseId());
            if (house != null && !"offline".equalsIgnoreCase(house.getStatus())) {
                house.setStatus("offline");
                house.setUpdatedAt(now);
                houseService.updateById(house);
            }

            lockPermissionService.revokeTenantEKeyForLease(lease.getId());
            lockPasscodePermissionService.revokePasscodesForLease(lease.getId());
        }
        return dueLeases.size();
    }

    /** 将已到达入住日期的待生效租约切换为生效中，并触发锁权限下发。 */
    @Override
    @Transactional
    public int activatePendingLeases() {
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Lease> pendingLeases = list(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getStatus, "pending")
                        .le(Lease::getStartDate, today)
        );
        for (Lease lease : pendingLeases) {
            lease.setStatus("active");
            lease.setUpdatedAt(now);
            updateById(lease);
            eventPublisher.publishEvent(new LeaseActivatedEvent(lease.getId()));
        }
        return pendingLeases.size();
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
                    + (house.getBuilding() != null ? house.getBuilding() + "栋" : "")
                    + (house.getUnit() != null ? house.getUnit() + "单元" : "")
                    + (house.getRoom() != null ? house.getRoom() : "");
            houseAddress = house.getAddress() != null ? house.getAddress() : "";
            houseSummary = (house.getRoomType() != null ? house.getRoomType() + " · " : "")
                    + (house.getArea() != null ? house.getArea().intValue() + "m² · " : "")
                    + (house.getOrientation() != null ? house.getOrientation() : "");
            houseImageUrl = house.getCoverImage() != null ? house.getCoverImage() : "";
            paymentMethod = house.getPaymentMethod() != null ? house.getPaymentMethod() : "";

            Landlord landlord = landlordService.findByUserId(house.getLandlordId());
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




