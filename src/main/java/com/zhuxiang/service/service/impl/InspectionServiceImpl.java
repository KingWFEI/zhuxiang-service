package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.dto.InspectionDtos;
import com.zhuxiang.service.entity.*;
import com.zhuxiang.service.entity.RentContract;
import com.zhuxiang.service.mapper.*;
import com.zhuxiang.service.service.FileRecordService;
import com.zhuxiang.service.service.InspectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 租约验收服务实现。
 * <p>
 * 简化流程：DRAFT → SUBMITTED → LOCKED
 * - 租客（contract.userId）可查看和提交自己的入住/退租验收
 * - 管理端可查看对比、创建结算、锁定验房
 * - DRAFT：照片尚未提交，可继续上传
 * - SUBMITTED：照片已提交，可由工作人员补充或重新上传
 * - LOCKED：管理端已确认线下验房完成，照片已归档，禁止删除或覆盖
 */
@Service
public class InspectionServiceImpl
        extends ServiceImpl<LeaseInspectionSnapshotMapper, LeaseInspectionSnapshot>
        implements InspectionService {

    private static final Logger log = LoggerFactory.getLogger(InspectionServiceImpl.class);

    private final ObjectMapper objectMapper;
    private final InspectionPhotoMapper inspectionPhotoMapper;
    private final DepositDeductionItemMapper deductionItemMapper;
    private final HouseInspectionTemplateMapper templateMapper;
    private final RentContractMapper rentContractMapper;
    private final FileRecordService fileRecordService;

    public InspectionServiceImpl(ObjectMapper objectMapper,
                                  InspectionPhotoMapper inspectionPhotoMapper,
                                  DepositDeductionItemMapper deductionItemMapper,
                                  HouseInspectionTemplateMapper templateMapper,
                                  RentContractMapper rentContractMapper,
                                  FileRecordService fileRecordService) {
        this.objectMapper = objectMapper;
        this.inspectionPhotoMapper = inspectionPhotoMapper;
        this.deductionItemMapper = deductionItemMapper;
        this.templateMapper = templateMapper;
        this.rentContractMapper = rentContractMapper;
        this.fileRecordService = fileRecordService;
    }

    // ==================== 快照创建 ====================

    @Override
    @Transactional
    public void createSnapshotFromTemplate(String contractId, String leaseId, String houseId) {
        HouseInspectionTemplate template = templateMapper.selectOne(
                Wrappers.<HouseInspectionTemplate>lambdaQuery()
                        .eq(HouseInspectionTemplate::getHouseId, houseId)
                        .last("LIMIT 1"), false);

        if (template == null) {
            log.warn("房源 {} 未配置验收模板，快照创建跳过（退租时将无对比基准）: contractId={}", houseId, contractId);
            return;
        }

        LeaseInspectionSnapshot existing = getOne(
                Wrappers.<LeaseInspectionSnapshot>lambdaQuery()
                        .eq(LeaseInspectionSnapshot::getContractId, contractId)
                        .last("LIMIT 1"), false);
        if (existing != null) {
            log.info("合同 {} 已有验收快照，跳过（幂等）", contractId);
            return;
        }

        LeaseInspectionSnapshot snapshot = new LeaseInspectionSnapshot();
        snapshot.setContractId(contractId);
        snapshot.setLeaseId(leaseId);
        snapshot.setHouseId(houseId);
        snapshot.setTemplateVersion(template.getVersion());
        snapshot.setRooms(template.getRooms());
        snapshot.setStatus(LeaseInspectionSnapshot.STATUS_DRAFT);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setUpdatedAt(LocalDateTime.now());
        save(snapshot);

        log.info("验收快照已创建: contractId={}, templateVersion={}", contractId, template.getVersion());
    }

    // ==================== 入住验收 ====================

    @Override
    public InspectionDtos.MoveInInspectionResponse getMoveInInspection(String userId, String contractId) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);
        verifyContractUser(userId, contractId);

        List<InspectionDtos.TemplateRoomItem> rooms = deserializeRooms(snapshot.getRooms());
        List<InspectionDtos.PhotoItem> existingPhotos = loadPhotos(contractId, InspectionPhoto.STAGE_MOVE_IN);

        return new InspectionDtos.MoveInInspectionResponse(
                snapshot.getId(), snapshot.getContractId(), snapshot.getHouseId(),
                snapshot.getTemplateVersion(), snapshot.getStatus(),
                rooms, existingPhotos);
    }

    @Override
    @Transactional
    public void submitMoveInInspection(String userId, String contractId,
                                        InspectionDtos.SubmitMoveInRequest request) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);
        verifyContractUser(userId, contractId);

        if (!LeaseInspectionSnapshot.STATUS_DRAFT.equals(snapshot.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许提交入住验收，当前状态: " + snapshot.getStatus());
        }

        List<InspectionDtos.TemplateRoomItem> rooms = deserializeRooms(snapshot.getRooms());

        // 校验 itemCode 存在于快照、URL 属于当前用户、满足必拍数量
        validatePhotos(rooms, request.photos(), userId, "move_in_inspection");

        saveInspectionPhotos(contractId, userId, InspectionPhoto.STAGE_MOVE_IN, request.photos());

        LocalDateTime now = LocalDateTime.now();
        snapshot.setStatus(LeaseInspectionSnapshot.STATUS_SUBMITTED);
        snapshot.setMoveInSubmittedAt(now);
        snapshot.setMoveInSubmittedBy(userId);
        snapshot.setUpdatedAt(now);
        updateById(snapshot);

        log.info("入住验收已提交: contractId={}, userId={}, photoCount={}",
                contractId, userId, request.photos().size());
    }

    @Override
    @Transactional
    public void confirmMoveIn(String userId, String contractId) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);
        verifyContractUser(userId, contractId);

        if (!LeaseInspectionSnapshot.STATUS_SUBMITTED.equals(snapshot.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许确认入住验收，当前状态: " + snapshot.getStatus());
        }
        snapshot.setStatus(LeaseInspectionSnapshot.STATUS_TENANT_CONFIRMED);
        snapshot.setUpdatedAt(LocalDateTime.now());
        updateById(snapshot);

        log.info("租客确认入住验收: contractId={}, userId={}", contractId, userId);
    }

    // ==================== 退租验收 ====================

    @Override
    public InspectionDtos.MoveOutInspectionResponse getMoveOutInspection(String userId, String contractId) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);
        verifyContractUser(userId, contractId);

        List<InspectionDtos.TemplateRoomItem> rooms = deserializeRooms(snapshot.getRooms());
        List<InspectionDtos.PhotoItem> moveInPhotos = loadPhotos(contractId, InspectionPhoto.STAGE_MOVE_IN);
        List<InspectionDtos.PhotoItem> existingMoveOutPhotos = loadPhotos(contractId, InspectionPhoto.STAGE_MOVE_OUT);

        return new InspectionDtos.MoveOutInspectionResponse(
                snapshot.getId(), snapshot.getContractId(), snapshot.getHouseId(),
                snapshot.getStatus(),
                rooms, moveInPhotos, existingMoveOutPhotos);
    }

    @Override
    @Transactional
    public void submitMoveOutInspection(String userId, String contractId,
                                         InspectionDtos.SubmitMoveOutRequest request) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);
        verifyContractUser(userId, contractId);

        String currentStatus = snapshot.getStatus();
        // DRAFT 可首次上传，SUBMITTED 可补充/重新上传
        if (!LeaseInspectionSnapshot.STATUS_DRAFT.equals(currentStatus)
                && !LeaseInspectionSnapshot.STATUS_SUBMITTED.equals(currentStatus)) {
            throw BusinessException.badRequest("当前状态不允许提交退租验收，当前状态: " + currentStatus);
        }

        List<InspectionDtos.TemplateRoomItem> rooms = deserializeRooms(snapshot.getRooms());

        // 校验 URL 属于当前用户、itemCode 存在于快照
        validatePhotos(rooms, request.photos(), userId, "move_out_inspection");

        // LOCKED 后禁止删除或覆盖照片，非锁定状态可覆盖
        if (LeaseInspectionSnapshot.STATUS_LOCKED.equals(currentStatus)) {
            throw BusinessException.badRequest("验房已锁定，禁止修改照片");
        }

        // 幂等：先删除已有退租照片，再插入
        inspectionPhotoMapper.delete(
                Wrappers.<InspectionPhoto>lambdaQuery()
                        .eq(InspectionPhoto::getContractId, contractId)
                        .eq(InspectionPhoto::getStage, InspectionPhoto.STAGE_MOVE_OUT));

        saveInspectionPhotos(contractId, userId, InspectionPhoto.STAGE_MOVE_OUT, request.photos());

        LocalDateTime now = LocalDateTime.now();
        // DRAFT 首次提交 → SUBMITTED
        if (LeaseInspectionSnapshot.STATUS_DRAFT.equals(currentStatus)) {
            snapshot.setStatus(LeaseInspectionSnapshot.STATUS_SUBMITTED);
        }
        snapshot.setMoveOutSubmittedAt(now);
        snapshot.setMoveOutSubmittedBy(userId);
        snapshot.setUpdatedAt(now);
        updateById(snapshot);

        log.info("退租验收已提交: contractId={}, userId={}, photoCount={}",
                contractId, userId, request.photos().size());
    }

    // ==================== 管理端对比 ====================

    @Override
    public InspectionDtos.ComparisonResponse getComparison(String contractId) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);

        List<InspectionDtos.TemplateRoomItem> rooms = deserializeRooms(snapshot.getRooms());
        List<InspectionPhoto> moveInPhotos = getPhotoEntities(contractId, InspectionPhoto.STAGE_MOVE_IN);
        List<InspectionPhoto> moveOutPhotos = getPhotoEntities(contractId, InspectionPhoto.STAGE_MOVE_OUT);

        Map<String, List<String>> moveInMap = groupPhotoUrls(moveInPhotos);
        Map<String, List<String>> moveOutMap = groupPhotoUrls(moveOutPhotos);

        List<DepositDeductionItem> deductions = deductionItemMapper.selectList(
                Wrappers.<DepositDeductionItem>lambdaQuery()
                        .eq(DepositDeductionItem::getSnapshotId, snapshot.getId()));

        Map<String, DepositDeductionItem> deductionMap = deductions.stream()
                .collect(Collectors.toMap(
                        d -> d.getRoomCode() + "|" + d.getItemCode(),
                        d -> d,
                        (a, b) -> a));

        List<InspectionDtos.ComparisonRoomItem> comparisonRooms = new ArrayList<>();
        int totalDeduction = 0;

        for (InspectionDtos.TemplateRoomItem room : rooms) {
            List<InspectionDtos.ComparisonItem> items = new ArrayList<>();
            for (InspectionDtos.TemplateCheckItem checkItem : room.items()) {
                if (!checkItem.enabled()) continue;

                String key = room.roomCode() + "|" + checkItem.itemCode();
                DepositDeductionItem deduction = deductionMap.get(key);

                items.add(new InspectionDtos.ComparisonItem(
                        checkItem.itemCode(),
                        checkItem.itemName(),
                        moveInMap.getOrDefault(key, List.of()),
                        moveOutMap.getOrDefault(key, List.of()),
                        deduction != null ? deduction.getResult() : null,
                        deduction != null ? deduction.getDeductionAmount() : null,
                        deduction != null ? deduction.getReason() : null,
                        deduction != null ? deduction.getTenantStatus() : null));
            }
            comparisonRooms.add(new InspectionDtos.ComparisonRoomItem(
                    room.roomCode(), room.roomName(), items));
        }

        for (DepositDeductionItem d : deductions) {
            totalDeduction += d.getDeductionAmount() != null ? d.getDeductionAmount() : 0;
        }

        return new InspectionDtos.ComparisonResponse(
                snapshot.getId(), snapshot.getContractId(), snapshot.getStatus(),
                comparisonRooms, totalDeduction);
    }

    // ==================== 押金扣款 ====================

    @Override
    @Transactional
    public InspectionDtos.SettlementResponse createSettlement(String adminId, String contractId,
                                                               InspectionDtos.CreateSettlementRequest request) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);

        // 幂等：先删除已有扣款，再插入
        deductionItemMapper.delete(
                Wrappers.<DepositDeductionItem>lambdaQuery()
                        .eq(DepositDeductionItem::getSnapshotId, snapshot.getId()));

        List<InspectionDtos.DeductionItemDetail> details = new ArrayList<>();
        int totalDeduction = 0;
        LocalDateTime now = LocalDateTime.now();

        for (InspectionDtos.DeductionItemRequest req : request.deductions()) {
            DepositDeductionItem item = new DepositDeductionItem();
            item.setContractId(contractId);
            item.setSnapshotId(snapshot.getId());
            item.setRoomCode(req.roomCode());
            item.setItemCode(req.itemCode());
            item.setResult(req.result());
            item.setReason(req.reason());
            item.setDeductionAmount(req.deductionAmount());
            item.setEvidenceUrls(serializeList(req.evidenceUrls()));
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            deductionItemMapper.insert(item);

            totalDeduction += req.deductionAmount();

            details.add(new InspectionDtos.DeductionItemDetail(
                    item.getId(), item.getRoomCode(), item.getItemCode(),
                    item.getResult(), item.getReason(), item.getDeductionAmount(),
                    req.evidenceUrls(), item.getTenantStatus(), item.getTenantDisputeReason()));
        }

        log.info("押金扣款已创建: contractId={}, snapshotId={}, totalDeduction={}, adminId={}",
                contractId, snapshot.getId(), totalDeduction, adminId);

        return new InspectionDtos.SettlementResponse(snapshot.getId(), totalDeduction, details);
    }

    // ==================== 押金结算查询 ====================

    @Override
    public InspectionDtos.SettlementResponse getTenantSettlement(String userId, String contractId) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);
        verifyContractUser(userId, contractId);
        return buildSettlementResponse(snapshot);
    }

    @Override
    public InspectionDtos.SettlementResponse getAdminSettlement(String contractId) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);
        return buildSettlementResponse(snapshot);
    }

    private InspectionDtos.SettlementResponse buildSettlementResponse(LeaseInspectionSnapshot snapshot) {
        List<DepositDeductionItem> items = deductionItemMapper.selectList(
                Wrappers.<DepositDeductionItem>lambdaQuery()
                        .eq(DepositDeductionItem::getSnapshotId, snapshot.getId()));

        int totalDeduction = 0;
        List<InspectionDtos.DeductionItemDetail> details = new ArrayList<>();
        for (DepositDeductionItem item : items) {
            totalDeduction += item.getDeductionAmount() != null ? item.getDeductionAmount() : 0;
            details.add(new InspectionDtos.DeductionItemDetail(
                    item.getId(), item.getRoomCode(), item.getItemCode(),
                    item.getResult(), item.getReason(), item.getDeductionAmount(),
                    deserializeStringList(item.getEvidenceUrls()),
                    item.getTenantStatus(), item.getTenantDisputeReason()));
        }
        return new InspectionDtos.SettlementResponse(snapshot.getId(), totalDeduction, details);
    }

    // ==================== 管理端锁定验房 ====================

    @Override
    @Transactional
    public void lockInspection(String adminId, String contractId, InspectionDtos.LockRequest request) {
        LeaseInspectionSnapshot snapshot = requireSnapshot(contractId);

        // 幂等：已锁定直接返回当前结果
        if (LeaseInspectionSnapshot.STATUS_LOCKED.equals(snapshot.getStatus())) {
            log.info("验房已锁定，幂等返回: contractId={}", contractId);
            return;
        }

        // 仅 SUBMITTED 状态可锁定
        if (!LeaseInspectionSnapshot.STATUS_SUBMITTED.equals(snapshot.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许锁定验房，当前状态: " + snapshot.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        snapshot.setStatus(LeaseInspectionSnapshot.STATUS_LOCKED);
        snapshot.setCompletedBy(adminId);
        snapshot.setCompletedAt(now);
        snapshot.setCompletionComment(request.comment());
        snapshot.setUpdatedAt(now);
        updateById(snapshot);

        log.info("验房已锁定（线下验房完成，照片已归档）: contractId={}, adminId={}", contractId, adminId);
    }

    // ==================== 私有工具方法 ====================

    private LeaseInspectionSnapshot requireSnapshot(String contractId) {
        LeaseInspectionSnapshot snapshot = getOne(
                Wrappers.<LeaseInspectionSnapshot>lambdaQuery()
                        .eq(LeaseInspectionSnapshot::getContractId, contractId)
                        .last("LIMIT 1"), false);
        if (snapshot == null) {
            throw BusinessException.notFound("该合同尚无验收快照，请先完成签约并确保房源已配置验收模板");
        }
        return snapshot;
    }

    /** 校验当前用户是合同的租客。 */
    private void verifyContractUser(String userId, String contractId) {
        RentContract contract = rentContractMapper.selectOne(
                Wrappers.<RentContract>lambdaQuery()
                        .eq(RentContract::getId, contractId)
                        .last("LIMIT 1"), false);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (!userId.equals(contract.getUserId())) {
            throw BusinessException.forbidden("无权操作该合同");
        }
    }

    /** 校验照片 URL 归属、itemCode 合法、满足必拍数量。 */
    private void validatePhotos(List<InspectionDtos.TemplateRoomItem> rooms,
                                 List<InspectionDtos.PhotoItem> photos,
                                 String userId, String expectedBizType) {
        // 构建合法 itemCode 集合
        Set<String> validItemCodes = new HashSet<>();
        for (InspectionDtos.TemplateRoomItem room : rooms) {
            for (InspectionDtos.TemplateCheckItem item : room.items()) {
                if (item.enabled()) {
                    validItemCodes.add(item.itemCode());
                }
            }
        }

        // 按 itemCode 分组统计
        Map<String, Long> photoCounts = photos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.roomCode() + "|" + p.itemCode(), Collectors.counting()));

        for (InspectionDtos.PhotoItem p : photos) {
            // 1. itemCode 必须存在于快照
            if (!validItemCodes.contains(p.itemCode())) {
                throw BusinessException.badRequest(
                        String.format("验收项「%s」不在当前快照中", p.itemCode()));
            }

            // 2. URL 必须属于当前用户上传（通过 FileRecord 校验）
            fileRecordService.validateFileOwnership(userId, p.url(), expectedBizType);
        }

        // 3. 必拍项满足 minPhotoCount
        for (InspectionDtos.TemplateRoomItem room : rooms) {
            for (InspectionDtos.TemplateCheckItem item : room.items()) {
                if (!item.enabled() || !item.required()) continue;

                String key = room.roomCode() + "|" + item.itemCode();
                long count = photoCounts.getOrDefault(key, 0L);
                if (count < item.minPhotoCount()) {
                    throw BusinessException.badRequest(
                            String.format("「%s - %s」至少需要 %d 张照片，当前仅 %d 张",
                                    room.roomName(), item.itemName(), item.minPhotoCount(), count));
                }
            }
        }
    }

    // ==================== 数据库访问 ====================

    private List<InspectionPhoto> getPhotoEntities(String contractId, String stage) {
        return inspectionPhotoMapper.selectList(
                Wrappers.<InspectionPhoto>lambdaQuery()
                        .eq(InspectionPhoto::getContractId, contractId)
                        .eq(InspectionPhoto::getStage, stage));
    }

    private List<InspectionDtos.PhotoItem> loadPhotos(String contractId, String stage) {
        return getPhotoEntities(contractId, stage).stream()
                .map(p -> new InspectionDtos.PhotoItem(
                        p.getRoomCode(), p.getItemCode(), p.getUrl(), p.getCapturedAt()))
                .collect(Collectors.toList());
    }

    private Map<String, List<String>> groupPhotoUrls(List<InspectionPhoto> photos) {
        return photos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getRoomCode() + "|" + p.getItemCode(),
                        Collectors.mapping(InspectionPhoto::getUrl, Collectors.toList())));
    }

    private void saveInspectionPhotos(String contractId, String userId, String stage,
                                       List<InspectionDtos.PhotoItem> photos) {
        LocalDateTime now = LocalDateTime.now();
        for (InspectionDtos.PhotoItem p : photos) {
            InspectionPhoto photo = new InspectionPhoto();
            photo.setContractId(contractId);
            photo.setRoomCode(p.roomCode());
            photo.setItemCode(p.itemCode());
            photo.setStage(stage);
            photo.setUrl(p.url());
            photo.setUserId(userId);
            photo.setCapturedAt(p.capturedAt());
            photo.setCreatedAt(now);
            inspectionPhotoMapper.insert(photo);
        }
    }

    // ==================== JSON 序列化 ====================

    private List<InspectionDtos.TemplateRoomItem> deserializeRooms(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<InspectionDtos.TemplateRoomItem>>() {});
        } catch (JsonProcessingException e) {
            log.error("反序列化验收 rooms 失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("序列化列表失败: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> deserializeStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("反序列化字符串列表失败: {}", e.getMessage());
            return List.of();
        }
    }
}
