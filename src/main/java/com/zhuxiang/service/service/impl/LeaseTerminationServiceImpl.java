package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.LeaseTerminationDtos.*;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.mapper.*;
import com.zhuxiang.service.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaseTerminationServiceImpl
        extends ServiceImpl<LeaseTerminationApplicationMapper, LeaseTerminationApplication>
        implements LeaseTerminationService {

    private static final Map<String, String> STATUS_TEXT = Map.ofEntries(
            Map.entry("pending_review", "待审核"),
            Map.entry("need_supplement", "待补充材料"),
            Map.entry("approved", "审核通过"),
            Map.entry("inspection_pending", "待验房"),
            Map.entry("settlement_pending", "待结算"),
            Map.entry("refund_pending", "待退款"),
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
            Map.entry("refund_completed", "退款完成")
    );

    private static final Set<String> IN_PROGRESS_STATUSES = Set.of(
            "pending_review", "need_supplement", "approved",
            "inspection_pending", "settlement_pending", "refund_pending"
    );

    private static final Set<String> CANCELLABLE_STATUSES = Set.of("pending_review", "need_supplement");

    private final RentContractMapper rentContractMapper;
    private final LeaseService leaseService;
    private final RentBillService rentBillService;
    private final HouseService houseService;
    private final MessageService messageService;
    private final UserService userService;
    private final LeaseTerminationLogMapper logMapper;
    private final ObjectMapper objectMapper;

    public LeaseTerminationServiceImpl(
            RentContractMapper rentContractMapper,
            LeaseService leaseService,
            RentBillService rentBillService,
            HouseService houseService,
            MessageService messageService,
            UserService userService,
            LeaseTerminationLogMapper logMapper,
            ObjectMapper objectMapper
    ) {
        this.rentContractMapper = rentContractMapper;
        this.leaseService = leaseService;
        this.rentBillService = rentBillService;
        this.houseService = houseService;
        this.messageService = messageService;
        this.userService = userService;
        this.logMapper = logMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TerminationCheckResponse checkTermination(String userId, String contractId) {
        RentContract contract = rentContractMapper.selectById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (!userId.equals(contract.getUserId())) {
            throw BusinessException.forbidden("无权操作该合同");
        }

        List<String> tips = new ArrayList<>();
        boolean canApply = true;

        if (!"signed".equals(contract.getStatus())) {
            canApply = false;
            tips.add("合同未生效，无法申请退租");
            return buildCheckResponse(canApply, false, 0, contract, tips);
        }

        Lease lease = leaseService.getOne(Wrappers.<Lease>lambdaQuery()
                .eq(Lease::getContractId, contractId)
                .eq(Lease::getStatus, "active")
                .last("LIMIT 1"), false);
        if (lease == null) {
            canApply = false;
            tips.add("未找到生效中的租约，无法申请退租");
            return buildCheckResponse(canApply, false, 0, contract, tips);
        }

        boolean hasProcessing = hasInProgressApplication(contractId);
        if (hasProcessing) {
            canApply = false;
            tips.add("该合同已有进行中的退租申请");
        }

        int unpaidAmount = calculateUnpaidAmount(lease.getId());
        if (unpaidAmount > 0) {
            tips.add("当前存在未缴账单，提交后将进入退租结算");
        }

        return buildCheckResponse(canApply, hasProcessing, unpaidAmount, contract, tips);
    }

    @Override
    @Transactional
    public ApplyResponse apply(String userId, String contractId, ApplyRequest request) {
        RentContract contract = rentContractMapper.selectById(contractId);
        if (contract == null) throw BusinessException.notFound("合同不存在");
        if (!userId.equals(contract.getUserId())) throw BusinessException.forbidden("无权操作该合同");
        if (!"signed".equals(contract.getStatus())) throw BusinessException.badRequest("合同未生效，无法申请退租");

        if (hasInProgressApplication(contractId)) {
            throw BusinessException.conflict("该合同已有进行中的退租申请");
        }

        Lease lease = leaseService.getOne(Wrappers.<Lease>lambdaQuery()
                .eq(Lease::getContractId, contractId)
                .eq(Lease::getStatus, "active")
                .last("LIMIT 1"), false);
        if (lease == null) {
            throw BusinessException.badRequest("未找到生效中的租约");
        }

        LocalDateTime now = LocalDateTime.now();
        String id = UUID.randomUUID().toString();

        LeaseTerminationApplication app = new LeaseTerminationApplication();
        app.setId(id);
        app.setApplicationNo(generateApplicationNo());
        app.setTenantId(userId);
        app.setContractId(contractId);
        app.setHouseId(contract.getHouseId());
        app.setReason(request.reason());
        app.setExpectedMoveOutDate(request.expectedMoveOutDate());
        app.setHasMovedOut(request.hasMovedOut());
        app.setContactName(request.contactName());
        app.setContactPhone(request.contactPhone());
        app.setRemark(request.remark());
        app.setAttachments(serializeAttachments(request.attachments()));
        app.setStatus("pending_review");
        app.setCreatedAt(now);
        app.setUpdatedAt(now);
        save(app);

        writeLog(id, "applied", null, "pending_review", userId, userId, null);

        createMessage(userId, "退租申请已提交",
                "您的退租申请（编号：" + app.getApplicationNo() + "）已提交，等待后台审核。",
                id);

        return new ApplyResponse(id, app.getApplicationNo(), "pending_review", STATUS_TEXT.get("pending_review"));
    }

    @Override
    public TerminationDetailResponse getCurrent(String userId, String contractId) {
        LeaseTerminationApplication app = getOne(Wrappers.<LeaseTerminationApplication>lambdaQuery()
                .eq(LeaseTerminationApplication::getContractId, contractId)
                .in(LeaseTerminationApplication::getStatus, IN_PROGRESS_STATUSES)
                .isNull(LeaseTerminationApplication::getDeletedAt)
                .orderByDesc(LeaseTerminationApplication::getCreatedAt)
                .last("LIMIT 1"), false);
        if (app == null) return null;
        if (!userId.equals(app.getTenantId())) throw BusinessException.forbidden("无权查看该申请");
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
        app.setStatus("cancelled");
        app.setCancelReason(request.cancelReason());
        app.setCancelTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        writeLog(applicationId, "cancelled", app.getStatus(), "cancelled", userId, userId, request.cancelReason());
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

        terminateLeaseAndHouse(app, now);

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
    public TerminationDetailResponse confirmSettlement(String adminId, String applicationId) {
        LeaseTerminationApplication app = getById(applicationId);
        if (app == null || app.getDeletedAt() != null) throw BusinessException.notFound("退租申请不存在");
        if (!"settlement_pending".equals(app.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许结算确认");
        }

        LocalDateTime now = LocalDateTime.now();
        String adminName = getAdminName(adminId);

        int refundAmount = Optional.ofNullable(app.getRefundAmount()).orElse(0);

        if (refundAmount > 0) {
            app.setStatus("refund_pending");
        } else {
            app.setStatus("completed");
            terminateLeaseAndHouse(app, now);
            app.setCompletedTime(now);
        }

        app.setSettlementConfirmedTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        String toStatus = app.getStatus();
        writeLog(applicationId, "settlement_confirmed", "settlement_pending", toStatus,
                adminId, adminName, null);
        createMessage(app.getTenantId(), "退租结算已确认",
                "您的退租结算已确认" + (refundAmount > 0 ? "，等待退款处理。" : "，退租流程已完成。"), applicationId);

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

        LocalDateTime now = LocalDateTime.now();
        String adminName = getAdminName(adminId);

        app.setStatus("completed");
        app.setRefundCompletedTime(now);
        app.setCompletedTime(now);
        app.setUpdatedAt(now);
        updateById(app);

        terminateLeaseAndHouse(app, now);

        writeLog(applicationId, "refund_completed", "refund_pending", "completed",
                adminId, adminName, null);
        createMessage(app.getTenantId(), "退租退款已完成",
                "您的退租退款已完成，退租流程全部结束。", applicationId);

        return toDetailResponse(app);
    }

    private void terminateLeaseAndHouse(LeaseTerminationApplication app, LocalDateTime now) {
        Lease lease = leaseService.getOne(Wrappers.<Lease>lambdaQuery()
                .eq(Lease::getContractId, app.getContractId())
                .eq(Lease::getStatus, "active")
                .last("LIMIT 1"), false);
        if (lease != null) {
            lease.setStatus("terminated");
            lease.setUpdatedAt(now);
            leaseService.updateById(lease);
        }

        House house = houseService.getById(app.getHouseId());
        if (house != null) {
            house.setStatus("available");
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
                contract.getEndDate(), tips
        );
    }

    private int calculateUnpaidAmount(String leaseId) {
        List<RentBill> unpaidBills = rentBillService.list(Wrappers.<RentBill>lambdaQuery()
                .eq(RentBill::getLeaseId, leaseId)
                .in(RentBill::getStatus, "pending", "overdue"));
        return unpaidBills.stream().mapToInt(b -> Optional.ofNullable(b.getAmountDue()).orElse(0)).sum();
    }

    private boolean hasInProgressApplication(String contractId) {
        return count(Wrappers.<LeaseTerminationApplication>lambdaQuery()
                .eq(LeaseTerminationApplication::getContractId, contractId)
                .in(LeaseTerminationApplication::getStatus, IN_PROGRESS_STATUSES)
                .isNull(LeaseTerminationApplication::getDeletedAt)) > 0;
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

        return new TerminationDetailResponse(
                app.getId(), app.getApplicationNo(), app.getContractId(),
                app.getHouseId(), houseName,
                app.getReason(), app.getExpectedMoveOutDate(), app.getHasMovedOut(),
                app.getContactName(), app.getContactPhone(), app.getRemark(),
                deserializeAttachments(app.getAttachments()),
                app.getStatus(), STATUS_TEXT.getOrDefault(app.getStatus(), app.getStatus()),
                app.getRejectReason(), app.getSupplementReason(),
                app.getTotalDeduction(), app.getRefundAmount(),
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

    private List<AttachmentItem> deserializeAttachments(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<AttachmentItem>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
