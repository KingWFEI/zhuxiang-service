package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.dto.InternalCustomerServiceDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.*;
import com.zhuxiang.service.service.InternalCustomerServiceService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内部白名单业务数据查询实现 —— 返回脱敏后的最小必要字段
 */
@Service
public class InternalCustomerServiceServiceImpl implements InternalCustomerServiceService {

    private final LeaseMapper leaseMapper;
    private final RentBillMapper rentBillMapper;
    private final LockPermissionMapper lockPermissionMapper;
    private final LockDeviceMapper lockDeviceMapper;
    private final SmartLockMapper smartLockMapper;
    private final AppointmentMapper appointmentMapper;
    private final RepairRecordMapper repairRecordMapper;
    private final HouseMapper houseMapper;
    private final CommunityMapper communityMapper;

    public InternalCustomerServiceServiceImpl(
            LeaseMapper leaseMapper,
            RentBillMapper rentBillMapper,
            LockPermissionMapper lockPermissionMapper,
            LockDeviceMapper lockDeviceMapper,
            SmartLockMapper smartLockMapper,
            AppointmentMapper appointmentMapper,
            RepairRecordMapper repairRecordMapper,
            HouseMapper houseMapper,
            CommunityMapper communityMapper
    ) {
        this.leaseMapper = leaseMapper;
        this.rentBillMapper = rentBillMapper;
        this.lockPermissionMapper = lockPermissionMapper;
        this.lockDeviceMapper = lockDeviceMapper;
        this.smartLockMapper = smartLockMapper;
        this.appointmentMapper = appointmentMapper;
        this.repairRecordMapper = repairRecordMapper;
        this.houseMapper = houseMapper;
        this.communityMapper = communityMapper;
    }

    @Override
    public List<InternalCustomerServiceDtos.LeaseBrief> getUserLeases(String userId) {
        List<Lease> leases = leaseMapper.selectList(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getUserId, userId)
                        .orderByDesc(Lease::getCreatedAt)
        );
        if (leases.isEmpty()) {
            return Collections.emptyList();
        }
        // 查询关联房源名称
        List<String> houseIds = leases.stream()
                .map(Lease::getHouseId)
                .distinct()
                .toList();
        Map<String, String> houseNames = houseMapper.selectBatchIds(houseIds).stream()
                .collect(Collectors.toMap(House::getId, House::getTitle, (a, b) -> a));

        return leases.stream()
                .map(l -> new InternalCustomerServiceDtos.LeaseBrief(
                        l.getId(),
                        l.getHouseId(),
                        houseNames.getOrDefault(l.getHouseId(), ""),
                        l.getStatus(),
                        l.getStartDate(),
                        l.getEndDate(),
                        l.getMonthlyRent(),
                        l.getDeposit(),
                        l.getPaymentMethod(),
                        l.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public List<InternalCustomerServiceDtos.BillBrief> getUserBills(String userId) {
        // 通过用户租约查找账单
        List<Lease> leases = leaseMapper.selectList(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getUserId, userId)
        );
        if (leases.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> leaseIds = leases.stream().map(Lease::getId).toList();
        List<RentBill> bills = rentBillMapper.selectList(
                Wrappers.<RentBill>lambdaQuery()
                        .in(RentBill::getLeaseId, leaseIds)
                        .orderByDesc(RentBill::getDueDate)
        );
        return bills.stream()
                .map(b -> new InternalCustomerServiceDtos.BillBrief(
                        b.getId(),
                        b.getLeaseId(),
                        b.getPeriodNo(),
                        b.getAmountDue(),
                        b.getAmountPaid(),
                        b.getStatus(),
                        b.getDueDate(),
                        b.getPaidAt()
                ))
                .toList();
    }

    @Override
    public List<InternalCustomerServiceDtos.LockBrief> getUserLocks(String userId) {
        List<LockPermission> permissions = lockPermissionMapper.selectList(
                Wrappers.<LockPermission>lambdaQuery()
                        .eq(LockPermission::getTenantId, userId)
                        .eq(LockPermission::getStatus, "ACTIVE")
        );
        if (permissions.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询关联房源和门锁信息
        List<String> houseIds = permissions.stream()
                .map(LockPermission::getHouseId)
                .distinct()
                .toList();
        Map<String, String> houseNames = houseMapper.selectBatchIds(houseIds).stream()
                .collect(Collectors.toMap(House::getId, House::getTitle, (a, b) -> a));

        List<String> lockDeviceIds = permissions.stream()
                .map(LockPermission::getSmartLockId)
                .distinct()
                .toList();
        Map<String, LockDevice> lockDevices = lockDeviceMapper.selectBatchIds(lockDeviceIds).stream()
                .collect(Collectors.toMap(LockDevice::getId, ld -> ld, (a, b) -> a));

        return permissions.stream()
                .map(p -> {
                    LockDevice ld = lockDevices.get(p.getSmartLockId());
                    return new InternalCustomerServiceDtos.LockBrief(
                            p.getSmartLockId(),
                            p.getHouseId(),
                            houseNames.getOrDefault(p.getHouseId(), ""),
                            ld != null ? ld.getLockName() : "",
                            ld != null ? ld.getStatus() : "unknown",
                            ld != null ? ld.getBatteryLevel() : null,
                            p.getStatus(),
                            p.getStartTime(),
                            p.getEndTime()
                    );
                })
                .toList();
    }

    @Override
    public List<InternalCustomerServiceDtos.AppointmentBrief> getUserAppointments(String userId) {
        List<Appointment> appointments = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getUserId, userId)
                        .orderByDesc(Appointment::getCreatedAt)
        );
        return appointments.stream()
                .map(a -> new InternalCustomerServiceDtos.AppointmentBrief(
                        a.getId(),
                        a.getHouseId(),
                        a.getAppointmentDate(),
                        a.getTimeSlot(),
                        a.getStatus(),
                        a.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public List<InternalCustomerServiceDtos.RepairBrief> getUserRepairs(String userId) {
        List<RepairRecord> repairs = repairRecordMapper.selectList(
                Wrappers.<RepairRecord>lambdaQuery()
                        .eq(RepairRecord::getUserId, userId)
                        .orderByDesc(RepairRecord::getCreatedAt)
        );
        return repairs.stream()
                .map(r -> new InternalCustomerServiceDtos.RepairBrief(
                        r.getId(),
                        r.getOrderNo(),
                        r.getHouseId(),
                        r.getHouseName(),
                        r.getRepairType(),
                        r.getDescription(),
                        r.getStatus(),
                        r.getAssignee(),
                        r.getRating(),
                        r.getCreatedAt(),
                        r.getCompletedTime()
                ))
                .toList();
    }

    @Override
    public InternalCustomerServiceDtos.HouseBrief getHouseBrief(String houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null) {
            return null;
        }
        String communityName = "";
        if (house.getCommunityId() != null) {
            Community community = communityMapper.selectById(house.getCommunityId());
            if (community != null) {
                communityName = community.getName();
            }
        }
        return new InternalCustomerServiceDtos.HouseBrief(
                house.getId(),
                house.getTitle(),
                house.getAddress(),
                house.getRoomType(),
                house.getPrice() != null ? house.getPrice() : 0,
                communityName
        );
    }
}
