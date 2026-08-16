package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminBillDtos;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.Lease;
import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.entity.RentBill;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RentBillMapper;
import com.zhuxiang.service.service.AdminBillService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.LeaseService;
import com.zhuxiang.service.service.PaymentRecordService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminBillServiceImpl implements AdminBillService {

    private static final Set<String> MANAGEMENT_ROLES = Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD");
    private static final Set<String> BILL_STATUSES = Set.of(
            "scheduled", "pending", "paid", "overdue", "cancelled"
    );

    private final RentBillMapper rentBillMapper;
    private final LeaseService leaseService;
    private final UserService userService;
    private final HouseService houseService;
    private final PaymentRecordService paymentRecordService;

    public AdminBillServiceImpl(
            RentBillMapper rentBillMapper,
            LeaseService leaseService,
            UserService userService,
            HouseService houseService,
            PaymentRecordService paymentRecordService
    ) {
        this.rentBillMapper = rentBillMapper;
        this.leaseService = leaseService;
        this.userService = userService;
        this.houseService = houseService;
        this.paymentRecordService = paymentRecordService;
    }

    @Override
    public PageData<AdminBillDtos.BillView> getBills(
            String operatorId,
            String status,
            String keyword,
            LocalDate dueDateStart,
            LocalDate dueDateEnd,
            long page,
            long pageSize
    ) {
        User operator = requireManagementRole(operatorId);
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        validateDateRange(dueDateStart, dueDateEnd);

        Set<String> accessibleLeaseIds = findAccessibleLeaseIds(operator);
        if (accessibleLeaseIds != null && accessibleLeaseIds.isEmpty()) {
            return PageData.of(List.of(), page, pageSize, 0);
        }

        LambdaQueryWrapper<RentBill> query = Wrappers.<RentBill>lambdaQuery()
                .eq(normalizedStatus != null, RentBill::getStatus, normalizedStatus)
                .ge(dueDateStart != null, RentBill::getDueDate, dueDateStart)
                .le(dueDateEnd != null, RentBill::getDueDate, dueDateEnd)
                .in(accessibleLeaseIds != null, RentBill::getLeaseId, accessibleLeaseIds)
                .orderByDesc(RentBill::getDueDate)
                .orderByDesc(RentBill::getCreatedAt);

        if (normalizedKeyword != null) {
            Set<String> matchingLeaseIds = findMatchingLeaseIds(normalizedKeyword);
            if (accessibleLeaseIds != null) {
                matchingLeaseIds.retainAll(accessibleLeaseIds);
            }
            query.and(wrapper -> {
                wrapper.like(RentBill::getId, normalizedKeyword)
                        .or().like(RentBill::getLeaseId, normalizedKeyword);
                if (!matchingLeaseIds.isEmpty()) {
                    wrapper.or().in(RentBill::getLeaseId, matchingLeaseIds);
                }
            });
        }

        IPage<RentBill> result = rentBillMapper.selectPage(new Page<>(page, pageSize), query);
        return PageData.of(toViews(result.getRecords()), page, pageSize, result.getTotal());
    }

    @Override
    public AdminBillDtos.BillView getBill(String operatorId, String billId) {
        User operator = requireManagementRole(operatorId);
        RentBill bill = rentBillMapper.selectById(billId);
        if (bill == null) {
            throw BusinessException.notFound("账单不存在");
        }
        ensureAccessible(operator, bill);
        return toViews(List.of(bill)).getFirst();
    }

    @Override
    public AdminBillDtos.BillSummary getSummary(String operatorId) {
        User operator = requireManagementRole(operatorId);
        AdminBillDtos.BillSummary summary = rentBillMapper.selectAdminSummary(
                "LANDLORD".equals(operator.getRole()) ? operator.getId() : null
        );
        return summary == null ? AdminBillDtos.BillSummary.empty() : summary;
    }

    private Set<String> findAccessibleLeaseIds(User operator) {
        if (!"LANDLORD".equals(operator.getRole())) {
            return null;
        }
        List<String> houseIds = houseService.list(
                Wrappers.<House>lambdaQuery()
                        .select(House::getId)
                        .eq(House::getLandlordId, operator.getId())
        ).stream().map(House::getId).toList();
        if (houseIds.isEmpty()) {
            return Set.of();
        }
        return leaseService.list(
                Wrappers.<Lease>lambdaQuery()
                        .select(Lease::getId)
                        .in(Lease::getHouseId, houseIds)
        ).stream().map(Lease::getId).collect(Collectors.toSet());
    }

    private Set<String> findMatchingLeaseIds(String keyword) {
        List<String> tenantIds = userService.list(
                Wrappers.<User>lambdaQuery()
                        .select(User::getId)
                        .and(wrapper -> wrapper.like(User::getNickname, keyword)
                                .or().like(User::getPhone, keyword))
        ).stream().map(User::getId).toList();
        List<String> houseIds = houseService.list(
                Wrappers.<House>lambdaQuery()
                        .select(House::getId)
                        .and(wrapper -> wrapper.like(House::getTitle, keyword)
                                .or().like(House::getAddress, keyword)
                                .or().like(House::getBuilding, keyword)
                                .or().like(House::getUnit, keyword)
                                .or().like(House::getRoom, keyword))
        ).stream().map(House::getId).toList();

        return leaseService.list(
                Wrappers.<Lease>lambdaQuery()
                        .select(Lease::getId)
                        .and(wrapper -> {
                            wrapper.like(Lease::getId, keyword);
                            if (!tenantIds.isEmpty()) {
                                wrapper.or().in(Lease::getUserId, tenantIds);
                            }
                            if (!houseIds.isEmpty()) {
                                wrapper.or().in(Lease::getHouseId, houseIds);
                            }
                        })
        ).stream().map(Lease::getId).collect(Collectors.toSet());
    }

    private List<AdminBillDtos.BillView> toViews(List<RentBill> bills) {
        if (bills.isEmpty()) {
            return List.of();
        }
        Set<String> leaseIds = bills.stream().map(RentBill::getLeaseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, Lease> leases = leaseIds.isEmpty() ? Collections.emptyMap() : leaseService.listByIds(leaseIds)
                .stream().collect(Collectors.toMap(Lease::getId, Function.identity()));

        Set<String> tenantIds = leases.values().stream().map(Lease::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, User> tenants = tenantIds.isEmpty() ? Collections.emptyMap() : userService.listByIds(tenantIds)
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        Set<String> houseIds = leases.values().stream().map(Lease::getHouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, House> houses = houseIds.isEmpty() ? Collections.emptyMap() : houseService.listByIds(houseIds)
                .stream().collect(Collectors.toMap(House::getId, Function.identity()));

        Set<String> billIds = bills.stream().map(RentBill::getId).collect(Collectors.toSet());
        Set<String> orderIds = leases.values().stream().map(Lease::getOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<String, PaymentRecord> billPayments = new LinkedHashMap<>();
        Map<String, PaymentRecord> orderPayments = new LinkedHashMap<>();
        paymentRecordService.list(
                Wrappers.<PaymentRecord>lambdaQuery()
                        .and(wrapper -> {
                            wrapper.in(PaymentRecord::getBillId, billIds);
                            if (!orderIds.isEmpty()) {
                                wrapper.or().in(PaymentRecord::getOrderId, orderIds);
                            }
                        })
                        .eq(PaymentRecord::getType, "rent")
                        .orderByDesc(PaymentRecord::getCreatedAt)
        ).forEach(record -> {
            if (record.getBillId() != null) {
                billPayments.putIfAbsent(record.getBillId(), record);
            }
            if (record.getOrderId() != null && "success".equals(record.getStatus())) {
                orderPayments.putIfAbsent(record.getOrderId(), record);
            }
        });

        return bills.stream().map(bill -> {
            Lease lease = leases.get(bill.getLeaseId());
            User tenant = lease == null ? null : tenants.get(lease.getUserId());
            House house = lease == null ? null : houses.get(lease.getHouseId());
            PaymentRecord payment = billPayments.get(bill.getId());
            if (payment == null && "paid".equals(bill.getStatus()) && lease != null) {
                payment = orderPayments.get(lease.getOrderId());
            }
            int amountDue = bill.getAmountDue() == null ? 0 : bill.getAmountDue();
            int amountPaid = bill.getAmountPaid() == null ? 0 : bill.getAmountPaid();
            int overdueAmount = bill.getOverdueAmount() == null ? 0 : bill.getOverdueAmount();
            int outstandingAmount = "cancelled".equals(bill.getStatus())
                    ? 0 : Math.max(amountDue + overdueAmount - amountPaid, 0);
            return new AdminBillDtos.BillView(
                    bill.getId(), bill.getLeaseId(), bill.getPeriodNo(), amountDue, amountPaid,
                    overdueAmount, outstandingAmount, bill.getDueDate(),
                    payment != null && payment.getPaidAt() != null ? payment.getPaidAt() : bill.getPaidAt(),
                    bill.getStatus(),
                    tenant == null ? null : tenant.getId(),
                    tenant == null ? null : tenant.getNickname(),
                    tenant == null ? null : tenant.getPhone(),
                    house == null ? null : house.getId(),
                    house == null ? null : house.getTitle(),
                    house == null ? null : house.getAddress(),
                    lease == null ? null : lease.getStatus(),
                    payment == null ? null : payment.getPaymentNo(),
                    payment == null ? null : payment.getPaymentChannel(),
                    payment == null ? null : payment.getStatus(),
                    payment == null ? null : payment.getChannelTradeNo(),
                    bill.getCreatedAt(), bill.getUpdatedAt()
            );
        }).toList();
    }

    private void ensureAccessible(User operator, RentBill bill) {
        if (!"LANDLORD".equals(operator.getRole())) {
            return;
        }
        Lease lease = leaseService.getById(bill.getLeaseId());
        House house = lease == null ? null : houseService.getById(lease.getHouseId());
        if (house == null || !operator.getId().equals(house.getLandlordId())) {
            throw BusinessException.forbidden("当前账号无权查看该账单");
        }
    }

    private User requireManagementRole(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (!MANAGEMENT_ROLES.contains(operator.getRole())) {
            throw BusinessException.forbidden("当前账号无权查看管理端账单");
        }
        return operator;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (!BILL_STATUSES.contains(normalized)) {
            throw BusinessException.badRequest("不支持的账单状态");
        }
        return normalized;
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw BusinessException.badRequest("到期日开始时间不能晚于结束时间");
        }
    }
}
