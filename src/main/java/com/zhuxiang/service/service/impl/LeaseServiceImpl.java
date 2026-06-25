package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.dto.LeaseDtos;
import com.zhuxiang.service.dto.ProfileDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.LeaseMapper;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.service.*;
import org.springframework.stereotype.Service;

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
    private final LockDeviceService lockDeviceService;
    private final LockPermissionService lockPermissionService;
    private final RentContractMapper rentContractMapper;
    private final LandlordService landlordService;

    public LeaseServiceImpl(
            HouseService houseService,
            CommunityService communityService,
            LockDeviceService lockDeviceService,
            LockPermissionService lockPermissionService,
            RentContractMapper rentContractMapper,
            LandlordService landlordService
    ) {
        this.houseService = houseService;
        this.communityService = communityService;
        this.lockDeviceService = lockDeviceService;
        this.lockPermissionService = lockPermissionService;
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
        LockDevice lock = lockDeviceService.getOne(
                Wrappers.<LockDevice>lambdaQuery()
                        .eq(LockDevice::getHouseId, house.getId())
                        .last("LIMIT 1"),
                false
        );
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
        LockDevice lock = lockDeviceService.getOne(
                Wrappers.<LockDevice>lambdaQuery()
                        .eq(LockDevice::getHouseId, lease.getHouseId())
                        .last("LIMIT 1"),
                false
        );
        if (lock == null) {
            return null;
        }
        LockPermission permission = lockPermissionService.getOne(
                Wrappers.<LockPermission>lambdaQuery()
                        .eq(LockPermission::getUserId, userId)
                        .eq(LockPermission::getLeaseId, lease.getId())
                        .eq(LockPermission::getLockId, lock.getId())
                        .last("LIMIT 1"),
                false
        );
        return new ProfileDtos.LockInfo(
                lock.getId(),
                lock.getLockName(),
                lock.getLockBrand(),
                lock.getStatus(),
                lock.getBatteryLevel(),
                lease.getId(),
                lease.getStatus(),
                lease.getStartDate() == null ? null : lease.getStartDate().toString(),
                lease.getEndDate() == null ? null : lease.getEndDate().toString(),
                permission == null ? null : permission.getStatus(),
                permission == null || permission.getValidFrom() == null ? null : permission.getValidFrom().toString(),
                permission == null || permission.getValidTo() == null ? null : permission.getValidTo().toString()
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

        LockDevice lock = lockDeviceService.getOne(
                Wrappers.<LockDevice>lambdaQuery()
                        .eq(LockDevice::getHouseId, lease.getHouseId())
                        .last("LIMIT 1"),
                false
        );
        String lockId = lock != null ? lock.getId() : null;

        String lockPermissionStatus = null;
        if (lock != null) {
            LockPermission permission = lockPermissionService.getOne(
                    Wrappers.<LockPermission>lambdaQuery()
                            .eq(LockPermission::getUserId, lease.getUserId())
                            .eq(LockPermission::getLeaseId, lease.getId())
                            .eq(LockPermission::getLockId, lock.getId())
                            .last("LIMIT 1"),
                    false
            );
            lockPermissionStatus = permission != null ? permission.getStatus() : "inactive";
        }

        return new LeaseDtos.LeaseItem(
                lease.getId(),
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




