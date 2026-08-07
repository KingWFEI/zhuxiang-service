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
    private static final int PAYMENT_WINDOW_MINUTES = 15;
    private static final int PRE_PAYMENT_STAGE_MINUTES = 5;
    private static final String PAYMENT_TIMEOUT_REASON = "PAYMENT_TIMEOUT";

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
                                "pendingContract", "pendingTenantSign", "pendingPayment",
                                "pendingLandlordSign", "completed")
                        .last("LIMIT 1 FOR UPDATE"));

        if (existing != null) {
            String status = existing.getStatus();

            // completed → 不允许再申请
            if ("completed".equals(status)) {
                throw BusinessException.conflict("该房源已完成租住");
            }

            // 合同确认、租客签署、支付、房东签署阶段的进行中订单直接返回
            if ("pendingContract".equals(status) || "pendingTenantSign".equals(status)
                    || "pendingPayment".equals(status) || "pendingLandlordSign".equals(status)) {
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
            existing.setPrePaymentDeadlineAt(LocalDateTime.now().plusMinutes(PRE_PAYMENT_STAGE_MINUTES));
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

        // 6. 合同草稿阶段只校验房源可租，不锁房。
        // 真正锁房移动到租客点击“去签署”的 sign() 中。
        LocalDateTime now = LocalDateTime.now();
        House house = houseService.getById(request.houseId());
        if (house == null) throw BusinessException.notFound("房源不存在");
        if (!"available".equals(house.getStatus())) {
            throw BusinessException.conflict("该房源正在被其他租客办理或已出租");
        }

        long activeLeaseCount = leaseService.count(Wrappers.<Lease>lambdaQuery()
                .eq(Lease::getHouseId, request.houseId())
                .eq(Lease::getStatus, "active"));
        if (activeLeaseCount > 0) {
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
        order.setPrePaymentDeadlineAt(now.plusMinutes(PRE_PAYMENT_STAGE_MINUTES));
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
        order.setPrePaymentDeadlineAt(LocalDateTime.now().plusMinutes(PRE_PAYMENT_STAGE_MINUTES));
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
    public RentOrderResponse confirmContract(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);

        if (!"pendingContract".equals(order.getStatus())
                && !"pendingTenantSign".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许确认合同");
        }
        // 这里只做状态校验，不修改订单或合同状态。
        // 真正调用 e签宝并成功取得签署链接后，才由 sign() 推进订单状态。
        House house = houseService.getById(order.getHouseId());
        return toResponse(order, house);
    }

    @Override
    public PaymentInfoResponse getPaymentInfo(String userId, String orderId) {
        RentOrder order = getOwnedOrder(userId, orderId);
        validatePaymentWindow(order, LocalDateTime.now());

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
        RentOrder order = getOwnedOrderForUpdate(userId, orderId);
        validatePaymentWindow(order, LocalDateTime.now());

        // 创建支付记录，包含费用明细拆账
        int rentAmount = order.getMonthlyRent() * order.getPaymentMonths();
        int depositAmount = order.getDeposit();
        int serviceFeeAmount = order.getServiceFee();

        String feeBreakdown = buildFeeBreakdown(rentAmount, depositAmount, serviceFeeAmount, order.getPaymentMonths());

        House payHouse = houseService.getById(order.getHouseId());
        String houseName = payHouse != null ? payHouse.getTitle() : "";
        String channel = request.paymentChannel();

        // 支付宝沙箱页面可能加载较慢。重复点击时复用同一支付编号，避免产生多笔待支付记录。
        if ("alipay".equals(channel)) {
            PaymentRecord existingRecord = paymentRecordService.getOne(
                    Wrappers.<PaymentRecord>lambdaQuery()
                            .eq(PaymentRecord::getOrderId, orderId)
                            .eq(PaymentRecord::getUserId, userId)
                            .eq(PaymentRecord::getPaymentChannel, "alipay")
                            .eq(PaymentRecord::getStatus, "pending")
                            .orderByDesc(PaymentRecord::getCreatedAt)
                            .last("LIMIT 1 FOR UPDATE"), false);
            if (existingRecord != null) {
                String subject = "勿忧管家租房-" + houseName;
                String existingPaymentUrl = alipayService.buildH5PayUrl(
                        existingRecord.getPaymentNo(), existingRecord.getAmount(), subject);
                log.info("复用支付宝待支付单 paymentNo={}", existingRecord.getPaymentNo());
                return new PayResponse(
                        orderId, existingRecord.getId(), alipayService.getPayType(),
                        existingPaymentUrl, order.getStatus(), existingRecord.getPaymentNo(),
                        existingRecord.getAmount());
            }
        }

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

        String payType = null;
        String paymentUrl = null;

        // mock 渠道自动确认支付（开发阶段），真实渠道等待回调确认
        if ("mock".equals(channel)) {
            confirmPayment(record.getId(), null);
        } else if ("alipay".equals(channel)) {
            String subject = "勿忧管家租房-" + houseName;
            try {
                paymentUrl = alipayService.buildH5PayUrl(record.getPaymentNo(), record.getAmount(), subject);
                payType = alipayService.getPayType();
                log.info("支付宝下单成功 paymentNo={}, payType={}", record.getPaymentNo(), payType);
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
        PaymentRecord recordSnapshot = paymentRecordService.getById(recordId);
        if (recordSnapshot == null) {
            throw BusinessException.badRequest("支付记录不存在");
        }

        // 先锁订单，再锁支付记录。支付回调、主动查询和超时任务都通过同一订单行串行化。
        RentOrder order = getBaseMapper().selectByIdForUpdate(recordSnapshot.getOrderId());
        if (order == null) {
            throw BusinessException.notFound("租房订单不存在");
        }
        PaymentRecord record = paymentRecordService.getOne(
                Wrappers.<PaymentRecord>lambdaQuery()
                        .eq(PaymentRecord::getId, recordId)
                        .last("LIMIT 1 FOR UPDATE"), false);
        if (record == null) {
            throw BusinessException.badRequest("支付记录不存在");
        }
        if ("success".equals(record.getStatus()) && "completed".equals(order.getStatus())) {
            return;
        }
        if (!"pending".equals(record.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String tradeNo = channelTradeNo != null ? channelTradeNo : "mock_" + UUID.randomUUID().toString().replace("-", "");

        // 支付网关可能在本地超时任务之后才送达成功通知：登记为待退款/人工处理，绝不创建租约。
        if ("paymentExpired".equals(order.getStatus()) || "cancelled".equals(order.getStatus())
                || "completed".equals(order.getStatus())
                || !"pendingPayment".equals(order.getStatus())
                || order.getPaymentDeadlineAt() == null
                || !now.isBefore(order.getPaymentDeadlineAt())) {
            record.setStatus("refundPending");
            record.setChannelTradeNo(tradeNo);
            record.setCallbackTime(now);
            record.setUpdatedAt(now);
            record.setRemark("支付回调到达时订单已不可支付，需退款或人工处理");
            paymentRecordService.updateById(record);
            log.warn("支付成功通知晚于可支付窗口，已转退款/人工处理: orderId={}, recordId={}, status={}",
                    order.getId(), recordId, order.getStatus());
            return;
        }

        record.setStatus("success");
        record.setChannelTradeNo(tradeNo);
        record.setPaidAt(now);
        record.setCallbackTime(now);
        record.setUpdatedAt(now);
        paymentRecordService.updateById(record);

        order.setPaidAt(now);
        order.setUpdatedAt(now);
        updateById(order);

        RentContract contract = rentContractMapper.selectByOrderIdForUpdate(order.getId());
        if (contract == null || !Integer.valueOf(1).equals(contract.getTenantSigned())) {
            throw BusinessException.badRequest("租客尚未完成合同签署，暂不能确认支付");
        }

        // 支付完成后才开放房东签署，不提前创建租约。
        order.setStatus("pendingLandlordSign");
        order.setPrePaymentDeadlineAt(null);
        order.setUpdatedAt(LocalDateTime.now());
        updateById(order);
        updateHouseReservationDeadline(order, null, now);

        // 极端情况下若房东已通过 e签宝其他入口签完，则在支付事务内直接完成，仍确保先支付后生效。
        if ("signed".equals(contract.getStatus())) {
            completeOrderAndCreateLease(order, contract);
        }
    }

    @Override
    public EsignSignResponse sign(String userId, String orderId) {
        RentOrder order = getRelatedOrder(userId, orderId);

        // 已完成 → 直接返回
        if ("completed".equals(order.getStatus())) {
            return new EsignSignResponse("COMPLETED",
                    userId.equals(order.getUserId()) ? "TENANT" : "LESSOR", true, null);
        }

        boolean isTenant = userId.equals(order.getUserId());
        if (isTenant && !"pendingContract".equals(order.getStatus())
                && !"pendingTenantSign".equals(order.getStatus())) {
            throw BusinessException.badRequest("当前订单状态不允许租客签约");
        }
        if (!isTenant && !"pendingLandlordSign".equals(order.getStatus())) {
            throw BusinessException.badRequest("支付完成后才允许房东签约");
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
            completeAfterLandlordSignature(contract);
            return new EsignSignResponse("COMPLETED",
                    userId.equals(order.getUserId()) ? "TENANT" : "LESSOR", true, null);
        }

        repairOrderLessorUser(order);
        LeaseContractFillData fillData = buildFillData(order, contract);
        if (fillData == null) {
            throw BusinessException.badRequest("房东或租户尚未完成实名认证");
        }

        // 判断用户角色
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

        boolean acquiredReservation = false;
        if (isTenant) {
            acquiredReservation = ensureTenantSigningReservation(order);
        }

        try {

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

        // 只有 e签宝成功返回当前签署链接后，才推进租客订单状态。
        // 如果前面的 e签宝调用失败，订单仍保持 pendingContract，可安全重试。
        if (isTenant && "pendingContract".equals(order.getStatus())) {
            order.setStatus("pendingTenantSign");
            order.setContractConfirmedAt(order.getContractConfirmedAt() == null
                    ? LocalDateTime.now() : order.getContractConfirmedAt());
            order.setPrePaymentDeadlineAt(LocalDateTime.now().plusMinutes(PRE_PAYMENT_STAGE_MINUTES));
            order.setUpdatedAt(LocalDateTime.now());
            updateById(order);
        }

        return new EsignSignResponse("SIGNING", userRole, currentUserSigned, signUrl);
        } catch (RuntimeException ex) {
            if (acquiredReservation) {
                releaseHouseReservation(order, LocalDateTime.now());
            }
            throw ex;
        }
    }

    /**
     * 租客点击“去签署”时才原子锁房。
     * 返回 true 表示本次新获得锁；返回 false 表示该订单此前已经持有锁。
     */
    private boolean ensureTenantSigningReservation(RentOrder order) {
        LocalDateTime now = LocalDateTime.now();
        if (order.getPrePaymentDeadlineAt() != null
                && !now.isBefore(order.getPrePaymentDeadlineAt())) {
            throw BusinessException.badRequest("合同办理已超时，请重新发起租赁申请");
        }

        House house = houseService.getById(order.getHouseId());
        if (house == null) throw BusinessException.notFound("房源不存在");
        if ("reserved".equals(house.getStatus())
                && order.getId().equals(house.getReservedOrderId())) {
            return false;
        }

        LocalDateTime reservedUntil = now.plusMinutes(PRE_PAYMENT_STAGE_MINUTES);
        boolean locked = houseService.lambdaUpdate()
                .eq(House::getId, order.getHouseId())
                .eq(House::getStatus, "available")
                .set(House::getStatus, "reserved")
                .set(House::getReservedOrderId, order.getId())
                .set(House::getReservedUntil, reservedUntil)
                .set(House::getUpdatedAt, now)
                .update();
        if (!locked) {
            House latest = houseService.getById(order.getHouseId());
            if (latest != null && "reserved".equals(latest.getStatus())
                    && order.getId().equals(latest.getReservedOrderId())) {
                return false;
            }
            throw BusinessException.conflict("该房源正在被其他租客签署或支付，请稍后再试");
        }

        try {
            order.setPrePaymentDeadlineAt(reservedUntil);
            order.setUpdatedAt(now);
            updateById(order);
            return true;
        } catch (RuntimeException ex) {
            releaseHouseReservation(order, LocalDateTime.now());
            throw ex;
        }
    }

    private void releaseHouseReservation(RentOrder order, LocalDateTime now) {
        houseService.lambdaUpdate()
                .eq(House::getId, order.getHouseId())
                .eq(House::getStatus, "reserved")
                .eq(House::getReservedOrderId, order.getId())
                .set(House::getStatus, "available")
                .set(House::getReservedOrderId, null)
                .set(House::getReservedUntil, null)
                .set(House::getUpdatedAt, now)
                .update();
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

        // 与支付回调保持相同锁顺序：先锁订单，再锁合同，避免支付与签约回调互相死锁。
        RentOrder lockedOrder = getBaseMapper().selectByIdForUpdate(contract.getOrderId());
        if (lockedOrder == null) return;
        RentContract lockedContract = rentContractMapper.selectByOrderIdForUpdate(contract.getOrderId());
        if (lockedContract == null) return;
        contract = lockedContract;

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

            // 双方签署完成；只有订单已支付并进入待房东签署状态时才创建租约。
            markTenantSignedAndOpenPaymentWindow(contract.getOrderId());
            completeAfterLandlordSignature(contract);
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
            // status == 0 或 1 时查询签署人明细；租客签完即开启 15 分钟支付窗口。
            applySignFlowDetail(contract, esignV3Client.getSignFlowDetail(contract.getSignFlowId()));
        }
    }

    private void requireEsignConfigured() {
        if (!esignV3Properties.isCredentialsConfigured()) {
            throw BusinessException.badRequest("e签宝电子合同未配置：请设置 ESIGN_APP_ID 和 ESIGN_APP_SECRET 环境变量");
        }
    }

    private void markTenantSignedAndOpenPaymentWindow(String orderId) {
        RentOrder lockedOrder = getBaseMapper().selectByIdForUpdate(orderId);
        if (lockedOrder == null || "completed".equals(lockedOrder.getStatus())
                || "pendingLandlordSign".equals(lockedOrder.getStatus())
                || "paymentExpired".equals(lockedOrder.getStatus())
                || "cancelled".equals(lockedOrder.getStatus())) {
            return;
        }
        if (!"pendingTenantSign".equals(lockedOrder.getStatus())
                && !"pendingPayment".equals(lockedOrder.getStatus())) {
            log.warn("租客签署完成但订单状态无法进入支付阶段: orderId={}, status={}",
                    orderId, lockedOrder.getStatus());
            return;
        }

        // 重复回调不得顺延支付截止时间。
        LocalDateTime now = LocalDateTime.now();
        if (lockedOrder.getPaidAt() != null) {
            // 兼容旧流程中已经支付、尚未完成双方签署的存量订单。
            lockedOrder.setStatus("pendingLandlordSign");
            lockedOrder.setPrePaymentDeadlineAt(null);
            lockedOrder.setUpdatedAt(now);
            updateById(lockedOrder);
            updateHouseReservationDeadline(lockedOrder, null, now);
            log.info("租客已签署且订单已有支付记录，进入房东签署阶段: orderId={}", orderId);
            return;
        }
        lockedOrder.setStatus("pendingPayment");
        lockedOrder.setPrePaymentDeadlineAt(null);
        if (lockedOrder.getPaymentDeadlineAt() == null) {
            lockedOrder.setPaymentDeadlineAt(now.plusMinutes(PAYMENT_WINDOW_MINUTES));
        }
        lockedOrder.setUpdatedAt(now);
        updateById(lockedOrder);
        updateHouseReservationDeadline(lockedOrder, lockedOrder.getPaymentDeadlineAt(), now);
        log.info("租客已签署，订单进入支付阶段: orderId={}, paymentDeadlineAt={}",
                orderId, lockedOrder.getPaymentDeadlineAt());
    }

    private void updateHouseReservationDeadline(RentOrder order,
                                                LocalDateTime reservedUntil,
                                                LocalDateTime now) {
        houseService.lambdaUpdate()
                .eq(House::getId, order.getHouseId())
                .eq(House::getStatus, "reserved")
                .eq(House::getReservedOrderId, order.getId())
                .set(House::getReservedUntil, reservedUntil)
                .set(House::getUpdatedAt, now)
                .update();
    }

    private SignProgress applySignFlowDetail(RentContract contract,
                                             EsignV3Client.SignFlowDetailResponse detail) {
        boolean lessorSigned = Integer.valueOf(1).equals(contract.getLessorSigned());
        boolean tenantSigned = Integer.valueOf(1).equals(contract.getTenantSigned());
        if (detail.getData() != null && detail.getData().getSigners() != null) {
            for (var signer : detail.getData().getSigners()) {
                // e签宝 V3：1=待签署，2=已签署。不能把待签署误判为已签署。
                boolean signed = signer.getSignStatus() == 2;
                if ("甲方".equals(signer.getSignerRole())
                        || signer.getSignOrder() == 2
                        || signerAccountMatches(signer.resolvedPsnAccount(), contract.getLandlordPhone())) {
                    lessorSigned = lessorSigned || signed;
                }
                if ("乙方".equals(signer.getSignerRole())
                        || signer.getSignOrder() == 1
                        || signerAccountMatches(signer.resolvedPsnAccount(), contract.getTenantPhone())) {
                    tenantSigned = tenantSigned || signed;
                }
            }
        }
        boolean completed = detail.getData() != null && detail.getData().getSignFlowStatus() == 2;
        if (completed) {
            lessorSigned = true;
            tenantSigned = true;
            contract.setStatus("signed");
            contract.setContractNum(detail.getData().getContractNum());
            LocalDateTime finishTime = detail.getData().getSignFlowFinishTime() == null
                    ? LocalDateTime.now()
                    : Instant.ofEpochMilli(detail.getData().getSignFlowFinishTime())
                            .atZone(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
            contract.setSignedAt(finishTime);
        }
        contract.setLessorSigned(lessorSigned ? 1 : 0);
        contract.setTenantSigned(tenantSigned ? 1 : 0);
        contract.setUpdatedAt(LocalDateTime.now());
        rentContractMapper.updateById(contract);
        if (tenantSigned) {
            markTenantSignedAndOpenPaymentWindow(contract.getOrderId());
        }
        if (completed) {
            completeAfterLandlordSignature(contract);
        }
        return new SignProgress(lessorSigned, tenantSigned, completed);
    }

    private void completeAfterLandlordSignature(RentContract contract) {
        RentOrder order = getById(contract.getOrderId());
        if (order == null) return;
        if ("pendingLandlordSign".equals(order.getStatus()) && order.getPaidAt() != null) {
            completeOrderAndCreateLease(order, contract);
        } else if (!"completed".equals(order.getStatus())) {
            log.warn("双方签署完成但订单尚未支付，不创建租约: orderId={}, status={}",
                    order.getId(), order.getStatus());
        }
    }

    private record SignProgress(boolean lessorSigned, boolean tenantSigned, boolean completed) {}

    @Override
    @Transactional
    public void processPaymentTimeout(String orderId) {
        RentOrder order = getBaseMapper().selectByIdForUpdate(orderId);
        LocalDateTime now = LocalDateTime.now();
        if (order == null || !"pendingPayment".equals(order.getStatus())
                || order.getPaymentDeadlineAt() == null
                || order.getPaymentDeadlineAt().isAfter(now)) {
            return;
        }

        expireUnpaidOrder(order, now);
    }

    private void expireUnpaidOrder(RentOrder order, LocalDateTime now) {
        RentContract contract = rentContractMapper.selectByOrderIdForUpdate(order.getId());
        if (contract != null && !"signed".equals(contract.getStatus())) {
            contract.setStatus("expired");
            contract.setFailureCode("PAYMENT_TIMEOUT");
            contract.setUpdatedAt(now);
            rentContractMapper.updateById(contract);
        }
        order.setStatus("paymentExpired");
        order.setPrePaymentDeadlineAt(null);
        order.setCancelReason(PAYMENT_TIMEOUT_REASON);
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        updateById(order);
        releaseHouseReservation(order, now);
        log.info("租客签署后支付超时，订单失效并释放房源: orderId={}, houseId={}",
                order.getId(), order.getHouseId());
    }

    @Override
    @Transactional
    public void processPrePaymentTimeout(String orderId, LocalDateTime now) {
        RentOrder order = getBaseMapper().selectByIdForUpdate(orderId);
        if (order == null || order.getPaidAt() != null
                || !("pendingRealName".equals(order.getStatus())
                || "pendingContract".equals(order.getStatus())
                || "pendingTenantSign".equals(order.getStatus()))) {
            return;
        }

        if (order.getPrePaymentDeadlineAt() == null
                || order.getPrePaymentDeadlineAt().isAfter(now)) {
            return;
        }

        String previousStatus = order.getStatus();
        RentContract contract = rentContractMapper.selectByOrderIdForUpdate(orderId);
        if (contract != null && !"signed".equals(contract.getStatus())) {
            contract.setStatus("expired");
            contract.setFailureCode("PRE_PAYMENT_TIMEOUT");
            contract.setUpdatedAt(now);
            rentContractMapper.updateById(contract);
        }

        order.setStatus("cancelled");
        order.setPrePaymentDeadlineAt(null);
        order.setCancelReason("支付前办理超时");
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        updateById(order);

        releaseHouseReservation(order, now);

        log.info("支付前流程超时，已原子关闭订单并释放房源: orderId={}, houseId={}, previousStatus={}",
                order.getId(), order.getHouseId(), previousStatus);
    }

    // ==================== 合同状态刷新 ====================

    @Transactional
    public EsignSignStatusResponse contractRefresh(String userId, String orderId) {
        RentOrder order = getRelatedOrderForUpdate(userId, orderId);
        boolean isTenant = userId.equals(order.getUserId());

        // 已完成 → 直接返回本地状态，不调 e签宝
        if ("completed".equals(order.getStatus())) {
            return new EsignSignStatusResponse("COMPLETED", true, true, true, true, order.getSignedAt());
        }

        RentContract contract = rentContractMapper.selectByOrderIdForUpdate(orderId);
        if (contract == null) {
            throw BusinessException.notFound("租房合同不存在");
        }
        if (contract.getSignFlowId() == null || contract.getSignFlowId().isBlank()) {
            return new EsignSignStatusResponse(
                    "NOT_STARTED", false, false, false, false, null);
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
            completeAfterLandlordSignature(contract);
            return new EsignSignStatusResponse("COMPLETED",
                    true, true, true,
                    true, contract.getSignedAt());
        }

        EsignV3Client.SignFlowDetailResponse detail =
                esignV3Client.getSignFlowDetail(contract.getSignFlowId());
        SignProgress progress = applySignFlowDetail(contract, detail);

        return new EsignSignStatusResponse(
                progress.completed() ? "COMPLETED" : "SIGNING",
                progress.lessorSigned(), progress.tenantSigned(),
                isTenant ? progress.tenantSigned() : progress.lessorSigned(),
                progress.completed(), progress.completed() ? contract.getSignedAt() : null);
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
        RentOrder lockedOrder = getBaseMapper().selectByIdForUpdate(order.getId());
        if (lockedOrder == null || "completed".equals(lockedOrder.getStatus())) return; // 幂等
        LocalDateTime now = LocalDateTime.now();
        if (!"pendingLandlordSign".equals(lockedOrder.getStatus())
                || lockedOrder.getPaidAt() == null
                || contract == null || !"signed".equals(contract.getStatus())) {
            throw BusinessException.badRequest("订单尚未满足租客签署、支付和房东签署条件，不能创建租约");
        }

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
        lockedOrder.setPrePaymentDeadlineAt(null);
        if (lockedOrder.getSignedAt() == null) {
            lockedOrder.setSignedAt(contract.getSignedAt());
        }
        lockedOrder.setUpdatedAt(now);
        updateById(lockedOrder);

        House house = houseService.getById(lockedOrder.getHouseId());
        if (house != null) {
            house.setStatus("rented");
            house.setReservedOrderId(null);
            house.setReservedUntil(null);
            house.setUpdatedAt(now);
            houseService.updateById(house);
        }
        if ("active".equals(leaseStatus)) {
            eventPublisher.publishEvent(new LeaseActivatedEvent(lease.getId()));
        }
        log.info("支付完成，租约已创建: orderId={}, leaseId={}", lockedOrder.getId(), lease.getId());
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
        if ("pendingLandlordSign".equals(order.getStatus()) || order.getPaidAt() != null) {
            throw BusinessException.badRequest("订单已支付，不能直接取消，请联系管理方办理退款或解约");
        }

        LocalDateTime now = LocalDateTime.now();
        RentContract contract = rentContractMapper.selectByOrderIdForUpdate(orderId);
        if (contract != null && !"signed".equals(contract.getStatus())) {
            contract.setStatus("canceled");
            contract.setFailureCode("ORDER_CANCELLED");
            contract.setUpdatedAt(now);
            rentContractMapper.updateById(contract);
        }
        order.setStatus("cancelled");
        order.setPrePaymentDeadlineAt(null);
        order.setCancelReason("用户取消订单");
        order.setCancelledAt(now);
        order.setUpdatedAt(now);
        updateById(order);

        // 仅释放当前订单持有的锁，避免旧订单误释放新订单的房源。
        releaseHouseReservation(order, now);

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

    private RentOrder getOwnedOrderForUpdate(String userId, String orderId) {
        RentOrder order = getBaseMapper().selectByIdForUpdate(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            throw BusinessException.forbidden("无权操作该订单");
        }
        return order;
    }

    private void validatePaymentWindow(RentOrder order, LocalDateTime now) {
        if ("paymentExpired".equals(order.getStatus()) || "cancelled".equals(order.getStatus())) {
            throw BusinessException.badRequest("支付已超时，订单已失效");
        }
        if (!"pendingPayment".equals(order.getStatus()) || order.getPaymentDeadlineAt() == null) {
            throw BusinessException.badRequest("租客尚未完成合同签署，暂不能支付");
        }
        if (!now.isBefore(order.getPaymentDeadlineAt())) {
            throw BusinessException.badRequest("支付已超时，订单已失效");
        }
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

    private RentOrder getRelatedOrderForUpdate(String userId, String orderId) {
        RentOrder order = getBaseMapper().selectByIdForUpdate(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (userId.equals(order.getUserId())
                || (order.getLessorUserId() != null && userId.equals(order.getLessorUserId()))) {
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
                order.getPaymentDeadlineAt(),
                order.getPrePaymentDeadlineAt(),
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
