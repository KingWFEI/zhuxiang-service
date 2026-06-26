package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.BusinessException;
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
import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
    private final LockDeviceService lockDeviceService;
    private final LockPermissionService lockPermissionService;
    private final FileRecordService fileRecordService;

    public RentOrderServiceImpl(
            HouseService houseService,
            RentContractMapper rentContractMapper,
            LeaseService leaseService,
            LockDeviceService lockDeviceService,
            LockPermissionService lockPermissionService,
            FileRecordService fileRecordService
    ) {
        this.houseService = houseService;
        this.rentContractMapper = rentContractMapper;
        this.leaseService = leaseService;
        this.lockDeviceService = lockDeviceService;
        this.lockPermissionService = lockPermissionService;
        this.fileRecordService = fileRecordService;
    }

    @Override
    @Transactional
    public RentOrderResponse createOrder(String userId, CreateRentOrderRequest request) {
        House house = houseService.requireAvailableHouse(request.houseId());

        long activeLeaseCount = leaseService.count(Wrappers.<Lease>lambdaQuery()
                .eq(Lease::getHouseId, request.houseId())
                .eq(Lease::getStatus, "active"));
        if (activeLeaseCount > 0) {
            throw BusinessException.conflict("该房源已被租出");
        }

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
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        save(order);

        return toResponse(order, house);
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
        return new ContractPreviewResponse(
                contract.getContractNo(),
                contract.getStatus(),
                contract.getTenantName(),
                contract.getTenantPhone(),
                contract.getTenantIdCard(),
                contract.getHouseName(),
                contract.getRoomName(),
                contract.getHouseAddress(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getLeaseMonths(),
                contract.getMonthlyRent(),
                contract.getDeposit(),
                contract.getServiceFee(),
                contract.getFirstPaymentAmount(),
                contract.getPaymentMonths()
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
                order.getStatus(),
                order.getMonthlyRent(),
                order.getDeposit(),
                order.getServiceFee(),
                order.getFirstPaymentAmount(),
                order.getPaymentMethod(),
                order.getPaymentMonths()
        );
    }

    @Override
    @Transactional
    public RentOrderResponse pay(String userId, String orderId, PayRequest request) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"pendingPayment".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许支付");
        }

        order.setPaymentMethod(request.paymentMethod());
        order.setStatus("pendingSign");
        order.setPaidAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);

        House house = houseService.getById(order.getHouseId());
        return toResponse(order, house);
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

        Lease lease = new Lease();
        lease.setId(UUID.randomUUID().toString());
        lease.setUserId(userId);
        lease.setHouseId(order.getHouseId());
        lease.setStatus("active");
        lease.setStartDate(order.getStartDate());
        lease.setEndDate(order.getEndDate());
        lease.setMonthlyRent(order.getMonthlyRent());
        lease.setDeposit(order.getDeposit());
        lease.setContractId(contract != null ? contract.getId() : null);
        lease.setCreatedAt(now);
        lease.setUpdatedAt(now);
        leaseService.save(lease);

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

        if (contract != null) {
            contract.setStatus("signed");
            contract.setSignedAt(now);
            contract.setUpdatedAt(now);
            rentContractMapper.updateById(contract);
        }

        order.setStatus("completed");
        order.setSignedAt(now);
        order.setUpdatedAt(now);
        updateById(order);

        House house = houseService.getById(order.getHouseId());
        if (house != null) {
            house.setStatus("rented");
            house.setUpdatedAt(now);
            houseService.updateById(house);
        }
        return toResponse(order, house);
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
                order.getStatus(),
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
