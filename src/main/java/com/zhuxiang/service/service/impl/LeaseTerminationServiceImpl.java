package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.EsignException;
import com.zhuxiang.service.client.EsignV3Client;
import com.zhuxiang.service.config.EsignV3Properties;
import com.zhuxiang.service.dto.LeaseTerminationDtos.*;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.*;
import com.zhuxiang.service.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaseTerminationServiceImpl
        extends ServiceImpl<LeaseTerminationApplicationMapper, LeaseTerminationApplication>
        implements LeaseTerminationService {

    private static final Logger log = LoggerFactory.getLogger(LeaseTerminationServiceImpl.class);
    private static final Map<String, String> STATUS_TEXT = Map.ofEntries(
            Map.entry("pending_review", "待审核"),
            Map.entry("pending_photos", "待上传验房照片"),
            Map.entry("need_supplement", "待补充材料"),
            Map.entry("approved", "审核通过"),
            Map.entry("inspection_pending", "待验房"),
            Map.entry("settlement_pending", "待结算"),
            Map.entry("refund_pending", "待退款"),
            Map.entry("refund_failed", "退款异常"),
            Map.entry("rescission_pending", "待发起合同解约"),
            Map.entry("rescission_signing", "解约协议签署中"),
            Map.entry("rescission_failed", "合同解约异常"),
            Map.entry("completed", "已完成"),
            Map.entry("rejected", "已驳回"),
            Map.entry("cancelled", "已撤销")
    );

    private static final Map<String, String> ACTION_TITLES = Map.ofEntries(
            Map.entry("applied", "已提交申请"),
            Map.entry("approved", "审核通过"),
            Map.entry("rejected", "已驳回"),
            Map.entry("supplement_requested", "要求补充材料"),
            Map.entry("supplemented", "已补充材料"),
            Map.entry("cancelled", "已撤销"),
            Map.entry("inspection_completed", "验房完成"),
            Map.entry("settlement_confirmed", "结算确认"),
            Map.entry("refund_completed", "退款完成"),
            Map.entry("photos_submitted", "验房照片已提交"),
            Map.entry("rescission_started", "解约协议已发起"),
            Map.entry("rescission_completed", "解约协议已完成"),
            Map.entry("manual_rescission_completed", "管理端直接完成退租")
    );

    private static final Set<String> IN_PROGRESS_STATUSES = Set.of(
            "pending_review", "need_supplement", "approved", "pending_photos",
            "inspection_pending", "settlement_pending", "refund_pending", "refund_failed",
            "rescission_pending", "rescission_signing", "rescission_failed"
    );

    private static final Set<String> CANCELLABLE_STATUSES = Set.of("pending_review", "need_supplement", "pending_photos");

    private final RentContractMapper rentContractMapper;
    private final LeaseService leaseService;
    private final RentBillService rentBillService;
    private final HouseService houseService;
    private final MessageService messageService;
    private final UserService userService;
    private final LockPermissionService lockPermissionService;
    private final LockPasscodePermissionService lockPasscodePermissionService;
    private final LeaseTerminationLogMapper logMapper;
    private final DepositService depositService;
    private final ObjectMapper objectMapper;
    private final EsignV3Client esignV3Client;
    private final EsignV3Properties esignProperties;
    private final UserRealNameAuthMapper userRealNameAuthMapper;

    public LeaseTerminationServiceImpl(
            RentContractMapper rentContractMapper,
            LeaseService leaseService,
            RentBillService rentBillService,
            HouseService houseService,
            MessageService messageService,
            UserService userService,
            LockPermissionService lockPermissionService,
            LockPasscodePermissionService lockPasscodePermissionService,
            LeaseTerminationLogMapper logMapper,
            DepositService depositService,
            ObjectMapper objectMapper,
            EsignV3Client esignV3Client,
            EsignV3Properties esignProperties,
            UserRealNameAuthMapper userRealNameAuthMapper
    ) {
        this.rentContractMapper = rentContractMapper;
        this.leaseService = leaseService;
        this.rentBillService = rentBillService;
        this.houseService = houseService;
        this.messageService = messageService;
        this.userService = userService;
        this.lockPermissionService = lockPermissionService;
        this.lockPasscodePermissionService = lockPasscodePermissionService;
        this.logMapper = logMapper;
        this.depositService = depositService;
        this.objectMapper = objectMapper;
        this.esignV3Client = esignV3Client;
        this.esignProperties = esignProperties;
        this.userRealNameAuthMapper = userRealNameAuthMapper;
    }

    @Override
    public TerminationCheckResponse checkTermination(String userId, String leaseId) {
        Lease lease = requireOwnedLease(userId, leaseId);
        RentContract contract = requireLeaseContract(lease);
        List<String> tips = new ArrayList<>();
        boolean canApply = true;

        if (!"active".equalsIgnoreCase(lease.getStatus())) {
            canApply = false;
            tips.add("租约未生效或已结束，无法申请退租");
            return buildCheckResponse(canApply, false, 0, contract, tips);
        }
        if (!"signed".equals(contract.getStatus())) {
            canApply = false;
            tips.add("合同未生效，无法申请退租");
            return buildCheckResponse(canApply, false, 0, contract, tips);
        }

        boolean hasProcessing = hasInProgressApplication(lease);
        if (hasProcessing) {
            canApply = false;
            tips.add("该租约已有进行中的退租申请");
        }

        int unpaidAmount = calculateUnpaidAmount(lease.getId());
        if (unpaidAmount > 0) {
            tips.add("当前存在未缴账单，提交后将进入退租结算");
        }

        return buildCheckResponse(canApply, hasProcessing, unpaidAmount, contract, tips);
    }

    @Override
    @Transactional
    public ApplyResponse apply(String userId, String leaseId, ApplyRequest request) {
        Lease lease = requireOwnedLease(userId, leaseId);
        if (!"active".equalsIgnoreCase(lease.getStatus())) {
            throw BusinessException.badRequest("租约未生效或已结束，无法申请退租");
        }
        RentContract contract = requireLeaseContract(lease);
        if (!"signed".equals(contract.getStatus())) throw BusinessException.badRequest("合同未生效，无法申请退租");

        if (hasInProgressApplication(lease)) {
            throw BusinessException.conflict("该租约已有进行中的退租申请");
        }

        LocalDateTime now = LocalDateTime.now();
        String id = UUID.randomUUID().toString();

        LeaseTerminationApplication app = new LeaseTerminationApplication();
        app.setId(id);
        app.setApplicationNo(generateApplicationNo());
        app.setTenantId(userId);
        app.setLeaseId(lease.getId());
        app.setContractId(lease.getContractId());
        app.setHouseId(lease.getHouseId());
        app.setReason(request.reason());
        app.setExpectedMoveOutDate(request.expectedMoveOutDate());
        app.setHasMovedOut(request.hasMovedOut());
        app.setContactName(request.contactName());
        app.setContactPhone(request.contactPhone());
        app.setRemark(request.remark());
        app.setAttachments(serializeAttachments(request.attachments()));
        app.setStatus(LeaseTerminationApplication.STATUS_PENDING_PHOTOS);
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        save(app);

        writeLog(id, "applied", null, LeaseTerminationApplication.STATUS_PENDING_PHOTOS,
                userId, userId, null);

        createMessage(userId, "退租申请已提交",
                "您的退租申请（编号：" + app.getApplicationNo() + "）已提交，等待后台审核。",
                id);

        return new ApplyResponse(
                id, app.getApplicationNo(), lease.getId(),
                LeaseTerminationApplication.STATUS_PENDING_PHOTOS,
                STATUS_TEXT.get(LeaseTerminationApplication.STATUS_PENDING_PHOTOS)
        );
    }

    @Override
    public TerminationDetailResponse getCurrent(String userId, String leaseId) {
        Lease lease = requireOwnedLease(userId, leaseId);
        LeaseTerminationApplication app = findCurrentApplication(lease);
        if (app == null) return null;
        return toDetailResponse(app);
    }

    @Override
    public TerminationDetailResponse getDetail(String userId, String applicationId) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!userId.equals(app.getTenantId())) throw BusinessException.forbidden("无权查看该申请");
        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public void supplement(String userId, String applicationId, SupplementRequest request) {
        LeaseTerminationApplication app = getOwnedApplication(userId, applicationId);
        if (!"need_supplement".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许补充材料");
        }

        LocalDateTime now = LocalDateTime.now();
        app.setReason(request.reason());
        app.setExpectedMoveOutDate(request.expectedMoveOutDate());
        app.setHasMovedOut(request.hasMovedOut());
        app.setContactName(request.contactName());
        app.setContactPhone(request.contactPhone());
        app.setRemark(request.remark());
        app.setAttachments(serializeAttachments(request.attachments()));
        app.setStatus("pending_review");
        app.setUpdatedAt(now);
        updateById(app);

        writeLog(applicationId, "supplemented", "need_supplement", "pending_review", userId, userId, null);
        createMessage(userId, "退租材料已补充",
                "您的退租申请材料已补充，等待后台重新审核。", applicationId);
    }

    @Override
    @Transactional
    public void cancel(String userId, String applicationId, CancelRequest request) {
        LeaseTerminationApplication app = getOwnedApplication(userId, applicationId);
        if (!CANCELLABLE_STATUSES.contains(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许撤销");
        }

        LocalDateTime now = LocalDateTime.now();
        String fromStatus = app.getStatus();
        app.setStatus("cancelled");
        app.setCancelReason(request.cancelReason());
        app.setCancelTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        writeLog(applicationId, "cancelled", fromStatus, "cancelled", userId, userId, request.cancelReason());
    }

    @Override
    @Transactional
    public TerminationDetailResponse adminCancel(
            String adminId,
            String applicationId,
            CancelRequest request
    ) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) {
            throw BusinessException.notFound("退租申请不存在");
        }
        if (LeaseTerminationApplication.STATUS_CANCELLED.equals(app.getStatus())) {
            return toDetailResponse(app);
        }
        if (!Set.of(
                LeaseTerminationApplication.STATUS_PENDING_PHOTOS,
                LeaseTerminationApplication.STATUS_INSPECTION_PENDING
        ).contains(app.getStatus())) {
            throw BusinessException.badRequest("当前阶段已进入结算、退款或解约流程，不允许撤销");
        }

        LocalDateTime now = LocalDateTime.now();
        String fromStatus = app.getStatus();
        String reason = request.cancelReason().trim();
        app.setStatus(LeaseTerminationApplication.STATUS_CANCELLED);
        app.setCancelReason(reason);
        app.setCancelTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        String adminName = getAdminName(adminId);
        writeLog(applicationId, "cancelled", fromStatus,
                LeaseTerminationApplication.STATUS_CANCELLED,
                adminId, adminName, reason);
        createMessage(app.getTenantId(), "退租申请已撤销",
                "您的退租申请已由管理端撤销，原租约继续有效。撤销原因：" + reason,
                applicationId);
        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public RescissionSignUrlResponse getRescissionSignUrl(String userId, String applicationId) {
        LeaseTerminationApplication app = baseMapper.selectByIdForUpdate(applicationId);
        if (app == null || app.getDeletedAt() != null) {
            throw BusinessException.notFound("退租申请不存在");
        }
        if (!userId.equals(app.getTenantId())) {
            throw BusinessException.forbidden("无权操作该申请");
        }
        if (!Set.of(
                LeaseTerminationApplication.STATUS_RESCISSION_PENDING,
                LeaseTerminationApplication.STATUS_RESCISSION_SIGNING
        ).contains(app.getStatus())) {
            throw BusinessException.badRequest("解约协议尚未进入签署阶段");
        }
        User tenant = userService.getById(userId);
        if (tenant == null || !StringUtils.hasText(tenant.getPhone())) {
            throw BusinessException.badRequest("当前账号缺少手机号，无法打开解约签署页面");
        }
        UserRealNameAuth auth = getVerifiedAuth(app.getTenantId());
        if (auth == null) throw BusinessException.badRequest("租客缺少已通过的实名认证记录");
        String account = resolveEsignAccount(auth, tenant);
        if (LeaseTerminationApplication.STATUS_RESCISSION_PENDING.equals(app.getStatus())) {
            RentContract contract = rentContractMapper.selectById(app.getContractId());
            if (contract == null || !StringUtils.hasText(contract.getSignFlowId())
                    || !StringUtils.hasText(contract.getContractFileId())) {
                throw BusinessException.badRequest("原合同缺少e签宝签署信息");
            }
            initiateRescission(app, LocalDateTime.now(), contract);
        }

        EsignV3Client.SignUrlResponse response = esignV3Client.getRescissionSignUrl(
                app.getRescissionSignFlowId(), account);
        if (response.getData() == null || !StringUtils.hasText(response.getData().getUrl())) {
            throw new IllegalStateException("e签宝未返回解约协议签署链接");
        }
        return new RescissionSignUrlResponse(
                "sign",
                app.getRescissionSignFlowId(),
                response.getData().getUrl(),
                response.getData().getShortUrl());
    }

    @Override
    public TerminationDetailResponse getDetailForAdmin(String applicationId) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public TerminationDetailResponse approve(String adminId, String applicationId) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!"pending_review".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许审核通过");
        }

        LocalDateTime now = LocalDateTime.now();
        String adminName = getAdminName(adminId);

        app.setStatus("inspection_pending");
        app.setAuditUserId(adminId);
        app.setAuditTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        writeLog(applicationId, "approved", "pending_review", "inspection_pending", adminId, adminName, null);
        createMessage(app.getTenantId(), "退租申请已通过",
                "您的退租申请已审核通过，请等待验房安排。", applicationId);

        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public TerminationDetailResponse reject(String adminId, String applicationId, RejectRequest request) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!"pending_review".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许驳回");
        }

        LocalDateTime now = LocalDateTime.now();
        String adminName = getAdminName(adminId);

        app.setStatus("rejected");
        app.setRejectReason(request.rejectReason());
        app.setAuditUserId(adminId);
        app.setAuditTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        writeLog(applicationId, "rejected", "pending_review", "rejected", adminId, adminName, request.rejectReason());
        createMessage(app.getTenantId(), "退租申请被驳回",
                "您的退租申请已被驳回，原因：" + request.rejectReason(), applicationId);

        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public TerminationDetailResponse requestSupplement(String adminId, String applicationId, SupplementReasonRequest request) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!"pending_review".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许要求补充材料");
        }

        LocalDateTime now = LocalDateTime.now();
        String adminName = getAdminName(adminId);

        app.setStatus("need_supplement");
        app.setSupplementReason(request.supplementReason());
        app.setAuditUserId(adminId);
        app.setAuditTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        writeLog(applicationId, "supplement_requested", "pending_review", "need_supplement",
                adminId, adminName, request.supplementReason());
        createMessage(app.getTenantId(), "退租申请需补充材料",
                "您的退租申请需要补充材料，原因：" + request.supplementReason(), applicationId);

        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public void markPhotosSubmitted(String userId, String contractId) {
        LeaseTerminationApplication app = getOne(
                Wrappers.<LeaseTerminationApplication>lambdaQuery()
                        .eq(LeaseTerminationApplication::getContractId, contractId)
                        .eq(LeaseTerminationApplication::getTenantId, userId)
                        .in(LeaseTerminationApplication::getStatus,
                                LeaseTerminationApplication.STATUS_PENDING_PHOTOS,
                                LeaseTerminationApplication.STATUS_INSPECTION_PENDING)
                        .isNull(LeaseTerminationApplication::getDeletedAt)
                        .orderByDesc(LeaseTerminationApplication::getCreatedAt)
                        .last("LIMIT 1"), false);
        if (app == null || LeaseTerminationApplication.STATUS_INSPECTION_PENDING.equals(app.getStatus())) return;

        app.setStatus(LeaseTerminationApplication.STATUS_INSPECTION_PENDING);
        app.setUpdatedAt(LocalDateTime.now());
        updateById(app);
        writeLog(app.getId(), "photos_submitted", LeaseTerminationApplication.STATUS_PENDING_PHOTOS,
                LeaseTerminationApplication.STATUS_INSPECTION_PENDING, userId, userId, null);
    }

    @Override
    @Transactional
    public void completeInspectionByContract(String adminId, String contractId, String comment) {
        LeaseTerminationApplication app = getOne(
                Wrappers.<LeaseTerminationApplication>lambdaQuery()
                        .eq(LeaseTerminationApplication::getContractId, contractId)
                        .eq(LeaseTerminationApplication::getStatus,
                                LeaseTerminationApplication.STATUS_INSPECTION_PENDING)
                        .isNull(LeaseTerminationApplication::getDeletedAt)
                        .orderByDesc(LeaseTerminationApplication::getCreatedAt)
                        .last("LIMIT 1"), false);
        if (app == null) return;

        LocalDateTime now = LocalDateTime.now();
        app.setStatus(LeaseTerminationApplication.STATUS_SETTLEMENT_PENDING);
        app.setInspectionResult(serializeInspectionResult(adminId, comment, now));
        app.setInspectionCompletedTime(now);
        app.setUpdatedAt(now);
        updateById(app);
        writeLog(app.getId(), "inspection_completed",
                LeaseTerminationApplication.STATUS_INSPECTION_PENDING,
                LeaseTerminationApplication.STATUS_SETTLEMENT_PENDING,
                adminId, getAdminName(adminId), comment);
        createMessage(app.getTenantId(), "验房已完成",
                "您的退租验房已完成，正在进行费用结算。", app.getId());
    }

    @Override
    @Transactional
    public TerminationDetailResponse completeInspection(String adminId, String applicationId) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!"inspection_pending".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许验房");
        }

        LocalDateTime now = LocalDateTime.now();
        String adminName = getAdminName(adminId);

        app.setStatus("settlement_pending");
        app.setInspectionCompletedTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        writeLog(applicationId, "inspection_completed", "inspection_pending", "settlement_pending",
                adminId, adminName, null);
        createMessage(app.getTenantId(), "验房已完成",
                "您的退租验房已完成，正在进行费用结算。", applicationId);

        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public TerminationDetailResponse confirmSettlement(
            String adminId,
            String applicationId,
            SettlementConfirmRequest request
    ) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!"settlement_pending".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许结算确认");
        }

        if (request == null) {
            throw BusinessException.badRequest("请填写退租结算金额和明细");
        }
        LocalDateTime now = LocalDateTime.now();
        String adminName = getAdminName(adminId);

        DepositRecord depositRecord = app.getLeaseId() == null
                ? null : depositService.getByLeaseId(app.getLeaseId());
        int depositAmount = depositRecord == null ? 0 : Optional.ofNullable(depositRecord.getAmount()).orElse(0);
        List<DeductionItem> requestedItems = request.deductions() == null ? List.of() : request.deductions();
        int itemDeduction = requestedItems.stream()
                .mapToInt(item -> Optional.ofNullable(item.amount()).orElse(0)).sum();
        if (itemDeduction < 0 || itemDeduction > depositAmount) {
            throw BusinessException.badRequest("扣款明细金额超出押金范围");
        }
        int recommendedRefund = Math.max(0, depositAmount - itemDeduction);
        int refundAmount = request.refundAmount();
        if (refundAmount > depositAmount) {
            throw BusinessException.badRequest("退款金额不能超过可退押金");
        }
        if (refundAmount != recommendedRefund
                && (request.adjustmentReason() == null || request.adjustmentReason().isBlank())) {
            throw BusinessException.badRequest("调整退款金额时必须填写调整原因");
        }

        int totalDeduction = depositAmount - refundAmount;
        if (request.settlementAmount() != null && request.settlementAmount() != totalDeduction) {
            throw BusinessException.badRequest("结算扣款金额必须等于押金减退款金额");
        }
        if (itemDeduction > totalDeduction) {
            throw BusinessException.badRequest("退款金额与扣款明细不一致");
        }
        List<DepositDeduction> deductions = new ArrayList<>();
        for (DeductionItem item : requestedItems) {
            DepositDeduction deduction = new DepositDeduction();
            deduction.setDeductionType(item.deductionType());
            deduction.setAmount(item.amount());
            deduction.setDescription(item.description());
            deduction.setEvidenceUrls(serializeEvidenceUrls(item.evidenceUrls()));
            deductions.add(deduction);
        }
        if (totalDeduction > itemDeduction) {
            DepositDeduction adjustment = new DepositDeduction();
            adjustment.setDeductionType("manual_adjustment");
            adjustment.setAmount(totalDeduction - itemDeduction);
            adjustment.setDescription(request.adjustmentReason());
            deductions.add(adjustment);
        }

        app.setTotalDeduction(totalDeduction);
        app.setRecommendedRefundAmount(recommendedRefund);
        app.setRefundAmount(refundAmount);
        app.setRefundAdjustmentReason(request.adjustmentReason());
        app.setSettlementOperatorId(adminId);
        app.setSettlementDetail(serializeSettlementDetail(request));
        if (depositRecord != null) {
            depositService.settle(depositRecord.getId(), deductions, app.getSettlementDetail());
            // 退款金额为 0 时无需调用外部支付渠道，但仍应闭合押金记录状态。
            if (refundAmount == 0) {
                depositService.refund(depositRecord.getId());
            }
        } else if (refundAmount > 0) {
            throw BusinessException.badRequest("未找到押金记录，不能发起退款");
        }

        app.setStatus(refundAmount > 0
                ? LeaseTerminationApplication.STATUS_REFUND_PENDING
                : LeaseTerminationApplication.STATUS_RESCISSION_PENDING);
        app.setProcessRetryCount(0);
        app.setProcessRetryAt(now);

        app.setSettlementConfirmedTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        String toStatus = app.getStatus();
        writeLog(applicationId, "settlement_confirmed", "settlement_pending", toStatus,
                adminId, adminName, null);
        createMessage(app.getTenantId(), "退租结算已确认",
                "您的退租结算已确认" + (refundAmount > 0
                        ? "，等待退款处理。" : "，即将发起合同解约。"), applicationId);

        return toDetailResponse(app);
    }

    @Override
    @Transactional
    public TerminationDetailResponse completeRefund(String adminId, String applicationId) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!"refund_pending".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许退款完成");
        }

        processPendingFlow(applicationId);
        return toDetailResponse(getById(applicationId));
    }

    @Override
    @Transactional
    public void processPendingFlow(String applicationId) {
        LeaseTerminationApplication app = getBaseMapper().selectByIdForUpdate(applicationId);
        if (app == null || app.getDeletedAt() != null) return;
        LocalDateTime now = LocalDateTime.now();
        try {
            if (LeaseTerminationApplication.STATUS_REFUND_PENDING.equals(app.getStatus())) {
                DepositRecord deposit = app.getLeaseId() == null
                        ? null : depositService.getByLeaseId(app.getLeaseId());
                if (deposit == null) throw new IllegalStateException("未找到押金记录");
                depositService.refund(deposit.getId());
                deposit = depositService.getById(deposit.getId());
                if (deposit == null || !"refunded".equals(deposit.getStatus())) {
                    app.setProcessRetryAt(now.plusSeconds(30));
                    app.setUpdatedAt(now);
                    updateById(app);
                    return;
                }
                app.setRefundCompletedTime(now);
                app.setStatus(LeaseTerminationApplication.STATUS_RESCISSION_PENDING);
                app.setProcessLastError(null);
                app.setProcessRetryCount(0);
                app.setProcessRetryAt(now);
                app.setUpdatedAt(now);
                updateById(app);
                writeLog(app.getId(), "refund_completed",
                        LeaseTerminationApplication.STATUS_REFUND_PENDING,
                        LeaseTerminationApplication.STATUS_RESCISSION_PENDING,
                        "system", "系统", null);
                createMessage(app.getTenantId(), "退租退款已完成",
                        "退款已原路退回，即将发起电子合同解约。", app.getId());
            }

            if (LeaseTerminationApplication.STATUS_RESCISSION_PENDING.equals(app.getStatus())) {
                initiateRescission(app, now);
                return;
            }
            if (LeaseTerminationApplication.STATUS_RESCISSION_SIGNING.equals(app.getStatus())) {
                EsignV3Client.SignFlowDetailResponse detail =
                        esignV3Client.getSignFlowDetail(app.getRescissionSignFlowId());
                Integer status = detail.getData() == null ? null : detail.getData().getSignFlowStatus();
                if (Integer.valueOf(2).equals(status)) {
                    completeTerminationAfterRescission(app, now);
                } else {
                    app.setProcessRetryAt(now.plusSeconds(30));
                    app.setUpdatedAt(now);
                    updateById(app);
                }
            }
        } catch (Exception exception) {
            app.setProcessLastError(exception.getMessage());
            int retryCount = app.getProcessRetryCount() == null
                    ? 1 : app.getProcessRetryCount() + 1;
            app.setProcessRetryCount(retryCount);
            if (isTransientProcessFailure(exception)) {
                long delayMinutes = retryDelayMinutes(retryCount);
                app.setProcessRetryAt(now.plusMinutes(delayMinutes));
                log.warn("退租流程临时异常，将延迟重试 applicationId={} status={} retryCount={} delayMinutes={} error={}",
                        applicationId, app.getStatus(), retryCount, delayMinutes, exception.getMessage());
            } else if (LeaseTerminationApplication.STATUS_RESCISSION_PENDING.equals(app.getStatus())) {
                app.setStatus(LeaseTerminationApplication.STATUS_RESCISSION_FAILED);
                app.setRescissionStatus("failed");
                app.setProcessRetryAt(null);
                log.error("合同解约发生确定性业务错误，已停止自动重试 applicationId={} error={}",
                        applicationId, exception.getMessage());
            } else {
                if (LeaseTerminationApplication.STATUS_REFUND_PENDING.equals(app.getStatus())) {
                    app.setStatus(LeaseTerminationApplication.STATUS_REFUND_FAILED);
                } else if (LeaseTerminationApplication.STATUS_RESCISSION_SIGNING.equals(app.getStatus())) {
                    app.setStatus(LeaseTerminationApplication.STATUS_RESCISSION_FAILED);
                    app.setRescissionStatus("failed");
                }
                app.setProcessRetryAt(null);
                log.error("退租流程发生不可重试错误，已停止自动重试 applicationId={} status={} error={}",
                        applicationId, app.getStatus(), exception.getMessage());
            }
            app.setUpdatedAt(now);
            updateById(app);
        }
    }

    private boolean isTransientProcessFailure(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof EsignException esignException) {
                String code = esignException.getEsignCode();
                int httpStatus = esignException.getHttpStatus();
                return "NETWORK".equals(code)
                        || "10000001".equals(code)
                        || httpStatus == 408 || httpStatus == 429 || httpStatus >= 500;
            }
            if (current instanceof java.net.SocketTimeoutException
                    || current instanceof java.net.ConnectException
                    || current instanceof java.io.IOException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long retryDelayMinutes(int retryCount) {
        if (retryCount <= 1) return 1;
        if (retryCount == 2) return 5;
        if (retryCount == 3) return 15;
        return 60;
    }

    private void initiateRescission(LeaseTerminationApplication app, LocalDateTime now) {
        if (!esignProperties.isCredentialsConfigured()) {
            throw new IllegalStateException("e签宝未配置，无法发起合同解约");
        }
        RentContract contract = rentContractMapper.selectById(app.getContractId());
        if (contract == null || contract.getSignFlowId() == null || contract.getContractFileId() == null) {
            throw new IllegalStateException("原合同缺少签署流程或合同文件ID");
        }
        initiateRescission(app, now, contract);
    }

    private UserRealNameAuth getVerifiedAuth(String tenantId) {
        return userRealNameAuthMapper.selectOne(
                Wrappers.<UserRealNameAuth>lambdaQuery()
                        .eq(UserRealNameAuth::getUserId, tenantId)
                        .eq(UserRealNameAuth::getAuthStatus, "VERIFIED")
                        .orderByDesc(UserRealNameAuth::getVerifiedAt)
                        .last("LIMIT 1"));
    }

    private String resolveEsignAccount(UserRealNameAuth auth, User tenant) {
        if (auth != null && StringUtils.hasText(auth.getAccountMobile())) {
            return auth.getAccountMobile().trim();
        }
        if (tenant != null && StringUtils.hasText(tenant.getPhone())) {
            return tenant.getPhone().trim();
        }
        throw BusinessException.badRequest("租客缺少可用于e签宝办理的手机号");
    }

    private void initiateRescission(
            LeaseTerminationApplication app,
            LocalDateTime now,
            RentContract contract
    ) {
        House house = houseService.getById(contract.getHouseId());
        boolean platformHouse = house == null
                || !"LANDLORD".equalsIgnoreCase(house.getSourceType());
        String reason = "租赁关系终止：" + app.getReason();
        String flowId = platformHouse
                ? esignV3Client.initiatePlatformRescission(
                        contract.getSignFlowId(), contract.getContractFileId(), reason)
                : esignV3Client.initiatePersonalHouseRescission(
                        contract.getSignFlowId(), contract.getContractFileId(), reason);
        app.setRescissionSignFlowId(flowId);
        app.setRescissionStatus("signing");
        app.setRescissionStartedAt(now);
        app.setStatus(LeaseTerminationApplication.STATUS_RESCISSION_SIGNING);
        app.setProcessLastError(null);
        app.setProcessRetryCount(0);
        app.setProcessRetryAt(now.plusSeconds(30));
        app.setUpdatedAt(now);
        updateById(app);
        writeLog(app.getId(), "rescission_started",
                LeaseTerminationApplication.STATUS_RESCISSION_PENDING,
                LeaseTerminationApplication.STATUS_RESCISSION_SIGNING,
                "system", "系统", null);
        createMessage(app.getTenantId(), "解约协议已发起",
                "电子合同解约协议已发起，请根据e签宝短信完成签署。", app.getId());
    }


    @Override
    @Transactional
    public boolean processRescissionCallback(String signFlowId, Integer signFlowStatus) {
        LeaseTerminationApplication app =
                baseMapper.selectByRescissionFlowIdForUpdate(signFlowId);
        if (app == null) return false;
        if (Integer.valueOf(2).equals(signFlowStatus)
                && !LeaseTerminationApplication.STATUS_COMPLETED.equals(app.getStatus())) {
            completeTerminationAfterRescission(app, LocalDateTime.now());
        }
        return true;
    }

    private void completeTerminationAfterRescission(LeaseTerminationApplication app, LocalDateTime now) {
        app.setStatus(LeaseTerminationApplication.STATUS_COMPLETED);
        app.setTerminationMode("ESIGN");
        app.setRescissionStatus("completed");
        app.setRescissionCompletedAt(now);
        app.setCompletedTime(now);
        app.setProcessLastError(null);
        app.setProcessRetryAt(null);
        app.setUpdatedAt(now);
        updateById(app);
        terminateLeaseAndHouse(app, now);
        writeLog(app.getId(), "rescission_completed",
                LeaseTerminationApplication.STATUS_RESCISSION_SIGNING,
                LeaseTerminationApplication.STATUS_COMPLETED,
                "system", "系统", null);
        createMessage(app.getTenantId(), "退租已完成",
                "解约协议已签署，租约和门锁权限已完成收尾。", app.getId());
    }

    private void terminateLeaseAndHouse(LeaseTerminationApplication app, LocalDateTime now) {
        Lease lease = resolveApplicationLease(app);
        if (lease != null) {
            lease.setStatus("terminated");
            lease.setUpdatedAt(now);
            leaseService.updateById(lease);

            lockPermissionService.revokeTenantEKeyForLease(lease.getId());
            lockPasscodePermissionService.revokePasscodesForLease(lease.getId());

            // 取消租约下所有未付账单（scheduled / pending / overdue → cancelled）
            List<RentBill> unpaidBills = rentBillService.list(
                    Wrappers.<RentBill>lambdaQuery()
                            .eq(RentBill::getLeaseId, lease.getId())
                            .in(RentBill::getStatus, "scheduled", "pending", "overdue"));
            for (RentBill bill : unpaidBills) {
                bill.setStatus("cancelled");
                bill.setUpdatedAt(now);
                rentBillService.updateById(bill);
            }
        }

        if (app.getContractId() != null) {
            RentContract contract = rentContractMapper.selectById(app.getContractId());
            if (contract != null && !"terminated".equalsIgnoreCase(contract.getStatus())) {
                contract.setStatus("terminated");
                contract.setUpdatedAt(now);
                rentContractMapper.updateById(contract);
            }
        }

        House house = houseService.getById(app.getHouseId());
        if (house != null) {
            house.setStatus("offline");
            house.setUpdatedAt(now);
            houseService.updateById(house);
        }
    }

    private TerminationCheckResponse buildCheckResponse(
            boolean canApply, boolean hasProcessing, int unpaidAmount,
            RentContract contract, List<String> tips) {
        return new TerminationCheckResponse(
                canApply, hasProcessing, unpaidAmount,
                Optional.ofNullable(contract.getDeposit()).orElse(0),
                contract.getStartDate(),
                contract.getEndDate(), tips
        );
    }

    private int calculateUnpaidAmount(String leaseId) {
        List<RentBill> unpaidBills = rentBillService.list(Wrappers.<RentBill>lambdaQuery()
                .eq(RentBill::getLeaseId, leaseId)
                .in(RentBill::getStatus, "pending", "overdue"));
        return unpaidBills.stream().mapToInt(b -> Optional.ofNullable(b.getAmountDue()).orElse(0)).sum();
    }

    private boolean hasInProgressApplication(Lease lease) {
        long currentCount = count(Wrappers.<LeaseTerminationApplication>lambdaQuery()
                .eq(LeaseTerminationApplication::getLeaseId, lease.getId())
                .in(LeaseTerminationApplication::getStatus, IN_PROGRESS_STATUSES)
                .isNull(LeaseTerminationApplication::getDeletedAt));
        if (currentCount > 0 || lease.getContractId() == null) {
            return currentCount > 0;
        }
        return count(Wrappers.<LeaseTerminationApplication>lambdaQuery()
                .isNull(LeaseTerminationApplication::getLeaseId)
                .eq(LeaseTerminationApplication::getContractId, lease.getContractId())
                .in(LeaseTerminationApplication::getStatus, IN_PROGRESS_STATUSES)
                .isNull(LeaseTerminationApplication::getDeletedAt)) > 0;
    }

    private LeaseTerminationApplication findCurrentApplication(Lease lease) {
        LeaseTerminationApplication application = getOne(
                Wrappers.<LeaseTerminationApplication>lambdaQuery()
                        .eq(LeaseTerminationApplication::getLeaseId, lease.getId())
                        .in(LeaseTerminationApplication::getStatus, IN_PROGRESS_STATUSES)
                        .isNull(LeaseTerminationApplication::getDeletedAt)
                        .orderByDesc(LeaseTerminationApplication::getCreatedAt)
                        .last("LIMIT 1"),
                false
        );
        if (application != null || lease.getContractId() == null) {
            return application;
        }
        return getOne(
                Wrappers.<LeaseTerminationApplication>lambdaQuery()
                        .isNull(LeaseTerminationApplication::getLeaseId)
                        .eq(LeaseTerminationApplication::getContractId, lease.getContractId())
                        .in(LeaseTerminationApplication::getStatus, IN_PROGRESS_STATUSES)
                        .isNull(LeaseTerminationApplication::getDeletedAt)
                        .orderByDesc(LeaseTerminationApplication::getCreatedAt)
                        .last("LIMIT 1"),
                false
        );
    }

    private Lease requireOwnedLease(String userId, String leaseId) {
        Lease lease = leaseService.getById(leaseId);
        if (lease == null) {
            throw BusinessException.notFound("租约不存在");
        }
        if (!userId.equals(lease.getUserId())) {
            throw BusinessException.forbidden("无权操作该租约");
        }
        return lease;
    }

    private RentContract requireLeaseContract(Lease lease) {
        if (lease.getContractId() == null) {
            throw BusinessException.notFound("租约未关联合同");
        }
        RentContract contract = rentContractMapper.selectById(lease.getContractId());
        if (contract == null) {
            throw BusinessException.notFound("租约关联合同不存在");
        }
        if (!Objects.equals(lease.getUserId(), contract.getUserId())
                || !Objects.equals(lease.getHouseId(), contract.getHouseId())) {
            throw BusinessException.conflict("租约与合同关联信息不一致");
        }
        return contract;
    }

    private Lease resolveApplicationLease(LeaseTerminationApplication application) {
        if (application.getLeaseId() != null) {
            return leaseService.getById(application.getLeaseId());
        }
        if (application.getContractId() == null) {
            return null;
        }
        return leaseService.getOne(
                Wrappers.<Lease>lambdaQuery()
                        .eq(Lease::getContractId, application.getContractId())
                        .orderByDesc(Lease::getCreatedAt)
                        .last("LIMIT 1"),
                false
        );
    }

    private LeaseTerminationApplication getOwnedApplication(String userId, String applicationId) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!userId.equals(app.getTenantId())) throw BusinessException.forbidden("无权操作该申请");
        return app;
    }

    private TerminationDetailResponse toDetailResponse(LeaseTerminationApplication app) {
        House house = houseService.getById(app.getHouseId());
        String houseName = house != null ? house.getTitle() : "";
        List<TimelineItem> timeline = getTimeline(app.getId());
        DepositRecord deposit = app.getLeaseId() == null ? null : depositService.getByLeaseId(app.getLeaseId());
        int depositAmount = deposit == null ? 0 : Optional.ofNullable(deposit.getAmount()).orElse(0);
        int unpaidAmount = app.getLeaseId() == null ? 0 : calculateUnpaidAmount(app.getLeaseId());

        return new TerminationDetailResponse(
                app.getId(), app.getApplicationNo(), app.getLeaseId(), app.getContractId(),
                app.getHouseId(), houseName,
                app.getReason(), app.getExpectedMoveOutDate(), app.getHasMovedOut(),
                app.getContactName(), app.getContactPhone(), app.getRemark(),
                deserializeAttachments(app.getAttachments()),
                app.getStatus(), STATUS_TEXT.getOrDefault(app.getStatus(), app.getStatus()),
                app.getRejectReason(), app.getSupplementReason(),
                depositAmount, unpaidAmount,
                app.getTotalDeduction(), app.getRefundAmount(),
                app.getRecommendedRefundAmount(), app.getRefundAdjustmentReason(),
                app.getRescissionSignFlowId(), app.getRescissionStatus(),
                app.getTerminationMode(), app.getManualTerminationReason(),
                deserializeStringList(app.getManualAgreementUrls()),
                app.getManualCompletedBy(), app.getManualCompletedAt(),
                app.getProcessLastError(),
                app.getActualMoveOutDate(),
                app.getCreatedAt(), app.getUpdatedAt(), timeline
        );
    }

    private List<TimelineItem> getTimeline(String applicationId) {
        List<LeaseTerminationLog> logs = logMapper.selectList(Wrappers.<LeaseTerminationLog>lambdaQuery()
                .eq(LeaseTerminationLog::getApplicationId, applicationId)
                .orderByAsc(LeaseTerminationLog::getCreatedAt));
        return logs.stream()
                .map(log -> new TimelineItem(
                        log.getToStatus(),
                        ACTION_TITLES.getOrDefault(log.getAction(), log.getAction()),
                        log.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    private void writeLog(String applicationId, String action, String fromStatus, String toStatus,
                          String operatorId, String operatorName, String remark) {
        LeaseTerminationLog log = new LeaseTerminationLog();
        log.setId(UUID.randomUUID().toString());
        log.setApplicationId(applicationId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    private void createMessage(String userId, String title, String content, String applicationId) {
        messageService.sendMessage(userId, "lease", title, content, "lease", applicationId);
    }

    private String getAdminName(String adminId) {
        User user = userService.getById(adminId);
        return user != null ? user.getNickname() : adminId;
    }

    private String generateApplicationNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(Wrappers.<LeaseTerminationApplication>lambdaQuery()
                .ge(LeaseTerminationApplication::getCreatedAt, LocalDate.now().atStartOfDay()));
        return "TZ" + date + String.format("%04d", count + 1);
    }

    private String serializeAttachments(List<AttachmentItem> items) {
        if (items == null || items.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化附件失败", e);
        }
    }

    private String serializeSettlementDetail(SettlementConfirmRequest request) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("settlementAmount", Optional.ofNullable(request.settlementAmount()).orElse(0));
        detail.put("refundAmount", Optional.ofNullable(request.refundAmount()).orElse(0));
        detail.put("adjustmentReason", request.adjustmentReason());
        detail.put("remark", request.remark());
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化退租结算明细失败", exception);
        }
    }

    private String serializeInspectionResult(String adminId, String comment, LocalDateTime completedAt) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completedBy", adminId);
        result.put("completedAt", completedAt.toString());
        if (comment != null && !comment.isBlank()) {
            result.put("comment", comment);
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化验房结果失败", exception);
        }
    }

    private String serializeEvidenceUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private List<AttachmentItem> deserializeAttachments(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AttachmentItem>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<String> deserializeStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
