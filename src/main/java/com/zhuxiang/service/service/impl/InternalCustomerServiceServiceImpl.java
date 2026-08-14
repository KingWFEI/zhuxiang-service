package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.dto.InternalCustomerServiceDtos;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.dto.LeaseTerminationDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.*;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.service.InternalCustomerServiceService;
import com.zhuxiang.service.service.LeaseTerminationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private final RentOrderMapper rentOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final DepositRecordMapper depositRecordMapper;
    private final LeaseTerminationApplicationMapper terminationMapper;
    private final RentContractMapper rentContractMapper;
    private final UserMapper userMapper;
    private final UserRealNameAuthMapper realNameAuthMapper;
    private final LandlordAuthApplicationMapper landlordAuthMapper;
    private final AppointmentService appointmentService;
    private final LeaseTerminationService terminationService;

    public InternalCustomerServiceServiceImpl(
            LeaseMapper leaseMapper,
            RentBillMapper rentBillMapper,
            LockPermissionMapper lockPermissionMapper,
            LockDeviceMapper lockDeviceMapper,
            SmartLockMapper smartLockMapper,
            AppointmentMapper appointmentMapper,
            RepairRecordMapper repairRecordMapper,
            HouseMapper houseMapper,
            CommunityMapper communityMapper,
            RentOrderMapper rentOrderMapper,
            PaymentRecordMapper paymentRecordMapper,
            DepositRecordMapper depositRecordMapper,
            LeaseTerminationApplicationMapper terminationMapper,
            RentContractMapper rentContractMapper,
            UserMapper userMapper,
            UserRealNameAuthMapper realNameAuthMapper,
            LandlordAuthApplicationMapper landlordAuthMapper,
            AppointmentService appointmentService,
            LeaseTerminationService terminationService
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
        this.rentOrderMapper = rentOrderMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.depositRecordMapper = depositRecordMapper;
        this.terminationMapper = terminationMapper;
        this.rentContractMapper = rentContractMapper;
        this.userMapper = userMapper;
        this.realNameAuthMapper = realNameAuthMapper;
        this.landlordAuthMapper = landlordAuthMapper;
        this.appointmentService = appointmentService;
        this.terminationService = terminationService;
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

    @Override
    public InternalCustomerServiceDtos.ServiceOverview getUserOverview(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        List<Lease> leases = leaseMapper.selectList(Wrappers.<Lease>lambdaQuery()
                .eq(Lease::getUserId, userId));
        List<String> leaseIds = leases.stream().map(Lease::getId).toList();
        long pendingBills = leaseIds.isEmpty() ? 0L : rentBillMapper.selectCount(
                Wrappers.<RentBill>lambdaQuery()
                        .in(RentBill::getLeaseId, leaseIds)
                        .in(RentBill::getStatus, "pending", "overdue"));
        long activeLeases = leases.stream()
                .filter(lease -> "active".equals(lease.getStatus()) || "pending".equals(lease.getStatus()))
                .count();
        long activeOrders = rentOrderMapper.selectCount(Wrappers.<RentOrder>lambdaQuery()
                .eq(RentOrder::getUserId, userId)
                .notIn(RentOrder::getStatus, "completed", "cancelled", "expired"));
        long activeAppointments = appointmentMapper.selectCount(Wrappers.<Appointment>lambdaQuery()
                .eq(Appointment::getUserId, userId)
                .notIn(Appointment::getStatus, "cancelled", "completed", "no_show", "rejected"));
        long activeRepairs = repairRecordMapper.selectCount(Wrappers.<RepairRecord>lambdaQuery()
                .eq(RepairRecord::getUserId, userId)
                .isNull(RepairRecord::getDeletedAt)
                .notIn(RepairRecord::getStatus, "completed", "cancelled"));
        return new InternalCustomerServiceDtos.ServiceOverview(
                user.getRole(),
                latestRealNameAuthStatus(userId),
                latestLandlordAuthStatus(userId, user),
                activeLeases,
                pendingBills,
                activeOrders,
                activeAppointments,
                activeRepairs
        );
    }

    @Override
    public List<InternalCustomerServiceDtos.HouseDetail> searchHouses(
            String keyword, String roomType, Integer minPrice, Integer maxPrice, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        var query = Wrappers.<House>lambdaQuery()
                .eq(House::getStatus, "available")
                .orderByDesc(House::getUpdatedAt)
                .last("LIMIT " + safeLimit);
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                    .like(House::getTitle, value)
                    .or().like(House::getLocation, value)
                    .or().like(House::getAddress, value)
                    .or().like(House::getRoomType, value));
        }
        if (roomType != null && !roomType.isBlank()) {
            query.like(House::getRoomType, roomType.trim());
        }
        if (minPrice != null) {
            query.ge(House::getPrice, minPrice);
        }
        if (maxPrice != null) {
            query.le(House::getPrice, maxPrice);
        }
        return houseMapper.selectList(query).stream().map(this::toHouseDetail).toList();
    }

    @Override
    public InternalCustomerServiceDtos.HouseDetail getHouseDetail(String houseId) {
        House house = houseMapper.selectById(houseId);
        if (house == null || !"available".equals(house.getStatus())) {
            throw BusinessException.notFound("房源不存在或当前不可查看");
        }
        return toHouseDetail(house);
    }

    @Override
    public AppointmentDtos.ViewingSlotResult getViewingSlots(String houseId, LocalDate startDate, int days) {
        return appointmentService.getViewingSlots(
                houseId,
                startDate == null ? LocalDate.now() : startDate,
                Math.max(1, Math.min(days, 14)),
                false
        );
    }

    @Override
    public List<InternalCustomerServiceDtos.RentOrderBrief> getUserRentOrders(String userId) {
        return rentOrderMapper.selectList(Wrappers.<RentOrder>lambdaQuery()
                        .eq(RentOrder::getUserId, userId)
                        .and(wrapper -> wrapper.isNull(RentOrder::getUserHidden)
                                .or().eq(RentOrder::getUserHidden, 0))
                        .orderByDesc(RentOrder::getCreatedAt)
                        .last("LIMIT 20"))
                .stream().map(this::toRentOrderBrief).toList();
    }

    @Override
    public InternalCustomerServiceDtos.RentOrderBrief getRentOrderDetail(String userId, String orderId) {
        RentOrder order = requireOwnedOrder(userId, orderId);
        return toRentOrderBrief(order);
    }

    @Override
    public InternalCustomerServiceDtos.LeaseDetail getLeaseDetail(String userId, String leaseId) {
        Lease lease = requireOwnedLease(userId, leaseId);
        House house = houseMapper.selectById(lease.getHouseId());
        return new InternalCustomerServiceDtos.LeaseDetail(
                lease.getId(), lease.getContractId(), lease.getOrderId(), lease.getHouseId(),
                house == null ? "" : house.getTitle(), house == null ? "" : house.getAddress(),
                lease.getStatus(), lease.getStartDate(), lease.getEndDate(), lease.getLeaseMonths(),
                lease.getMonthlyRent(), lease.getDeposit(), lease.getServiceFee(),
                lease.getPaymentMethod(), lease.getPaymentMonths()
        );
    }

    @Override
    public InternalCustomerServiceDtos.ContractSummary getContractSummary(String userId, String leaseId) {
        Lease lease = requireOwnedLease(userId, leaseId);
        RentContract contract = null;
        if (lease.getContractId() != null) {
            contract = rentContractMapper.selectById(lease.getContractId());
        }
        if (contract == null && lease.getOrderId() != null) {
            contract = rentContractMapper.selectOne(Wrappers.<RentContract>lambdaQuery()
                    .eq(RentContract::getOrderId, lease.getOrderId())
                    .last("LIMIT 1"));
        }
        if (contract == null) {
            return null;
        }
        return new InternalCustomerServiceDtos.ContractSummary(
                contract.getId(), lease.getId(), contract.getContractNo(), contract.getStatus(),
                Integer.valueOf(1).equals(contract.getTenantSigned()),
                Integer.valueOf(1).equals(contract.getLessorSigned()),
                contract.getHouseName(), contract.getStartDate(), contract.getEndDate(),
                contract.getMonthlyRent(), contract.getDeposit(), contract.getSignedAt()
        );
    }

    @Override
    public InternalCustomerServiceDtos.BillDetail getBillDetail(String userId, String billId) {
        RentBill bill = rentBillMapper.selectById(billId);
        if (bill == null) {
            throw BusinessException.notFound("账单不存在");
        }
        requireOwnedLease(userId, bill.getLeaseId());
        return new InternalCustomerServiceDtos.BillDetail(
                bill.getId(), bill.getLeaseId(), bill.getPeriodNo(), bill.getAmountDue(),
                bill.getAmountPaid(), bill.getOverdueAmount(), bill.getStatus(), bill.getDueDate(),
                bill.getPaidAt(), bill.getCreatedAt()
        );
    }

    @Override
    public List<InternalCustomerServiceDtos.PaymentBrief> getUserPayments(String userId) {
        return paymentRecordMapper.selectList(Wrappers.<PaymentRecord>lambdaQuery()
                        .eq(PaymentRecord::getUserId, userId)
                        .orderByDesc(PaymentRecord::getCreatedAt)
                        .last("LIMIT 20"))
                .stream().map(this::toPaymentBrief).toList();
    }

    @Override
    public InternalCustomerServiceDtos.PaymentBrief getPaymentDetail(String userId, String paymentId) {
        PaymentRecord payment = paymentRecordMapper.selectById(paymentId);
        if (payment == null) {
            throw BusinessException.notFound("支付记录不存在");
        }
        if (!userId.equals(payment.getUserId())) {
            throw BusinessException.forbidden("无权查看该支付记录");
        }
        return toPaymentBrief(payment);
    }

    @Override
    public LeaseTerminationDtos.TerminationCheckResponse checkTermination(String userId, String leaseId) {
        return terminationService.checkTermination(userId, leaseId);
    }

    @Override
    public InternalCustomerServiceDtos.TerminationStatus getTerminationStatus(String userId, String leaseId) {
        requireOwnedLease(userId, leaseId);
        LeaseTerminationApplication application = terminationMapper.selectOne(
                Wrappers.<LeaseTerminationApplication>lambdaQuery()
                        .eq(LeaseTerminationApplication::getTenantId, userId)
                        .eq(LeaseTerminationApplication::getLeaseId, leaseId)
                        .isNull(LeaseTerminationApplication::getDeletedAt)
                        .orderByDesc(LeaseTerminationApplication::getCreatedAt)
                        .last("LIMIT 1"));
        return application == null ? null : toTerminationStatus(application);
    }

    @Override
    public InternalCustomerServiceDtos.DepositSummary getDepositDetail(String userId, String leaseId) {
        requireOwnedLease(userId, leaseId);
        DepositRecord deposit = depositRecordMapper.selectOne(Wrappers.<DepositRecord>lambdaQuery()
                .eq(DepositRecord::getLeaseId, leaseId)
                .eq(DepositRecord::getUserId, userId)
                .last("LIMIT 1"));
        return deposit == null ? null : toDepositSummary(deposit);
    }

    @Override
    public InternalCustomerServiceDtos.RepairDetail getRepairDetail(String userId, String repairId) {
        RepairRecord repair = repairRecordMapper.selectById(repairId);
        if (repair == null || repair.getDeletedAt() != null) {
            throw BusinessException.notFound("报修记录不存在");
        }
        if (!userId.equals(repair.getUserId())) {
            throw BusinessException.forbidden("无权查看该报修记录");
        }
        return toRepairDetail(repair);
    }

    @Override
    public InternalCustomerServiceDtos.AppointmentDetail getAppointmentDetail(String userId, String appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw BusinessException.notFound("预约不存在");
        }
        if (!userId.equals(appointment.getUserId())) {
            throw BusinessException.forbidden("无权查看该预约");
        }
        House house = houseMapper.selectById(appointment.getHouseId());
        return new InternalCustomerServiceDtos.AppointmentDetail(
                appointment.getId(), appointment.getHouseId(), house == null ? "" : house.getTitle(),
                appointment.getStatus(), appointment.getSourceType(), appointment.getViewingMode(),
                appointment.getAppointmentStartAt(), appointment.getAppointmentEndAt(),
                appointment.getMeetingPoint(), appointment.getViewingInstruction(),
                appointment.getRejectReason(), appointment.getCancelReason(),
                appointment.getProposedStartAt(), appointment.getProposedEndAt(),
                appointment.getRescheduleReason(), appointment.getCreatedAt(), appointment.getUpdatedAt()
        );
    }

    @Override
    public InternalCustomerServiceDtos.AuthStatus getRealNameAuthStatus(String userId) {
        UserRealNameAuth auth = latestRealNameAuth(userId);
        return auth == null
                ? new InternalCustomerServiceDtos.AuthStatus("UNVERIFIED", null, null, null)
                : new InternalCustomerServiceDtos.AuthStatus(
                        auth.getAuthStatus(), auth.getFailureMessage(), auth.getVerifiedAt(), auth.getUpdatedAt());
    }

    @Override
    public InternalCustomerServiceDtos.LandlordAuthStatus getLandlordAuthStatus(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        LandlordAuthApplication application = latestLandlordAuth(userId);
        boolean landlord = "LANDLORD".equalsIgnoreCase(user.getRole());
        return application == null
                ? new InternalCustomerServiceDtos.LandlordAuthStatus(
                        landlord ? "APPROVED" : "NOT_SUBMITTED", landlord, !landlord, null, null, null)
                : new InternalCustomerServiceDtos.LandlordAuthStatus(
                        application.getStatus(), landlord, !landlord && !"PENDING".equals(application.getStatus()),
                        application.getRejectReason(), application.getReviewedAt(), application.getUpdatedAt());
    }

    private InternalCustomerServiceDtos.HouseDetail toHouseDetail(House house) {
        String communityName = "";
        if (house.getCommunityId() != null) {
            Community community = communityMapper.selectById(house.getCommunityId());
            communityName = community == null ? "" : community.getName();
        }
        return new InternalCustomerServiceDtos.HouseDetail(
                house.getId(), house.getTitle(), house.getLocation(), house.getAddress(), communityName,
                house.getRoomType(), house.getArea(), house.getPrice(), house.getDeposit(),
                house.getPaymentMethod(), house.getFloor(), house.getOrientation(), house.getDecoration(),
                house.getAvailableDate(), house.getMetro(), house.getRentMode(), house.getRentType(),
                house.getStatus(), Integer.valueOf(1).equals(house.getIsSmartLockSupported()),
                Integer.valueOf(1).equals(house.getIsSelfViewingSupported())
        );
    }

    private InternalCustomerServiceDtos.RentOrderBrief toRentOrderBrief(RentOrder order) {
        House house = houseMapper.selectById(order.getHouseId());
        return new InternalCustomerServiceDtos.RentOrderBrief(
                order.getId(), order.getHouseId(), house == null ? "" : house.getTitle(), order.getStatus(),
                order.getStartDate(), order.getEndDate(), order.getLeaseMonths(), order.getPaymentMethod(),
                order.getMonthlyRent(), order.getDeposit(), order.getServiceFee(), order.getFirstPaymentAmount(),
                order.getRealNameAt(), order.getContractConfirmedAt(), order.getPaidAt(), order.getSignedAt(),
                order.getPaymentDeadlineAt(), order.getCancelledAt(), order.getCancelReason(), order.getCreatedAt()
        );
    }

    private InternalCustomerServiceDtos.PaymentBrief toPaymentBrief(PaymentRecord payment) {
        return new InternalCustomerServiceDtos.PaymentBrief(
                payment.getId(), payment.getOrderId(), payment.getBillId(), payment.getLeaseId(),
                payment.getHouseName(), payment.getAmount(), payment.getPaymentChannel(), payment.getStatus(),
                payment.getType(), payment.getPaidAt(), payment.getCreatedAt()
        );
    }

    private InternalCustomerServiceDtos.TerminationStatus toTerminationStatus(
            LeaseTerminationApplication application) {
        return new InternalCustomerServiceDtos.TerminationStatus(
                application.getId(), application.getApplicationNo(), application.getLeaseId(),
                application.getHouseId(), application.getStatus(), application.getReason(),
                application.getExpectedMoveOutDate(), application.getRejectReason(),
                application.getSupplementReason(), application.getTotalDeduction(),
                application.getRefundAmount(), application.getCreatedAt(), application.getUpdatedAt()
        );
    }

    private InternalCustomerServiceDtos.DepositSummary toDepositSummary(DepositRecord deposit) {
        return new InternalCustomerServiceDtos.DepositSummary(
                deposit.getId(), deposit.getLeaseId(), deposit.getHouseId(), deposit.getAmount(),
                deposit.getWithheldAmount(), deposit.getRefundedAmount(), deposit.getStatus(),
                deposit.getSettlementDetail(), deposit.getTerminatedAt(), deposit.getRefundedAt(),
                deposit.getCreatedAt()
        );
    }

    private InternalCustomerServiceDtos.RepairDetail toRepairDetail(RepairRecord repair) {
        return new InternalCustomerServiceDtos.RepairDetail(
                repair.getId(), repair.getOrderNo(), repair.getHouseId(), repair.getHouseName(),
                repair.getRoomName(), repair.getRepairType(), repair.getDescription(),
                repair.getExpectedVisitTime(), repair.getStatus(), repair.getAssignee(),
                repair.getRepairmanName(), repair.getRating(), repair.getReviewContent(),
                repair.getCancelReason(), repair.getCompletedTime(), repair.getCreatedAt(), repair.getUpdatedAt()
        );
    }

    private RentOrder requireOwnedOrder(String userId, String orderId) {
        RentOrder order = rentOrderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("租房订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw BusinessException.forbidden("无权查看该租房订单");
        }
        return order;
    }

    private Lease requireOwnedLease(String userId, String leaseId) {
        Lease lease = leaseMapper.selectById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        if (!userId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权查看该租约");
        }
        return lease;
    }

    private UserRealNameAuth latestRealNameAuth(String userId) {
        return realNameAuthMapper.selectOne(Wrappers.<UserRealNameAuth>lambdaQuery()
                .eq(UserRealNameAuth::getUserId, userId)
                .orderByDesc(UserRealNameAuth::getCreatedAt)
                .last("LIMIT 1"));
    }

    private String latestRealNameAuthStatus(String userId) {
        UserRealNameAuth auth = latestRealNameAuth(userId);
        return auth == null ? "UNVERIFIED" : auth.getAuthStatus();
    }

    private LandlordAuthApplication latestLandlordAuth(String userId) {
        return landlordAuthMapper.selectOne(Wrappers.<LandlordAuthApplication>lambdaQuery()
                .eq(LandlordAuthApplication::getUserId, userId)
                .orderByDesc(LandlordAuthApplication::getCreatedAt)
                .last("LIMIT 1"));
    }

    private String latestLandlordAuthStatus(String userId, User user) {
        if ("LANDLORD".equalsIgnoreCase(user.getRole())) {
            return "APPROVED";
        }
        LandlordAuthApplication application = latestLandlordAuth(userId);
        return application == null ? "NOT_SUBMITTED" : application.getStatus();
    }
}
