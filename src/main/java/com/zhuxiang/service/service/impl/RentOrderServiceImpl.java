package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.EsignException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.dto.*;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.event.LeaseActivatedEvent;
import com.zhuxiang.service.mapper.RentContractMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.mapper.UserRealNameAuthMapper;
import com.zhuxiang.service.service.*;
import com.zhuxiang.service.service.EsignCallbackData;
import com.zhuxiang.service.service.IdCardCryptoService;
import com.zhuxiang.service.service.InspectionService;
import com.zhuxiang.service.service.RealNameAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class RentOrderServiceImpl extends ServiceImpl<RentOrderMapper, RentOrder>
        implements RentOrderService {

    private static final Logger log = LoggerFactory.getLogger(RentOrderServiceImpl.class);

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
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileRecordService fileRecordService;
    private final PaymentRecordService paymentRecordService;
    private final RentBillService rentBillService;
    private final AlipayService alipayService;
    private final DepositService depositService;
    private final InspectionService inspectionService;
    private final ObjectMapper objectMapper;
    private final RealNameAuthService realNameAuthService;
    private final UserRealNameAuthMapper userRealNameAuthMapper;
    private final IdCardCryptoService idCardCryptoService;
    private final EsignV3Client esignV3Client;
    private final EsignV3Properties esignV3Properties;
    private AdminContractTemplateService adminContractTemplateService;
    private final CommunityService communityService;

    public RentOrderServiceImpl(
            HouseService houseService,
            RentContractMapper rentContractMapper,
            LeaseService leaseService,
            UserService userService,
            ApplicationEventPublisher eventPublisher,
            FileRecordService fileRecordService,
            PaymentRecordService paymentRecordService,
            RentBillService rentBillService,
            AlipayService alipayService,
            DepositService depositService,
            ObjectMapper objectMapper,
            RealNameAuthService realNameAuthService,
            UserRealNameAuthMapper userRealNameAuthMapper,
            IdCardCryptoService idCardCryptoService,
            EsignV3Client esignV3Client,
            EsignV3Properties esignV3Properties,
            InspectionService inspectionService,
            CommunityService communityService
    ) {
        this.houseService = houseService;
        this.rentContractMapper = rentContractMapper;
        this.leaseService = leaseService;
        this.eventPublisher = eventPublisher;
        this.fileRecordService = fileRecordService;
        this.paymentRecordService = paymentRecordService;
        this.rentBillService = rentBillService;
        this.alipayService = alipayService;
        this.depositService = depositService;
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.realNameAuthService = realNameAuthService;
        this.userRealNameAuthMapper = userRealNameAuthMapper;
        this.idCardCryptoService = idCardCryptoService;
        this.esignV3Client = esignV3Client;
        this.esignV3Properties = esignV3Properties;
        this.inspectionService = inspectionService;
        this.communityService = communityService;
    }

    @Autowired(required = false)
    void setAdminContractTemplateService(AdminContractTemplateService service) {
        this.adminContractTemplateService = service;
    }

    @Override
    @Transactional
    public RentOrderResponse createOrder(String userId, CreateRentOrderRequest request) {
        // 1. 校验全局实名认证状态
        if (!realNameAuthService.isVerified(userId)) {
            throw BusinessException.realNameRequired();
        }

        // 2. 读取已认证的租客信息
        UserRealNameAuth verifiedAuth = userRealNameAuthMapper.selectOne(
                new LambdaQueryWrapper<UserRealNameAuth>()
                        .eq(UserRealNameAuth::getUserId, userId)
                        .eq(UserRealNameAuth::getAuthStatus, "VERIFIED")
                        .orderByDesc(UserRealNameAuth::getCreatedAt)
                        .last("LIMIT 1"));
        if (verifiedAuth == null) {
            throw BusinessException.realNameRequired();
        }

        // 解密完整身份证号（仅在后端生成合同时短暂使用，不通过接口返回）
        String fullIdCard = idCardCryptoService.decrypt(verifiedAuth.getIdCardCiphertext());

        // 3. 查询该用户+房源下所有进行中/已完成订单（FOR UPDATE 防并发）
        RentOrder existing = getBaseMapper().selectOne(
                Wrappers.<RentOrder>lambdaQuery()
                        .eq(RentOrder::getUserId, userId)
                        .eq(RentOrder::getHouseId, request.houseId())
                        .in(RentOrder::getStatus, "created", "pendingRealName",
                                "pendingContract", "pendingPayment", "pendingEsign", "completed")
                        .last("LIMIT 1 FOR UPDATE"));

        if (existing != null) {
            String status = existing.getStatus();

            // completed → 不允许再申请
            if ("completed".equals(status)) {
                throw BusinessException.conflict("该房源已完成租住");
            }

            // pendingContract / pendingPayment / pendingSign → 直接返回
            if ("pendingContract".equals(status) || "pendingPayment".equals(status)
                    || "pendingEsign".equals(status)) {
                House house = houseService.getById(existing.getHouseId());
                return toResponse(existing, house);
            }

            // created / pendingRealName → 升级为 pendingContract + 补合同
            House upgHouse = houseService.getById(existing.getHouseId());
            existing.setTenantName(verifiedAuth.getRealName());
            existing.setTenantPhone(verifiedAuth.getAccountMobile());
            existing.setTenantIdCard(fullIdCard);
            existing.setLessorUserId(getLessorUserId(upgHouse));
            existing.setStatus("pendingContract");
            existing.setRealNameAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            updateById(existing);

            Long contractCount = rentContractMapper.selectCount(
                    Wrappers.<RentContract>lambdaQuery()
                            .eq(RentContract::getOrderId, existing.getId()));
            if (contractCount == null || contractCount == 0) {
                createContract(existing, verifiedAuth, fullIdCard);
            }

            House house = houseService.getById(existing.getHouseId());
            return toResponse(existing, house);
        }

        // 4. 参数校验
        String paymentMethod = request.paymentMethod();
        Integer paymentMonths = PAYMENT_MONTHS_MAP.get(paymentMethod);
        if (paymentMonths == null) {
            throw BusinessException.badRequest("不支持的付款方式");
        }
        if (paymentMonths > request.leaseMonths()) {
            throw BusinessException.badRequest("付款周期不能超过租期");
        }

        // 6. 原子锁房
        LocalDateTime now = LocalDateTime.now();
        House house = houseService.getById(request.houseId());
        if (house == null) throw BusinessException.notFound("房源不存在");

        long otherPendingCount = count(Wrappers.<RentOrder>lambdaQuery()
                .eq(RentOrder::getHouseId, request.houseId())
                .ne(RentOrder::getUserId, userId)
                .in(RentOrder::getStatus, "created", "pendingRealName",
                        "pendingContract", "pendingPayment", "pendingEsign"));
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

        // 7. 创建订单（状态直接为 pendingContract）
        RentOrder order = new RentOrder();
        order.setId(UUID.randomUUID().toString());
        order.setUserId(userId);
        order.setLessorUserId(getLessorUserId(house));
        order.setHouseId(request.houseId());
        order.setStatus("pendingContract");
        order.setStartDate(request.startDate());
        order.setEndDate(endDate);
        order.setLeaseMonths(request.leaseMonths());
        order.setPaymentMethod(paymentMethod);
        order.setPaymentMonths(paymentMonths);
        order.setTenantCount(request.tenantCount());
        order.setTenantName(verifiedAuth.getRealName());
        order.setTenantPhone(verifiedAuth.getAccountMobile());
        order.setTenantIdCard(fullIdCard);
        order.setRealNameAt(LocalDateTime.now());
        order.setMonthlyRent(monthlyRent);
        order.setDeposit(deposit);
        order.setServiceFee(serviceFee);
        order.setFirstPaymentAmount(firstPaymentAmount);
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        save(order);

        // 8. 生成合同
        createContract(order, verifiedAuth, fullIdCard);

        return toResponse(order, house);
    }

    /** 从认证记录生成租赁合同 —— 身份证以密文存储 */
    private void createContract(RentOrder order, UserRealNameAuth auth, String fullIdCard) {
        House house = houseService.getById(order.getHouseId());
        String contractNo = generateContractNo();

        // 租户身份证密文（从实名认证记录直接复制，不解密）
        String tenantCiphertext = auth.getIdCardCiphertext();

        // 房东信息统一从房东用户的实名认证记录获取。
        String landlordName = "";
        String landlordPhone = "";
        String landlordCiphertext = "";
        if (order.getLessorUserId() != null) {
            UserRealNameAuth lessorAuth = realNameAuthService.getVerifiedRecord(order.getLessorUserId());
            if (lessorAuth != null) {
                landlordName = lessorAuth.getRealName();
                landlordPhone = lessorAuth.getAccountMobile();
                landlordCiphertext = lessorAuth.getIdCardCiphertext();
            }
        }
        // 兼容旧订单：按 house.landlord_id（即 user.id）补齐房东用户。
        if (landlordName.isBlank() && house != null && house.getLandlordId() != null) {
            UserRealNameAuth lessorAuth = realNameAuthService.getVerifiedRecord(house.getLandlordId());
            if (lessorAuth != null) {
                landlordName = lessorAuth.getRealName();
                landlordPhone = lessorAuth.getAccountMobile();
                landlordCiphertext = lessorAuth.getIdCardCiphertext();
            }
        }

        RentContract contract = new RentContract();
        contract.setId(UUID.randomUUID().toString());
        contract.setOrderId(order.getId());
        contract.setUserId(order.getUserId());
        contract.setHouseId(order.getHouseId());
        contract.setContractNo(contractNo);
        contract.setStatus("draft");
        contract.setTenantName(auth.getRealName());
        contract.setTenantPhone(auth.getAccountMobile());
        contract.setTenantIdCard(""); // 旧明文字段，后续迁移清空
        contract.setTenantIdCardCiphertext(tenantCiphertext);
        contract.setLandlordName(landlordName);
        contract.setLandlordPhone(landlordPhone);
        contract.setLandlordIdCard(""); // 旧明文字段
        contract.setLandlordIdCardCiphertext(landlordCiphertext);
        contract.setIdCardFrontUrl(null);
        contract.setIdCardBackUrl(null);
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
        contract.setHouseAddress(buildContractHouseAddress(house));
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        rentContractMapper.insert(contract);
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
        contract.setTenantIdCard(""); // 旧明文字段
        contract.setTenantIdCardCiphertext(idCardCryptoService.encrypt(request.tenantIdCard()));
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
        contract.setHouseAddress(buildContractHouseAddress(house));
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        rentContractMapper.insert(contract);

        return toResponse(order, house);
    }

    @Override
    public ContractPreviewResponse getContractPreview(String userId, String orderId) {
        RentOrder order = getRelatedOrder(userId, orderId);
        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"),
                false
        );
        if (contract == null) {
            throw BusinessException.notFound("合同尚未生成");
        }

        // 获取房东信息（优先使用合同内的，兼容旧合同从 landlord 表查）
        String landlordName = contract.getLandlordName() != null && !contract.getLandlordName().isBlank()
                ? contract.getLandlordName() : "";
        String landlordPhone = contract.getLandlordPhone() != null
                ? contract.getLandlordPhone() : "";
        String landlordIdCard = "";
        // 优先从密文列解密，兼容旧明文列
        String lessorCiphertext = contract.getLandlordIdCardCiphertext();
        if (lessorCiphertext != null && !lessorCiphertext.isBlank()) {
            try {
                landlordIdCard = idCardCryptoService.mask(idCardCryptoService.decrypt(lessorCiphertext));
            } catch (Exception e) {
                log.warn("合同房东身份证密文解密失败: orderId={}", orderId);
            }
        } else if (contract.getLandlordIdCard() != null && !contract.getLandlordIdCard().isBlank()) {
            try {
                landlordIdCard = idCardCryptoService.mask(idCardCryptoService.decrypt(contract.getLandlordIdCard()));
            } catch (Exception e) {
                log.warn("合同房东身份证解密失败: orderId={}", orderId);
            }
        }
        if (landlordName.isBlank()) {
            String lessorUserId = order.getLessorUserId();
            if (lessorUserId == null || lessorUserId.isBlank()) {
                House house = houseService.getById(contract.getHouseId());
                lessorUserId = getLessorUserId(house);
            }
            UserRealNameAuth lessorAuth = lessorUserId != null
                    ? realNameAuthService.getVerifiedRecord(lessorUserId) : null;
            if (lessorAuth != null) {
                landlordName = lessorAuth.getRealName();
                landlordPhone = lessorAuth.getAccountMobile();
                landlordIdCard = idCardCryptoService.mask(
                        idCardCryptoService.decrypt(lessorAuth.getIdCardCiphertext()));
            }
        }

        // 生成合同条款
        List<String> clauses = buildContractClauses(contract);

        // 租户身份证脱敏（从密文列解密，兼容旧明文列）
        String tenantIdCardMasked = "";
        String tenantCiphertext = contract.getTenantIdCardCiphertext();
        if (tenantCiphertext != null && !tenantCiphertext.isBlank()) {
            try {
                tenantIdCardMasked = idCardCryptoService.mask(idCardCryptoService.decrypt(tenantCiphertext));
            } catch (Exception e) {
                log.warn("合同租户身份证密文解密失败: orderId={}", orderId);
            }
        } else if (contract.getTenantIdCard() != null && !contract.getTenantIdCard().isBlank()) {
            tenantIdCardMasked = idCardCryptoService.mask(contract.getTenantIdCard());
        }

        return new ContractPreviewResponse(
                orderId,
                contract.getContractNo(),
                contract.getStatus(),
                contract.getStatus(),
                contract.getPreviewUrl(),
                contract.getTenantName(),
                contract.getTenantPhone(),
                tenantIdCardMasked,
                contract.getHouseName(),
                contract.getRoomName(),
                contract.getHouseAddress(),
                landlordName,
                landlordPhone,
                landlordIdCard,
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

        // 同步更新合同状态为已确认
        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"), false);
        if (contract != null && "generated".equals(contract.getStatus())) {
            contract.setStatus("confirmed");
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);
        }

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
    public PayResponse pay(String userId, String orderId, PayRequest request) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"pendingPayment".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许支付");
        }

        // 创建支付记录，包含费用明细拆账
        int rentAmount = order.getMonthlyRent() * order.getPaymentMonths();
        int depositAmount = order.getDeposit();
        int serviceFeeAmount = order.getServiceFee();

        String feeBreakdown = buildFeeBreakdown(rentAmount, depositAmount, serviceFeeAmount, order.getPaymentMonths());

        House payHouse = houseService.getById(order.getHouseId());
        String houseName = payHouse != null ? payHouse.getTitle() : "";

        PaymentRecord record = new PaymentRecord();
        record.setId(UUID.randomUUID().toString());
        record.setPaymentNo(paymentRecordService.generatePaymentNo());
        record.setOrderId(orderId);
        record.setUserId(userId);
        record.setHouseId(order.getHouseId());
        record.setHouseName(houseName);
        record.setType("rent");
        record.setAmount(order.getFirstPaymentAmount());
        record.setPaymentChannel(request.paymentChannel());
        record.setStatus("pending");
        record.setFeeBreakdown(feeBreakdown);
        record.setRemark(order.getPaymentMonths() + "期租金及押金服务费");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        paymentRecordService.save(record);

        String channel = request.paymentChannel();
        String payType = null;
        String paymentUrl = null;

        // mock 渠道自动确认支付（开发阶段），真实渠道等待回调确认
        if ("mock".equals(channel)) {
            confirmPayment(record.getId(), null);
        } else if ("alipay".equals(channel)) {
            String subject = "住享租房-" + houseName;
            try {
                paymentUrl = alipayService.buildH5PayUrl(record.getPaymentNo(), record.getAmount(), subject);
                payType = alipayService.getPayType();
                log.info("支付宝下单成功 paymentNo={} paymentUrl={}", record.getPaymentNo(), paymentUrl);
            } catch (Exception e) {
                log.error("支付宝下单失败 paymentNo={}", record.getPaymentNo(), e);
                // 支付记录保留为 pending，不回滚订单（用户可重新发起支付）
            }
        }

        return new PayResponse(
                orderId,
                record.getId(),
                payType,
                paymentUrl,
                order.getStatus(),
                record.getPaymentNo(),
                record.getAmount()
        );
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

        // 支付确认后推进订单状态 → pendingEsign
        RentOrder order = getById(record.getOrderId());
        if (order != null && "pendingPayment".equals(order.getStatus())) {
            order.setStatus("pendingEsign");
            order.setPaidAt(now);
            order.setUpdatedAt(now);
            updateById(order);
        }
    }

    @Override
    @Transactional
    public EsignSignResponse sign(String userId, String orderId) {
        RentOrder order = getRelatedOrder(userId, orderId);

        // 已完成 → 直接返回
        if ("completed".equals(order.getStatus())) {
            return new EsignSignResponse("COMPLETED",
                    userId.equals(order.getUserId()) ? "TENANT" : "LESSOR", true, null);
        }

        if (!"pendingEsign".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许签约");
        }

        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"), false);
        if (contract == null) {
            throw BusinessException.notFound("合同尚未生成");
        }

        // 合同已签署完成
        if ("signed".equals(contract.getStatus())) {
            return new EsignSignResponse("COMPLETED",
                    userId.equals(order.getUserId()) ? "TENANT" : "LESSOR", true, null);
        }

        repairOrderLessorUser(order);
        LeaseContractFillData fillData = buildFillData(order, contract);
        if (fillData == null) {
            throw BusinessException.badRequest("房东或租户尚未完成实名认证");
        }

        // 判断用户角色
        boolean isTenant = userId.equals(order.getUserId());
        if (!isTenant) {
            // 房东签署：校验身份信息
            if (order.getLessorUserId() == null) {
                throw BusinessException.badRequest("房东未关联系统账号，无法签署。请联系管理员在房东管理中绑定账号。");
            }
            if (!realNameAuthService.isVerified(order.getLessorUserId())) {
                throw BusinessException.badRequest("房东尚未完成实名认证");
            }
        }

        AdminContractTemplateService.RuntimeTemplate runtimeTemplate = resolveRuntimeTemplate(contract, fillData);

        // 生成合同文件（幂等）
        if (contract.getContractFileId() == null || contract.getContractFileId().isBlank()) {
            requireEsignConfigured();
            EsignV3Client.CreateFileResponse fileResp = esignV3Client.createByDocTemplateComponents(
                    runtimeTemplate.components(), runtimeTemplate.docTemplateId(), "租房合同.pdf");
            contract.setDocTemplateId(runtimeTemplate.docTemplateId());
            contract.setTemplateConfigId(runtimeTemplate.configId());
            contract.setTemplateVersion(runtimeTemplate.version());
            contract.setTemplateFingerprint(runtimeTemplate.fingerprint());
            contract.setContractFileId(fileResp.getData().getFileId());
            contract.setPreviewUrl(fileResp.getData().getFileDownloadUrl());
            contract.setStatus("generated");
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);
        }

        // 发起签署流程（幂等）
        if (contract.getSignFlowId() == null || contract.getSignFlowId().isBlank()) {
            requireEsignConfigured();
            if (runtimeTemplate.lessorSignature() == null || runtimeTemplate.tenantSignature() == null) {
                throw BusinessException.badRequest("已发布合同模板缺少甲方或乙方签章位置，请管理员重新校验并发布");
            }
            var lp = runtimeTemplate.lessorSignature();
            var tp = runtimeTemplate.tenantSignature();
            EsignV3Client.CreateSignFlowResponse flowResp = esignV3Client.createSignFlow(
                    contract.getContractFileId(), fillData,
                    lp.page(), lp.x(), lp.y(), tp.page(), tp.x(), tp.y());
            contract.setSignFlowId(flowResp.getData().getSignFlowId());
            contract.setStatus("signing");
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);
            // 提交本地合同状态，不与 e签宝 HTTP 共用事务
        }

        // 获取当前用户的签署链接
        String signerPhone = isTenant ? fillData.getTenantMobile() : fillData.getLessorMobile();
        String userRole = isTenant ? "TENANT" : "LESSOR";

        requireEsignConfigured();
        EsignV3Client.SignUrlResponse signUrlResp =
                esignV3Client.getSignUrl(contract.getSignFlowId(), signerPhone);

        String signUrl = signUrlResp.getData().getShortUrl() != null
                ? signUrlResp.getData().getShortUrl()
                : signUrlResp.getData().getUrl();

        // 检查当前用户是否已签
        boolean currentUserSigned = isTenant
                ? (contract.getTenantSigned() != null && contract.getTenantSigned() == 1)
                : (contract.getLessorSigned() != null && contract.getLessorSigned() == 1);

        return new EsignSignResponse("SIGNING", userRole, currentUserSigned, signUrl);
    }

    private AdminContractTemplateService.RuntimeTemplate resolveRuntimeTemplate(
            RentContract contract, LeaseContractFillData fillData) {
        if (adminContractTemplateService == null) {
            throw BusinessException.badRequest("合同模板管理服务未启用，无法发起签署");
        }
        if (contract.getTemplateConfigId() != null && !contract.getTemplateConfigId().isBlank()) {
            return adminContractTemplateService.resolveRuntimeTemplate(contract.getTemplateConfigId(), fillData);
        }
        return adminContractTemplateService.resolveActiveRuntimeTemplate(fillData);
    }

    // ==================== e签宝回调处理 ====================

    @Override
    @Transactional
    public void processEsignCallback(EsignCallbackData callback) {
        // 根据 signFlowId 查找合同（利用唯一约束保证精准匹配）
        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getSignFlowId, callback.getSignFlowId())
                        .last("LIMIT 1"), false);
        if (contract == null) {
            log.warn("e签宝回调：未找到对应合同 signFlowId={}", callback.getSignFlowId());
            return;
        }

        Integer status = callback.getSignFlowStatus();
        if (status == null) return;

        // 已完成回调允许重复进入，以便修复历史上 status=signed 但双方签署标记未落库的数据。
        // 其他迟到的回调不能把已签署合同降级成撤销、拒签或过期。
        if ("signed".equals(contract.getStatus()) && status != 2) {
            log.info("e签宝回调：合同已签署，忽略非完成状态 signFlowId={}, status={}",
                    callback.getSignFlowId(), status);
            return;
        }

        log.info("e签宝回调处理：signFlowId={}, status={}, contractNum={}",
                callback.getSignFlowId(), status, callback.getContractNum());

        if (status == 2) {
            // 签署完成
            contract.setStatus("signed");
            if (callback.getContractNum() != null) {
                contract.setContractNum(callback.getContractNum());
            }
            contract.setLessorSigned(1);
            contract.setTenantSigned(1);
            LocalDateTime finishTime = EsignCallbackData.toLocalDateTime(callback.getSignFlowFinishTime());
            if (finishTime != null) {
                contract.setSignedAt(finishTime);
            } else if (contract.getSignedAt() == null) {
                contract.setSignedAt(LocalDateTime.now());
            }
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);

            // 查找订单并完成
            RentOrder order = getById(contract.getOrderId());
            if (order != null && !"completed".equals(order.getStatus())) {
                completeOrderAndCreateLease(order, contract);
            }
        } else if (status == 3 || status == 5) {
            // 撤销(3) / 拒签(5)
            contract.setStatus("canceled");
            contract.setFailureCode("ESIGN_" + status);
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);
        } else if (status == 4) {
            // 过期
            contract.setStatus("expired");
            contract.setFailureCode("ESIGN_EXPIRED");
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);
        } else {
            // status == 0 或 1（草稿/签署中）：更新签署人状态
            // 回调可能只包含部分签署人信息，保守更新
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);
        }
    }

    private void requireEsignConfigured() {
        if (!esignV3Properties.isCredentialsConfigured()) {
            throw BusinessException.badRequest("e签宝电子合同未配置：请设置 ESIGN_APP_ID 和 ESIGN_APP_SECRET 环境变量");
        }
    }

    // ==================== 合同状态刷新 ====================

    @Transactional
    public EsignSignStatusResponse contractRefresh(String userId, String orderId) {
        RentOrder order = getRelatedOrder(userId, orderId);
        boolean isTenant = userId.equals(order.getUserId());

        // 已完成 → 直接返回本地状态，不调 e签宝
        if ("completed".equals(order.getStatus())) {
            return new EsignSignStatusResponse("COMPLETED", true, true, true, true, order.getSignedAt());
        }

        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"), false);
        if (contract == null || contract.getSignFlowId() == null) {
            throw BusinessException.notFound("合同签署流程不存在");
        }

        // 本地已签完 → 不重复调 e签宝，同时自愈历史异常签署标记
        if ("signed".equals(contract.getStatus())) {
            if (!Integer.valueOf(1).equals(contract.getLessorSigned())
                    || !Integer.valueOf(1).equals(contract.getTenantSigned())) {
                contract.setLessorSigned(1);
                contract.setTenantSigned(1);
                contract.setUpdatedAt(LocalDateTime.now());
                rentContractMapper.updateById(contract);
            }
            return new EsignSignStatusResponse("COMPLETED",
                    true, true, true,
                    true, contract.getSignedAt());
        }

        EsignV3Client.SignFlowDetailResponse detail =
                esignV3Client.getSignFlowDetail(contract.getSignFlowId());

        boolean lessorSigned = false;
        boolean tenantSigned = false;
        if (detail.getData() != null && detail.getData().getSigners() != null) {
            for (var s : detail.getData().getSigners()) {
                boolean signed = s.getSignStatus() == 1;
                if ("甲方".equals(s.getSignerRole())
                        || signerAccountMatches(s.getPsnAccount(), contract.getLandlordPhone())) {
                    lessorSigned = lessorSigned || signed;
                }
                if ("乙方".equals(s.getSignerRole())
                        || signerAccountMatches(s.getPsnAccount(), contract.getTenantPhone())) {
                    tenantSigned = tenantSigned || signed;
                }
            }
        }

        // 流程完成是双方均已签署的权威状态，不依赖签署人列表是否返回角色字段。
        boolean completed = detail.getData() != null && detail.getData().getSignFlowStatus() == 2;
        if (completed) {
            lessorSigned = true;
            tenantSigned = true;
        }
        contract.setLessorSigned(lessorSigned ? 1 : 0);
        contract.setTenantSigned(tenantSigned ? 1 : 0);
        contract.setUpdatedAt(LocalDateTime.now());

        // 双方签完 → 完成签约
        if (completed) {
            contract.setStatus("signed");
            if (detail.getData().getContractNum() != null) {
                contract.setContractNum(detail.getData().getContractNum());
            }
            // 签署完成时间以 e签宝返回为准
            LocalDateTime finishTime = detail.getData().getSignFlowFinishTime() != null
                    ? Instant.ofEpochMilli(detail.getData().getSignFlowFinishTime())
                              .atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime()
                    : LocalDateTime.now();
            contract.setSignedAt(finishTime);
            rentContractMapper.updateById(contract);

            // 创建租约（带 FOR UPDATE 防并发）
            completeOrderAndCreateLease(order, contract);
        } else {
            rentContractMapper.updateById(contract);
        }

        return new EsignSignStatusResponse(
                completed ? "COMPLETED" : "SIGNING",
                lessorSigned, tenantSigned,
                isTenant ? tenantSigned : lessorSigned,
                completed, completed ? contract.getSignedAt() : null);
    }

    private boolean signerAccountMatches(String signerAccount, String contractPhone) {
        if (signerAccount == null || contractPhone == null) return false;
        String normalizedSigner = signerAccount.replaceAll("[\\s-]", "");
        String normalizedPhone = contractPhone.replaceAll("[\\s-]", "");
        if (normalizedSigner.startsWith("+86")) normalizedSigner = normalizedSigner.substring(3);
        if (normalizedPhone.startsWith("+86")) normalizedPhone = normalizedPhone.substring(3);
        return !normalizedSigner.isBlank() && normalizedSigner.equalsIgnoreCase(normalizedPhone);
    }

    // ==================== 获取已签合同下载链接 ====================

    public ContractDownloadUrlResponse contractDownloadUrl(String userId, String orderId) {
        RentOrder order = getRelatedOrder(userId, orderId);

        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getOrderId, orderId)
                        .last("LIMIT 1"), false);
        if (contract == null || contract.getSignFlowId() == null) {
            throw BusinessException.notFound("合同签署流程不存在");
        }
        if (!"signed".equals(contract.getStatus())) {
            throw EsignException.notSigned();
        }

        EsignV3Client.FileDownloadResponse resp =
                esignV3Client.getFileDownloadUrl(contract.getSignFlowId());

        String downloadUrl = null;
        String fileName = "租房合同.pdf";
        if (resp.getData() != null && resp.getData().getFiles() != null
                && !resp.getData().getFiles().isEmpty()) {
            EsignV3Client.FileDownloadResponse.FileItem f = resp.getData().getFiles().get(0);
            downloadUrl = f.getDownloadUrl();
            fileName = f.getFileName() != null ? f.getFileName() : fileName;
        }

        return new ContractDownloadUrlResponse(fileName, downloadUrl,
                resp.getData() != null ? resp.getData().getCertificateDownloadUrl() : null);
    }

    // ==================== 租约创建（双方签完后） ====================

    @Transactional
    public void completeOrderAndCreateLease(RentOrder order, RentContract contract) {
        // 用 FOR UPDATE 重新加载订单，防止并发重复创建租约
        RentOrder lockedOrder = getBaseMapper().selectOne(
                Wrappers.<RentOrder>lambdaQuery()
                        .eq(RentOrder::getId, order.getId())
                        .last("LIMIT 1 FOR UPDATE"));
        if (lockedOrder == null || "completed".equals(lockedOrder.getStatus())) return; // 幂等

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        String leaseStatus = lockedOrder.getStartDate().isAfter(today) ? "pending" : "active";

        Lease lease = new Lease();
        lease.setId(UUID.randomUUID().toString());
        lease.setUserId(lockedOrder.getUserId());
        lease.setHouseId(lockedOrder.getHouseId());
        lease.setStatus(leaseStatus);
        lease.setStartDate(lockedOrder.getStartDate());
        lease.setEndDate(lockedOrder.getEndDate());
        lease.setLeaseMonths(lockedOrder.getLeaseMonths());
        lease.setPaymentMethod(lockedOrder.getPaymentMethod());
        lease.setPaymentMonths(lockedOrder.getPaymentMonths());
        lease.setMonthlyRent(lockedOrder.getMonthlyRent());
        lease.setDeposit(lockedOrder.getDeposit());
        lease.setServiceFee(lockedOrder.getServiceFee());
        lease.setFirstPaymentAmount(lockedOrder.getFirstPaymentAmount());
        lease.setContractId(contract.getId());
        lease.setOrderId(lockedOrder.getId());
        lease.setCreatedAt(now);
        lease.setUpdatedAt(now);
        leaseService.save(lease);

        PaymentRecord paymentRecord = paymentRecordService.getOne(
                Wrappers.<PaymentRecord>lambdaQuery()
                        .eq(PaymentRecord::getOrderId, lockedOrder.getId())
                        .eq(PaymentRecord::getType, "rent")
                        .eq(PaymentRecord::getStatus, "success")
                        .last("LIMIT 1"), false);

        DepositRecord depositRecord = new DepositRecord();
        depositRecord.setLeaseId(lease.getId());
        depositRecord.setUserId(lockedOrder.getUserId());
        depositRecord.setHouseId(lockedOrder.getHouseId());
        depositRecord.setAmount(lockedOrder.getDeposit());
        depositRecord.setPaymentRecordId(paymentRecord != null ? paymentRecord.getId() : null);
        depositService.createDeposit(depositRecord);

        // 创建验收快照（非致命：模板不存在时不影响签约）
        try {
            inspectionService.createSnapshotFromTemplate(
                    contract.getId(), lease.getId(), lockedOrder.getHouseId());
        } catch (Exception e) {
            log.warn("创建验房快照失败，签约不受影响: contractId={}, err={}",
                    contract.getId(), e.getMessage());
        }

        generateRentBills(lease, lockedOrder);

        lockedOrder.setStatus("completed");
        lockedOrder.setSignedAt(now);
        lockedOrder.setUpdatedAt(now);
        updateById(lockedOrder);

        House house = houseService.getById(lockedOrder.getHouseId());
        if (house != null) {
            house.setStatus("rented");
            house.setUpdatedAt(now);
            houseService.updateById(house);
        }
        if ("active".equals(leaseStatus)) {
            eventPublisher.publishEvent(new LeaseActivatedEvent(lease.getId()));
        }
        log.info("eSign签约完成，租约已创建: orderId={}, leaseId={}", lockedOrder.getId(), lease.getId());
    }

    // ==================== 构建合同填充数据 ====================

    private LeaseContractFillData buildFillData(RentOrder order, RentContract contract) {
        // 租户：从实名认证记录获取
        UserRealNameAuth tenantAuth = realNameAuthService.getVerifiedRecord(order.getUserId());
        if (tenantAuth == null) return null;
        String tenantIdCard = idCardCryptoService.decrypt(tenantAuth.getIdCardCiphertext());

        // 房东：必须绑定用户并完成实名认证，不允许 Landlord 表兜底
        if (order.getLessorUserId() == null) {
            log.warn("订单 {} 未关联房东用户ID，无法生成合同填充数据", order.getId());
            return null;
        }
        UserRealNameAuth lessorAuth = realNameAuthService.getVerifiedRecord(order.getLessorUserId());
        if (lessorAuth == null) return null; // 房东未完成实名认证
        String lessorIdCard = idCardCryptoService.decrypt(lessorAuth.getIdCardCiphertext());

        // 兼容修复前生成的空白合同，签约前用房东实名记录补齐合同快照。
        boolean contractChanged = false;
        if (contract.getLandlordName() == null || contract.getLandlordName().isBlank()) {
            contract.setLandlordName(lessorAuth.getRealName());
            contractChanged = true;
        }
        if (contract.getLandlordPhone() == null || contract.getLandlordPhone().isBlank()) {
            contract.setLandlordPhone(lessorAuth.getAccountMobile());
            contractChanged = true;
        }
        if (contract.getLandlordIdCardCiphertext() == null
                || contract.getLandlordIdCardCiphertext().isBlank()) {
            contract.setLandlordIdCardCiphertext(lessorAuth.getIdCardCiphertext());
            contractChanged = true;
        }
        // 合同地址统一使用“小区名称 + 楼栋 + 单元 + 房号”。
        House house = houseService.getById(order.getHouseId());
        String addr = buildContractHouseAddress(house);
        if (!Objects.equals(contract.getHouseAddress(), addr)) {
            contract.setHouseAddress(addr);
            contractChanged = true;
        }
        if (contractChanged) {
            contract.setUpdatedAt(LocalDateTime.now());
            rentContractMapper.updateById(contract);
        }

        return LeaseContractFillData.builder()
                .lessorName(lessorAuth.getRealName())
                .lessorMobile(lessorAuth.getAccountMobile())
                .lessorIdCard(lessorIdCard)
                .tenantName(tenantAuth.getRealName())
                .tenantMobile(tenantAuth.getAccountMobile())
                .tenantIdCard(tenantIdCard)
                .houseAddress(addr)
                .leaseMonths(order.getLeaseMonths() != null ? order.getLeaseMonths() : 1)  // 直接传月数
                .leaseStartDate(order.getStartDate())
                .leaseEndDate(order.getEndDate())
                .noticeMonths(1)
                .deposit(new java.math.BigDecimal(order.getDeposit()))
                .monthlyRent(new java.math.BigDecimal(order.getMonthlyRent()))
                .rentPaymentDate(order.getStartDate())
                .lessorSignDate(null)   // 不预填，签署时由签署人填写
                .tenantSignDate(null)   // 不预填
                .build();
    }

    private String buildContractHouseAddress(House house) {
        if (house == null) return "";
        Community community = house.getCommunityId() == null ? null : communityService.getById(house.getCommunityId());
        if (community == null || !org.springframework.util.StringUtils.hasText(community.getName())) {
            throw BusinessException.badRequest("房源未关联有效小区，无法生成合同地址");
        }
        String communityName = compactAddress(community.getName());
        String building = trimAddressLabel(house.getBuilding(), "栋");
        String unit = trimAddressLabel(house.getUnit(), "单元");
        String room = trimAddressLabel(trimAddressLabel(house.getRoom(), "室"), "号");
        String labeledTail = (building.isEmpty() ? "" : building + "栋")
                + (unit.isEmpty() ? "" : unit + "单元") + room;
        return labeledTail.isEmpty() ? communityName : communityName + " " + labeledTail;
    }

    private String compactAddress(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }

    private String trimAddressLabel(String value, String label) {
        String normalized = compactAddress(value);
        return normalized.endsWith(label) ? normalized.substring(0, normalized.length() - label.length()) : normalized;
    }

    private String getUserVerifiedPhone(String userId) {
        UserRealNameAuth auth = realNameAuthService.getVerifiedRecord(userId);
        return auth != null ? auth.getAccountMobile() : null;
    }

    // ==================== 签约相关方法 ====================

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
                // 未来账单预生成但不激活，由定时任务在到期时激活为 pending
                bill.setStatus("scheduled");
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

    /** 允许租户或房东访问订单（签署和查看合同相关操作） */
    private RentOrder getRelatedOrder(String userId, String orderId) {
        RentOrder order = getById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (userId.equals(order.getUserId())) {
            return order;
        }
        if (order.getLessorUserId() != null && userId.equals(order.getLessorUserId())) {
            return order;
        }
        throw BusinessException.forbidden("无权操作该订单");
    }

    /** house.landlord_id 直接关联 user.id，并要求该用户为有效房东。 */
    private String getLessorUserId(House house) {
        if (house == null || house.getLandlordId() == null) return null;
        User landlordUser = userService.getById(house.getLandlordId());
        if (landlordUser == null || landlordUser.getRole() == null
                || !"LANDLORD".equalsIgnoreCase(landlordUser.getRole())) {
            return null;
        }
        return landlordUser.getId();
    }

    /** 修复旧订单中缺失或仍指向旧 landlord.id 的房东用户关联。 */
    private void repairOrderLessorUser(RentOrder order) {
        House house = houseService.getById(order.getHouseId());
        String lessorUserId = getLessorUserId(house);
        if (lessorUserId != null && !lessorUserId.equals(order.getLessorUserId())) {
            order.setLessorUserId(lessorUserId);
            order.setUpdatedAt(LocalDateTime.now());
            updateById(order);
        }
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
                idCardCryptoService.mask(order.getTenantIdCard()),
                order.getRealNameAt(), order.getContractConfirmedAt(),
                order.getPaidAt(), order.getSignedAt(),
                order.getCancelledAt(),
                order.getCreatedAt(), order.getUpdatedAt(),
                houseName, roomName, address
        );
    }

    private static String formatRoomName(House house) {
        return (house.getBuilding() != null ? house.getBuilding() + "栋" : "")
                + (house.getUnit() != null ? house.getUnit() + "单元" : "")
                + (house.getRoom() != null ? house.getRoom() : "");
    }

    private static String generateContractNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(10000);
        return "CT" + timestamp + String.format("%04d", random);
    }
}
