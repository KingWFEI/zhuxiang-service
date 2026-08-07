package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AdminOrderDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {
    private static final Set<String> ROLES = Set.of("ADMIN", "HOUSEKEEPER");
    private static final Set<String> STATUSES = Set.of("created", "pendingRealName", "pendingContract",
            "pendingTenantSign", "pendingPayment", "pendingLandlordSign",
            "paymentExpired", "completed", "cancelled");

    private final RentOrderMapper orderMapper;
    private final RentContractMapper contractMapper;
    private final HouseService houseService;
    private final UserService userService;
    private final PaymentRecordService paymentRecordService;

    public AdminOrderServiceImpl(RentOrderMapper orderMapper, RentContractMapper contractMapper,
                                 HouseService houseService, UserService userService,
                                 PaymentRecordService paymentRecordService) {
        this.orderMapper = orderMapper; this.contractMapper = contractMapper;
        this.houseService = houseService; this.userService = userService;
        this.paymentRecordService = paymentRecordService;
    }

    @Override
    public PageData<AdminOrderDtos.OrderView> list(String operatorId, String status, String keyword,
                                                   long page, long pageSize) {
        requireRole(operatorId);
        String normalizedStatus = StringUtils.hasText(status) ? status.trim() : null;
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) throw BusinessException.badRequest("不支持的订单状态");
        String q = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<String> houseIds = q == null ? List.of() : houseService.list(Wrappers.<House>lambdaQuery()
                .select(House::getId).and(w -> w.like(House::getTitle, q).or().like(House::getAddress, q)))
                .stream().map(House::getId).toList();
        List<String> contractOrderIds = q == null ? List.of() : contractMapper.selectList(Wrappers.<RentContract>lambdaQuery()
                .select(RentContract::getOrderId).and(w -> w.like(RentContract::getContractNo, q)
                        .or().like(RentContract::getContractNum, q)))
                .stream().map(RentContract::getOrderId).filter(Objects::nonNull).toList();

        LambdaQueryWrapper<RentOrder> wrapper = Wrappers.<RentOrder>lambdaQuery()
                .eq(normalizedStatus != null, RentOrder::getStatus, normalizedStatus);
        if (q != null) wrapper.and(w -> {
            w.like(RentOrder::getId, q).or().like(RentOrder::getTenantName, q).or().like(RentOrder::getTenantPhone, q);
            if (!houseIds.isEmpty()) w.or().in(RentOrder::getHouseId, houseIds);
            if (!contractOrderIds.isEmpty()) w.or().in(RentOrder::getId, contractOrderIds);
        });
        wrapper.orderByDesc(RentOrder::getCreatedAt);
        Page<RentOrder> result = orderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Context context = loadContext(result.getRecords());
        return PageData.of(result.getRecords().stream().map(o -> view(o, context)).toList(),
                page, pageSize, result.getTotal());
    }

    @Override
    public AdminOrderDtos.OrderView get(String operatorId, String orderId) {
        requireRole(operatorId);
        RentOrder order = orderMapper.selectById(orderId);
        if (order == null) throw BusinessException.notFound("订单不存在");
        return view(order, loadContext(List.of(order)));
    }

    private Context loadContext(List<RentOrder> orders) {
        Set<String> houseIds = orders.stream().map(RentOrder::getHouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> lessorIds = orders.stream().map(RentOrder::getLessorUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> orderIds = orders.stream().map(RentOrder::getId).collect(Collectors.toSet());
        Map<String, House> houses = houseIds.isEmpty() ? Map.of() : houseService.listByIds(houseIds).stream()
                .collect(Collectors.toMap(House::getId, Function.identity()));
        Map<String, User> lessors = lessorIds.isEmpty() ? Map.of() : userService.listByIds(lessorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<String, RentContract> contracts = orderIds.isEmpty() ? Map.of() : contractMapper.selectList(
                Wrappers.<RentContract>lambdaQuery().in(RentContract::getOrderId, orderIds)).stream()
                .collect(Collectors.toMap(RentContract::getOrderId, Function.identity(), (a, b) -> a));
        Map<String, PaymentRecord> payments = orderIds.isEmpty() ? Map.of() : paymentRecordService.list(
                Wrappers.<PaymentRecord>lambdaQuery().in(PaymentRecord::getOrderId, orderIds)
                        .eq(PaymentRecord::getType, "rent").orderByDesc(PaymentRecord::getCreatedAt)).stream()
                .collect(Collectors.toMap(PaymentRecord::getOrderId, Function.identity(), (a, b) -> a));
        return new Context(houses, lessors, contracts, payments);
    }

    private AdminOrderDtos.OrderView view(RentOrder o, Context context) {
        House h = o.getHouseId() == null ? null : context.houses().get(o.getHouseId());
        User lessor = o.getLessorUserId() == null ? null : context.lessors().get(o.getLessorUserId());
        RentContract contract = context.contracts().get(o.getId()); PaymentRecord payment = context.payments().get(o.getId());
        String landlordName = contract != null && StringUtils.hasText(contract.getLandlordName())
                ? contract.getLandlordName() : lessor == null ? null : lessor.getNickname();
        String landlordPhone = contract != null && StringUtils.hasText(contract.getLandlordPhone())
                ? contract.getLandlordPhone() : lessor == null ? null : lessor.getPhone();
        return new AdminOrderDtos.OrderView(o.getId(), o.getStatus(), o.getUserId(), o.getLessorUserId(),
                o.getTenantName(), o.getTenantPhone(), landlordName, landlordPhone, o.getHouseId(),
                h == null ? null : h.getTitle(), h == null ? null : room(h), h == null ? null : h.getAddress(),
                h == null ? null : h.getStatus(), o.getStartDate(), o.getEndDate(), o.getLeaseMonths(),
                o.getPaymentMethod(), o.getPaymentMonths(), o.getTenantCount(), o.getMonthlyRent(), o.getDeposit(),
                o.getServiceFee(), o.getFirstPaymentAmount(), o.getTotalAmount(), contract == null ? null : contract.getId(),
                contract == null ? null : contract.getContractNo(), contract == null ? null : contract.getStatus(),
                payment == null ? null : payment.getPaymentNo(), payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getPaymentChannel(), o.getRealNameAt(), o.getContractConfirmedAt(),
                o.getPaidAt(), o.getSignedAt(), o.getCancelledAt(), o.getCreatedAt(), o.getUpdatedAt());
    }

    private String room(House h) { return nvl(h.getBuilding()) + (StringUtils.hasText(h.getBuilding()) ? "栋" : "")
            + nvl(h.getUnit()) + (StringUtils.hasText(h.getUnit()) ? "单元" : "") + nvl(h.getRoom()); }
    private String nvl(String s) { return s == null ? "" : s; }
    private void requireRole(String id) { User user = userService.requireActiveUser(id);
        if (!ROLES.contains(user.getRole())) throw BusinessException.forbidden("当前账号无权查看全部订单"); }
    private record Context(Map<String, House> houses, Map<String, User> lessors,
                           Map<String, RentContract> contracts, Map<String, PaymentRecord> payments) {}
}
