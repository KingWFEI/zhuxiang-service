package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.*;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RentOrderServiceImpl extends ServiceImpl<RentOrderMapper, RentOrder>
        implements RentOrderService {

    private static final int SERVICE_FEE = 20000;

    private static final Map<String, Integer> PAYMENT_MONTHS_MAP = Map.of(
            "monthly", 1,
            "quarterly", 3,
            "semi_annual", 6,
            "annual", 12
    );

    private final HouseService houseService;
    private final RentContractMapper rentContractMapper;
    private final LeaseService leaseService;
    private final LandlordService landlordService;
    private final LockDeviceService lockDeviceService;
    private final LockPermissionService lockPermissionService;
    private final FileRecordService fileRecordService;
    private final PaymentRecordService paymentRecordService;
    private final RentBillService rentBillService;
    private final ObjectMapper objectMapper;

    public RentOrderServiceImpl(
            HouseService houseService,
            RentContractMapper rentContractMapper,
            LeaseService leaseService,
            LandlordService landlordService,
            LockDeviceService lockDeviceService,
            LockPermissionService lockPermissionService,
            FileRecordService fileRecordService,
            PaymentRecordService paymentRecordService,
            RentBillService rentBillService,
            ObjectMapper objectMapper
    ) {
        this.houseService = houseService;
        this.rentContractMapper = rentContractMapper;
        this.leaseService = leaseService;
        this.landlordService = landlordService;
        this.lockDeviceService = lockDeviceService;
        this.lockPermissionService = lockPermissionService;
        this.fileRecordService = fileRecordService;
        this.paymentRecordService = paymentRecordService;
        this.rentBillService = rentBillService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public RentOrderResponse createOrder(String userId, CreateRentOrderRequest request) {
        // 用户对该房源没有进行中的订单
        long pendingCount = count(Wrappers.<RentOrder>lambdaQuery()
                .eq(RentOrder::getUserId, userId)
                .eq(RentOrder::getHouseId, request.houseId())
                .in(RentOrder::getStatus, "pendingRealName", "pendingContract", "pendingPayment", "pendingSign"));
        if (pendingCount > 0) {
            throw BusinessException.conflict("该房源已有进行中的订单");
        }

        String paymentMethod = request.paymentMethod();
        Integer paymentMonths = PAYMENT_MONTHS_MAP.get(paymentMethod);
        if (paymentMonths == null) {
            throw BusinessException.badRequest("不支持的付款方式");
        }
        if (paymentMonths > request.leaseMonths()) {
            throw BusinessException.badRequest("付款周期不能超过租期");
        }

        // 原子锁房：只有 status = available 时才能成功更新为 reserved
        LocalDateTime now = LocalDateTime.now();
        House house = houseService.getById(request.houseId());
        if (house == null) throw BusinessException.notFound("房源不存在");

        // 其他用户有进行中订单
        long otherPendingCount = count(Wrappers.<RentOrder>lambdaQuery()
                .eq(RentOrder::getHouseId, request.houseId())
                .ne(RentOrder::getUserId, userId)
                .in(RentOrder::getStatus, "pendingRealName", "pendingContract", "pendingPayment", "pendingSign"));
        if (otherPendingCount > 0) {
            throw BusinessException.conflict("该房源已有租客办理中，暂时无法发起新的租赁申请");
        }

        boolean locked = houseService.lambdaUpdate()
                .eq(House::getId, request.houseId())
                .eq(House::getStatus, "available")
                .set(House::getStatus, "reserved")
                .set(House::getUpdatedAt, now)
                .update();
        if (!locked) {
            throw BusinessException.conflict("该房源已被预定或已出租");
        }

        house.setStatus("reserved");

        // 安全性检查：确认没有活跃租约
        long activeLeaseCount = leaseService.count(Wrappers.<Lease>lambdaQuery()
                .eq(Lease::getHouseId, request.houseId())
                .eq(Lease::getStatus, "active"));
        if (activeLeaseCount > 0) {
            houseService.lambdaUpdate()
                    .eq(House::getId, request.houseId())
                    .set(House::getStatus, "available")
                    .set(House::getUpdatedAt, now)
                    .update();
            throw BusinessException.conflict("该房源已被租出");
        }

        LocalDate endDate = request.startDate().plusMonths(request.leaseMonths()).minusDays(1);
        int monthlyRent = house.getPrice();
        int deposit = house.getDeposit();
        int serviceFee = SERVICE_FEE;
        int firstPaymentAmount = monthlyRent * paymentMonths + deposit + serviceFee;
        int totalAmount = monthlyRent * request.leaseMonths() + deposit + serviceFee;

        RentOrder order = new RentOrder();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setHouseId(request.houseId());
        order.setStatus("pendingRealName");
        order.setStartDate(request.startDate());
        order.setEndDate(endDate);
        order.setLeaseMonths(request.leaseMonths());
        order.setPaymentMethod(paymentMethod);
        order.setPaymentMonths(paymentMonths);
        order.setTenantCount(request.tenantCount());
        order.setMonthlyRent(monthlyRent);
        order.setDeposit(deposit);
        order.setServiceFee(serviceFee);
        order.setFirstPaymentAmount(firstPaymentAmount);
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        save(order);

        return toResponse(order, house);
    }

    @Override
    public PageData<RentOrderResponse> listMyOrders(String userId, long page, long pageSize) {
        var result = page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize),
                Wrappers.<RentOrder>lambdaQuery()
                        .eq(RentOrder::getUserId, userId)
                        .eq(RentOrder::getUserHidden, 0)
                        .orderByDesc(RentOrder::getCreatedAt)
        );
        List<RentOrderResponse> items = result.getRecords().stream()
                .map(order -> {
                    House house = order.getHouseId() != null ? houseService.getById(order.getHouseId()) : null;
                    return toResponse(order, house);
                })
                .toList();
        return PageData.of(items, page, pageSize, result.getTotal());
    }

    @Override
    public RentOrderResponse getOrderDetail(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);
        House house = houseService.getById(order.getHouseId());
        return toResponse(order, house);
    }

    @Override
    @Transactional
    public RentOrderResponse submitRealName(String userId, String orderId, RealNameRequest request) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"pendingRealName".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许提交实名认证");
        }

        fileRecordService.validateFileOwnership(userId, request.idCardFrontUrl(), "id_card_front");
        fileRecordService.validateFileOwnership(userId, request.idCardBackUrl(), "id_card_back");

        order.setTenantName(request.tenantName());
        order.setTenantPhone(request.tenantPhone());
        order.setTenantIdCard(request.tenantIdCard());
        order.setStatus("pendingContract");
        order.setRealNameAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        House house = houseService.getById(order.getHouseId());
        String contractNo = generateContractNo();

        RentContract contract = new RentContract();
        contract.setId(UUID.randomUUID().toString());
        contract.setOrderId(orderId);
        contract.setUserId(userId);
        contract.setHouseId(order.getHouseId());
        contract.setContractNo(contractNo);
        contract.setStatus("draft");
        contract.setTenantName(request.tenantName());
        contract.setTenantPhone(request.tenantPhone());
        contract.setTenantIdCard(request.tenantIdCard());
        contract.setIdCardFrontUrl(request.idCardFrontUrl());
        contract.setIdCardBackUrl(request.idCardBackUrl());
        contract.setStartDate(order.getStartDate());
        contract.setEndDate(order.getEndDate());
        contract.setLeaseMonths(order.getLeaseMonths());
        contract.setMonthlyRent(order.getMonthlyRent());
        contract.setDeposit(order.getDeposit());
        contract.setServiceFee(order.getServiceFee());
        contract.setPaymentMonths(order.getPaymentMonths());
        contract.setFirstPaymentAmount(order.getFirstPaymentAmount());
        contract.setHouseName(house.getTitle());
        contract.setRoomName(formatRoomName(house));
        contract.setHouseAddress(house.getAddress());
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        rentContractMapper.insert(contract);

        return toResponse(order, house);
    }

    @Override
    public ContractPreviewResponse getContractPreview(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);
        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"),
                false
        );
        if (contract == null) {
            throw BusinessException.notFound("合同尚未生成");
        }

        // 获取房东名称
        String landlordName = "";
        House house = houseService.getById(contract.getHouseId());
        if (house != null && house.getLandlordId() != null) {
            Landlord landlord = landlordService.getById(house.getLandlordId());
            if (landlord != null) {
                landlordName = landlord.getName();
            }
        }

        // 生成合同条款
        List<String> clauses = buildContractClauses(contract);

        return new ContractPreviewResponse(
                orderId,
                contract.getContractNo(),
                contract.getStatus(),
                contract.getTenantName(),
                contract.getTenantPhone(),
                contract.getTenantIdCard(),
                contract.getHouseName(),
                contract.getRoomName(),
                contract.getHouseAddress(),
                landlordName,
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getLeaseMonths(),
                contract.getMonthlyRent(),
                contract.getDeposit(),
                contract.getServiceFee(),
                order.getPaymentMethod(),
                contract.getPaymentMonths(),
                clauses
        );
    }

    @Override
    @Transactional
    public RentOrderResponse confirmContract(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"pendingContract".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许确认合同");
        }

        order.setStatus("pendingPayment");
        order.setContractConfirmedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        House house = houseService.getById(order.getHouseId());
        return toResponse(order, house);
    }

    @Override
    public PaymentInfoResponse getPaymentInfo(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);

        return new PaymentInfoResponse(
                order.getId(),
                order.getFirstPaymentAmount(),
                order.getMonthlyRent(),
                order.getDeposit(),
                order.getServiceFee(),
                List.of("mock", "wechat", "alipay")
        );
    }

    @Override
    @Transactional
    public RentOrderResponse pay(String userId, String orderId, PayRequest request) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"pendingPayment".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许支付");
        }

        // 创建支付记录，包含费用明细拆账
        int rentAmount = order.getMonthlyRent() * order.getPaymentMonths();
        int depositAmount = order.getDeposit();
        int serviceFeeAmount = order.getServiceFee();

        String feeBreakdown = buildFeeBreakdown(rentAmount, depositAmount, serviceFeeAmount, order.getPaymentMonths());

        PaymentRecord record = new PaymentRecord();
        record.setId(UUID.randomUUID().toString());
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setAmount(order.getFirstPaymentAmount());
        record.setPaymentChannel(request.paymentChannel());
        record.setStatus("pending");
        record.setFeeBreakdown(feeBreakdown);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        paymentRecordService.save(record);

        // mock 渠道自动确认支付（开发阶段），真实渠道等待回调确认
        if ("mock".equals(request.paymentChannel())) {
            confirmPayment(record.getId(), null);
        }

        House house = houseService.getById(order.getHouseId());
        return toResponse(order, house);
    }

    @Override
    @Transactional
    public void confirmPayment(String recordId, String channelTradeNo) {
        PaymentRecord record = paymentRecordService.getById(recordId);
        if (record == null || !"pending".equals(record.getStatus())) {
            throw BusinessException.badRequest("支付记录不存在或状态不正确");
        }

        LocalDateTime now = LocalDateTime.now();
        String tradeNo = channelTradeNo != null ? channelTradeNo : "mock_" + UUID.randomUUID().toString().replace("-", "");

        record.setStatus("success");
        record.setChannelTradeNo(tradeNo);
        record.setPaidAt(now);
        record.setCallbackTime(now);
        record.setUpdatedAt(now);
        paymentRecordService.updateById(record);

        // 支付确认后推进订单状态
        RentOrder order = getById(record.getOrderId());
        if (order != null && "pendingPayment".equals(order.getStatus())) {
            order.setStatus("pendingSign");
            order.setPaidAt(now);
            order.setUpdatedAt(now);
            updateById(order);
        }
    }

    @Override
    @Transactional
    public RentOrderResponse sign(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"pendingSign".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许签约");
        }

        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"),
                false
        );

        LocalDateTime now = LocalDateTime.now();

        // 创建租约（补齐所有履约字段）
        Lease lease = new Lease();
        lease.setId(UUID.randomUUID().toString());
        lease.setUserId(userId);
        lease.setHouseId(order.getHouseId());
        lease.setStatus("active");
        lease.setStartDate(order.getStartDate());
        lease.setEndDate(order.getEndDate());
        lease.setLeaseMonths(order.getLeaseMonths());
        lease.setPaymentMethod(order.getPaymentMethod());
        lease.setPaymentMonths(order.getPaymentMonths());
        lease.setMonthlyRent(order.getMonthlyRent());
        lease.setDeposit(order.getDeposit());
        lease.setServiceFee(order.getServiceFee());
        lease.setFirstPaymentAmount(order.getFirstPaymentAmount());
        lease.setContractId(contract != null ? contract.getId() : null);
        lease.setCreatedAt(now);
        lease.setUpdatedAt(now);
        leaseService.save(lease);

        // 批量生成租金账单
        generateRentBills(lease, order);

        // 门锁授权（保持不变）
        LockDevice lockDevice = lockDeviceService.getOne(
                Wrappers.<LockDevice>lambdaQuery()
                        .eq(LockDevice::getHouseId, order.getHouseId())
                        .last("LIMIT 1"),
                false
        );
        if (lockDevice != null) {
            LockPermission permission = new LockPermission();
            permission.setId(UUID.randomUUID().toString());
            permission.setUserId(userId);
            permission.setLeaseId(lease.getId());
            permission.setLockId(lockDevice.getId());
            permission.setStatus("active");
            permission.setValidFrom(order.getStartDate().atStartOfDay());
            permission.setValidTo(order.getEndDate().atTime(23, 59, 59));
            permission.setCreatedAt(now);
            permission.setUpdatedAt(now);
            lockPermissionService.save(permission);
        }

        // 合同签署
        if (contract != null) {
            contract.setStatus("signed");
            contract.setSignedAt(now);
            contract.setUpdatedAt(now);
            rentContractMapper.updateById(contract);
        }

        // 订单完成
        order.setStatus("completed");
        order.setSignedAt(now);
        order.setUpdatedAt(now);
        updateById(order);

        // 房源置为已租
        House house = houseService.getById(order.getHouseId());
        if (house != null) {
            house.setStatus("rented");
            house.setUpdatedAt(now);
            houseService.updateById(house);
        }
        return toResponse(order, house);
    }

    @Override
    @Transactional
    public RentOrderResponse cancelOrder(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if ("completed".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
            throw BusinessException.badRequest("已完成或已取消的订单无法取消");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setStatus("cancelled");
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        updateById(order);

        // 释放房源（仅当状态仍是 reserved 时回退为 available）
        houseService.lambdaUpdate()
                .eq(House::getId, order.getHouseId())
                .eq(House::getStatus, "reserved")
                .set(House::getStatus, "available")
                .set(House::getUpdatedAt, now)
                .update();

        House house = houseService.getById(order.getHouseId());
        return toResponse(order, house);
    }

    @Override
    public void hideOrder(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"cancelled".equals(order.getStatus())) {
            throw BusinessException.badRequest("只有已取消订单可以删除记录");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setUserHidden(1);
        order.setHiddenAt(now);
        order.setUpdatedAt(now);
        updateById(order);
    }

    private void generateRentBills(Lease lease, RentOrder order) {
        LocalDate billStartDate = order.getStartDate();
        int leaseMonths = order.getLeaseMonths();
        int paidMonths = order.getPaymentMonths();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i <= leaseMonths; i++) {
            RentBill bill = new RentBill();
            bill.setId(UUID.randomUUID().toString());
            bill.setLeaseId(lease.getId());
            bill.setPeriodNo(i);
            bill.setAmountDue(order.getMonthlyRent());
            bill.setDueDate(billStartDate.plusMonths(i - 1));
            if (i <= paidMonths) {
                bill.setAmountPaid(order.getMonthlyRent());
                bill.setPaidAt(now);
                bill.setStatus("paid");
            } else {
                bill.setAmountPaid(0);
                bill.setStatus("pending");
            }
            bill.setCreatedAt(now);
            bill.setUpdatedAt(now);
            rentBillService.save(bill);
        }
    }

    private List<String> buildContractClauses(RentContract contract) {
        String months = contract.getLeaseMonths() + "";
        String monthlyRent = String.format("%.2f", contract.getMonthlyRent() / 100.0);
        String deposit = String.format("%.2f", contract.getDeposit() / 100.0);

        return List.of(
                "甲乙双方确认房源信息：甲方（出租方）将位于" + contract.getHouseAddress() + "的" + contract.getHouseName()
                        + "（" + contract.getRoomName() + "）出租给乙方（承租方）" + contract.getTenantName() + "使用。",
                "租赁期限：自" + formatLocalDate(contract.getStartDate()) + "起至" + formatLocalDate(contract.getEndDate())
                        + "止，共计" + months + "个月。",
                "租金及支付方式：房屋月租金为人民币" + monthlyRent + "元，押金为人民币" + deposit + "元。"
                        + "乙方应按合同约定及时支付租金。",
                "房屋用途：乙方承诺该房屋仅作为居住使用，不得擅自改变房屋用途或转租给第三方。",
                "维修责任：房屋及其设施设备的自然损耗由甲方负责维修。因乙方使用不当造成的损坏，由乙方负责维修或赔偿。",
                "合同解除：任何一方提前解除合同，应提前30日书面通知对方，并按合同约定承担违约责任。",
                "其他约定：双方确认本合同内容真实有效，未尽事宜另行协商解决。本合同一式两份，甲乙双方各执一份，具有同等法律效力。"
        );
    }

    private static String formatLocalDate(LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日"));
    }

    private String buildFeeBreakdown(int rentAmount, int depositAmount, int serviceFeeAmount, int paymentMonths) {
        try {
            List<Map<String, Object>> items = new ArrayList<>();
            items.add(Map.of("type", "rent", "amount", rentAmount, "description", "租金(" + paymentMonths + "个月)"));
            items.add(Map.of("type", "deposit", "amount", depositAmount, "description", "押金"));
            items.add(Map.of("type", "service_fee", "amount", serviceFeeAmount, "description", "服务费"));
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化费用明细失败", e);
        }
    }

    private RentOrder getOwnedOrder(String userId, String orderId) {
        RentOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw BusinessException.forbidden("无权操作该订单");
        }
        return order;
    }

    private RentOrderResponse toResponse(RentOrder order, House house) {
        String houseName = house != null ? house.getTitle() : "";
        String roomName = house != null ? formatRoomName(house) : "";
        String address = house != null ? house.getAddress() : "";

        return new RentOrderResponse(
                order.getId(), order.getUserId(), order.getHouseId(),
                order.getStatus(), house != null ? house.getStatus() : null,
                order.getStartDate(), order.getEndDate(),
                order.getLeaseMonths(), order.getPaymentMethod(), order.getPaymentMonths(),
                order.getTenantCount(),
                order.getMonthlyRent(), order.getDeposit(),
                order.getServiceFee(), order.getFirstPaymentAmount(),
                order.getTotalAmount(),
                order.getTenantName(), order.getTenantPhone(),
                order.getTenantIdCard(),
                order.getRealNameAt(), order.getContractConfirmedAt(),
                order.getPaidAt(), order.getSignedAt(),
                order.getCancelledAt(),
                order.getCreatedAt(), order.getUpdatedAt(),
                houseName, roomName, address
        );
    }

    private static String formatRoomName(House house) {
        return (house.getBuilding() != null ? house.getBuilding() : "")
                + (house.getUnit() != null ? house.getUnit() : "")
                + (house.getRoom() != null ? house.getRoom() : "");
    }

    private static String generateContractNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(10000);
        return "CT" + timestamp + String.format("%04d", random);
    }
}
