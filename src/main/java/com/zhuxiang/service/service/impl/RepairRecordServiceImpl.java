package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.RepairDtos.AdminRepairItem;
import com.zhuxiang.service.dto.RepairDtos.AdminRepairDetail;
import com.zhuxiang.service.dto.RepairDtos.AssignRepairRequest;
import com.zhuxiang.service.dto.RepairDtos.CreateRepairRequest;
import com.zhuxiang.service.dto.RepairDtos.RepairItem;
import com.zhuxiang.service.dto.RepairDtos.TimelineItem;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.RepairLog;
import com.zhuxiang.service.entity.RepairRecord;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.RepairLogMapper;
import com.zhuxiang.service.mapper.RepairRecordMapper;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.RepairRecordService;
import com.zhuxiang.service.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RepairRecordServiceImpl extends ServiceImpl<RepairRecordMapper, RepairRecord>
        implements RepairRecordService {

    private static final Map<String, String> REPAIR_TYPE_TEXT = Map.ofEntries(
            Map.entry("plumbing", "水管维修"),
            Map.entry("electrical", "电路维修"),
            Map.entry("appliance", "家电维修"),
            Map.entry("furniture", "家具维修"),
            Map.entry("door_window", "门窗维修"),
            Map.entry("other", "其他")
    );

    private static final Map<String, String> STATUS_TITLE = Map.ofEntries(
            Map.entry("submitted", "已提交"),
            Map.entry("accepted", "已受理"),
            Map.entry("assigned", "已分派"),
            Map.entry("processing", "处理中"),
            Map.entry("pendingReview", "待评价"),
            Map.entry("completed", "已完成"),
            Map.entry("cancelled", "已取消")
    );
    private static final Set<String> REPAIR_STATUSES = STATUS_TITLE.keySet();

    private static final String DEFAULT_HOUSEKEEPER_NAME = "小住管家";
    private static final String DEFAULT_HOUSEKEEPER_PHONE = "400-800-1234";

    private final RepairLogMapper repairLogMapper;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final HouseService houseService;

    public RepairRecordServiceImpl(RepairLogMapper repairLogMapper, ObjectMapper objectMapper,
                                   UserService userService, HouseService houseService) {
        this.repairLogMapper = repairLogMapper;
        this.objectMapper = objectMapper;
        this.userService = userService;
        this.houseService = houseService;
    }

    @Override
    @Transactional
    public String createRepair(String userId, CreateRepairRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String id = UUID.randomUUID().toString();

        RepairRecord record = new RepairRecord();
        record.setId(id);
        record.setOrderNo(generateOrderNo());
        record.setUserId(userId);
        record.setHouseId(request.houseId());
        record.setHouseName(request.houseName());
        record.setRoomName(request.roomName());
        record.setRepairType(request.repairType());
        record.setDescription(request.description());
        record.setImageUrls(serializeImageUrls(request.imageUrls()));
        record.setContactName(request.contactName());
        record.setContactPhone(request.contactPhone());
        record.setExpectedVisitTime(request.expectedVisitTime());
        record.setStatus("submitted");
        record.setHousekeeperName(DEFAULT_HOUSEKEEPER_NAME);
        record.setHousekeeperPhone(DEFAULT_HOUSEKEEPER_PHONE);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        save(record);

        writeLog(id, "已提交", "用户提交报修", "submitted", now);

        return id;
    }

    @Override
    public RepairItem getRepairDetail(String userId, String repairId) {
        RepairRecord record = getById(repairId);
        if (record == null || record.getDeletedAt() != null) {
            throw BusinessException.notFound("报修记录不存在");
        }
        if (!userId.equals(record.getUserId())) {
            throw BusinessException.forbidden("无权查看该报修记录");
        }
        return toItem(record);
    }

    @Override
    public PageData<RepairItem> listMyRepairs(String userId, long page, long pageSize) {
        var result = page(
                new Page<>(page, pageSize),
                Wrappers.<RepairRecord>lambdaQuery()
                        .eq(RepairRecord::getUserId, userId)
                        .isNull(RepairRecord::getDeletedAt)
                        .orderByDesc(RepairRecord::getCreatedAt)
        );

        List<RepairItem> items = result.getRecords().stream()
                .map(this::toItem)
                .toList();

        return PageData.of(items, page, pageSize, result.getTotal());
    }

    @Override
    @Transactional
    public void cancelRepair(String userId, String repairId, String cancelReason) {
        RepairRecord record = getOwnedRecord(userId, repairId);

        if (!"submitted".equals(record.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许取消报修");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setStatus("cancelled");
        record.setCancelReason(cancelReason);
        record.setCancelTime(now);
        record.setUpdatedAt(now);
        updateById(record);

        writeLog(repairId, "已取消", cancelReason != null ? cancelReason : "用户取消报修", "cancelled", now);
    }

    @Override
    @Transactional
    public void reviewRepair(String userId, String repairId, Integer rating, String reviewContent) {
        RepairRecord record = getOwnedRecord(userId, repairId);

        if (!"pendingReview".equals(record.getStatus())) {
            throw BusinessException.badRequest("当前状态不允许评价");
        }

        LocalDateTime now = LocalDateTime.now();
        record.setStatus("completed");
        record.setRating(rating);
        record.setReviewContent(reviewContent);
        record.setReviewTime(now);
        if (record.getCompletedTime() == null) {
            record.setCompletedTime(now);
        }
        record.setUpdatedAt(now);
        updateById(record);

        writeLog(repairId, "已完成", "用户评价完成", "completed", now);
    }

    @Override
    public PageData<AdminRepairItem> listAdminRepairs(
            String operatorId, String keyword, String status, long page, long pageSize
    ) {
        User operator = requireAdminOperator(operatorId);
        Set<String> accessibleHouseIds = accessibleHouseIds(operator);
        if (accessibleHouseIds != null && accessibleHouseIds.isEmpty()) {
            return PageData.of(List.of(), page, pageSize, 0);
        }
        String normalizedStatus = normalizeStatus(status);
        var query = Wrappers.<RepairRecord>lambdaQuery()
                .isNull(RepairRecord::getDeletedAt)
                .in(accessibleHouseIds != null, RepairRecord::getHouseId, accessibleHouseIds)
                .orderByDesc(RepairRecord::getCreatedAt);

        if (normalizedStatus != null) {
            query.eq(RepairRecord::getStatus, normalizedStatus);
        }

        if (keyword != null && !keyword.isBlank()) {
            List<String> userIds = userService.list(Wrappers.<User>lambdaQuery()
                            .like(User::getNickname, keyword)
                            .or().like(User::getPhone, keyword))
                    .stream().map(User::getId).toList();

            List<String> houseIds = houseService.list(Wrappers.<House>lambdaQuery()
                            .like(House::getAddress, keyword))
                    .stream().map(House::getId).toList();

            query.and(w -> {
                w.like(RepairRecord::getOrderNo, keyword)
                        .or().like(RepairRecord::getHouseName, keyword)
                        .or().like(RepairRecord::getDescription, keyword)
                        .or().like(RepairRecord::getContactName, keyword)
                        .or().like(RepairRecord::getContactPhone, keyword);
                if (!userIds.isEmpty()) {
                    w.or().in(RepairRecord::getUserId, userIds);
                }
                if (!houseIds.isEmpty()) {
                    w.or().in(RepairRecord::getHouseId, houseIds);
                }
            });
        }

        var result = page(new Page<>(page, pageSize), query);
        List<AdminRepairItem> items = toAdminItems(result.getRecords());

        return PageData.of(items, page, pageSize, result.getTotal());
    }

    @Override
    public AdminRepairDetail getAdminRepairDetail(String operatorId, String repairId) {
        RepairRecord record = requireAdminRecord(repairId);
        ensureAdminAccessible(requireAdminOperator(operatorId), record);
        return toAdminDetail(record);
    }

    @Override
    @Transactional
    public AdminRepairDetail acceptAdminRepair(String operatorId, String repairId) {
        RepairRecord record = requireAdminRecord(repairId);
        ensureAdminAccessible(requireAdminOperator(operatorId), record);
        requireStatus(record, Set.of("submitted"), "当前状态不能受理");
        changeStatus(record, "submitted", "accepted", "已受理", "管理端已受理报修");
        return toAdminDetail(record);
    }

    @Override
    @Transactional
    public AdminRepairDetail assignAdminRepair(
            String operatorId, String repairId, AssignRepairRequest request
    ) {
        RepairRecord record = requireAdminRecord(repairId);
        ensureAdminAccessible(requireAdminOperator(operatorId), record);
        requireStatus(record, Set.of("accepted"), "当前状态不能派单");
        String assignee = request.assignee().trim();
        String repairmanName = request.repairmanName() == null || request.repairmanName().isBlank()
                ? request.assignee().trim() : request.repairmanName().trim();
        LocalDateTime now = LocalDateTime.now();
        int updated = getBaseMapper().assignIfCurrent(
                record.getId(), assignee, repairmanName, now
        );
        requireUpdated(updated, "当前状态不能派单");
        record.setAssignee(assignee);
        record.setRepairmanName(repairmanName);
        record.setStatus("assigned");
        record.setUpdatedAt(now);
        writeLog(record.getId(), "已分派", "已分派给 " + assignee, "assigned", now);
        return toAdminDetail(record);
    }

    @Override
    @Transactional
    public AdminRepairDetail startAdminRepair(String operatorId, String repairId) {
        RepairRecord record = requireAdminRecord(repairId);
        ensureAdminAccessible(requireAdminOperator(operatorId), record);
        requireStatus(record, Set.of("assigned"), "当前状态不能开始处理");
        changeStatus(record, "assigned", "processing", "处理中", "维修人员已开始处理");
        return toAdminDetail(record);
    }

    @Override
    @Transactional
    public AdminRepairDetail finishAdminRepair(String operatorId, String repairId) {
        RepairRecord record = requireAdminRecord(repairId);
        ensureAdminAccessible(requireAdminOperator(operatorId), record);
        requireStatus(record, Set.of("processing"), "当前状态不能完成维修");
        LocalDateTime now = LocalDateTime.now();
        requireUpdated(getBaseMapper().finishIfProcessing(record.getId(), now), "当前状态不能完成维修");
        record.setCompletedTime(now);
        record.setStatus("pendingReview");
        record.setUpdatedAt(now);
        writeLog(record.getId(), "待评价", "维修已完成，等待用户评价", "pendingReview", now);
        return toAdminDetail(record);
    }

    private List<AdminRepairItem> toAdminItems(List<RepairRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<String, User> users = byId(userService.listByIds(ids(records.stream()
                .map(RepairRecord::getUserId).toList())));
        Map<String, House> houses = byId(houseService.listByIds(ids(records.stream()
                .map(RepairRecord::getHouseId).toList())));
        return records.stream().map(r -> toAdminItem(r, users.get(r.getUserId()), houses.get(r.getHouseId()))).toList();
    }

    private AdminRepairItem toAdminItem(RepairRecord r, User user, House house) {
        return new AdminRepairItem(
                r.getId(), r.getOrderNo(), r.getHouseId(), r.getHouseName(),
                house != null ? house.getAddress() : null, r.getRoomName(), r.getUserId(),
                user != null ? user.getNickname() : r.getContactName(),
                user != null ? user.getPhone() : r.getContactPhone(),
                r.getRepairType(), r.getDescription(), r.getStatus(), r.getAssignee(),
                r.getRepairmanName(), r.getHousekeeperName(), r.getExpectedVisitTime(),
                r.getCompletedTime(), r.getRating(), r.getReviewContent(), r.getCreatedAt(), r.getUpdatedAt()
        );
    }

    private AdminRepairDetail toAdminDetail(RepairRecord r) {
        User user = userService.getById(r.getUserId());
        House house = houseService.getById(r.getHouseId());
        return new AdminRepairDetail(
                r.getId(), r.getOrderNo(), r.getHouseId(), r.getHouseName(),
                house == null ? null : house.getAddress(), r.getRoomName(), r.getUserId(),
                user == null ? r.getContactName() : user.getNickname(),
                user == null ? r.getContactPhone() : user.getPhone(),
                r.getRepairType(), r.getDescription(), deserializeImageUrls(r.getImageUrls()),
                r.getContactName(), r.getContactPhone(), r.getExpectedVisitTime(), r.getStatus(),
                r.getAssignee(), r.getRepairmanName(), r.getHousekeeperName(), r.getHousekeeperPhone(),
                r.getCompletedTime(), r.getRating(), r.getReviewContent(), r.getCancelReason(),
                r.getCancelTime(), r.getCreatedAt(), r.getUpdatedAt(), getTimeline(r.getId()),
                availableActions(r.getStatus())
        );
    }

    private RepairRecord requireAdminRecord(String repairId) {
        RepairRecord record = getById(repairId);
        if (record == null || record.getDeletedAt() != null) {
            throw BusinessException.notFound("报修记录不存在");
        }
        return record;
    }

    private void requireStatus(RepairRecord record, Set<String> allowed, String message) {
        if (!allowed.contains(record.getStatus())) {
            throw BusinessException.conflict(message);
        }
    }

    private void changeStatus(
            RepairRecord record, String expectedStatus, String status, String title, String description
    ) {
        LocalDateTime now = LocalDateTime.now();
        requireUpdated(
                getBaseMapper().updateStatusIfCurrent(record.getId(), expectedStatus, status, now),
                "报修状态已变化，请刷新后重试"
        );
        record.setStatus(status);
        record.setUpdatedAt(now);
        writeLog(record.getId(), title, description, status, now);
    }

    private void requireUpdated(int updated, String message) {
        if (updated != 1) throw BusinessException.conflict(message);
    }

    private User requireAdminOperator(String operatorId) {
        User operator = userService.requireActiveUser(operatorId);
        if (!Set.of("ADMIN", "HOUSEKEEPER", "LANDLORD").contains(operator.getRole())) {
            throw BusinessException.forbidden("无权处理管理端报修");
        }
        return operator;
    }

    private Set<String> accessibleHouseIds(User operator) {
        if (!"LANDLORD".equals(operator.getRole())) return null;
        return houseService.list(
                Wrappers.<House>lambdaQuery()
                        .select(House::getId)
                        .eq(House::getLandlordId, operator.getId())
        ).stream().map(House::getId).collect(Collectors.toSet());
    }

    private void ensureAdminAccessible(User operator, RepairRecord record) {
        if (!"LANDLORD".equals(operator.getRole())) return;
        House house = houseService.getById(record.getHouseId());
        if (house == null || !operator.getId().equals(house.getLandlordId())) {
            throw BusinessException.forbidden("无权查看或处理该报修");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim();
        String canonical = REPAIR_STATUSES.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).equals(normalized.toLowerCase(Locale.ROOT)))
                .findFirst().orElse(null);
        if (canonical == null) throw BusinessException.badRequest("不支持的报修状态");
        return canonical;
    }

    private List<String> availableActions(String status) {
        return switch (status) {
            case "submitted" -> List.of("accept");
            case "accepted" -> List.of("assign");
            case "assigned" -> List.of("start");
            case "processing" -> List.of("finish");
            default -> List.of();
        };
    }

    private Set<String> ids(Collection<String> values) {
        return values.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private <T> Map<String, T> byId(List<T> values) {
        if (values == null || values.isEmpty()) return Collections.emptyMap();
        if (values.getFirst() instanceof User) {
            @SuppressWarnings("unchecked") Map<String, T> result = (Map<String, T>) values.stream()
                    .map(User.class::cast).collect(Collectors.toMap(User::getId, Function.identity()));
            return result;
        }
        @SuppressWarnings("unchecked") Map<String, T> result = (Map<String, T>) values.stream()
                .map(House.class::cast).collect(Collectors.toMap(House::getId, Function.identity()));
        return result;
    }

    private RepairRecord getOwnedRecord(String userId, String repairId) {
        RepairRecord record = getById(repairId);
        if (record == null || record.getDeletedAt() != null) {
            throw BusinessException.notFound("报修记录不存在");
        }
        if (!userId.equals(record.getUserId())) {
            throw BusinessException.forbidden("无权操作该报修记录");
        }
        return record;
    }

    private RepairItem toItem(RepairRecord r) {
        return new RepairItem(
                r.getId(), r.getOrderNo(), r.getHouseId(),
                r.getHouseName(), r.getRoomName(),
                r.getRepairType(), r.getDescription(),
                deserializeImageUrls(r.getImageUrls()),
                r.getContactName(), r.getContactPhone(),
                r.getExpectedVisitTime(),
                r.getStatus(),
                r.getHousekeeperName(), r.getHousekeeperPhone(),
                r.getRepairmanName(),
                r.getCreatedAt(), r.getUpdatedAt(),
                getTimeline(r.getId())
        );
    }

    private List<TimelineItem> getTimeline(String repairId) {
        List<RepairLog> logs = repairLogMapper.selectList(Wrappers.<RepairLog>lambdaQuery()
                .eq(RepairLog::getRepairId, repairId)
                .orderByAsc(RepairLog::getCreatedAt));
        return logs.stream()
                .map(log -> new TimelineItem(
                        log.getTitle(),
                        log.getDescription(),
                        log.getCreatedAt(),
                        log.getStatus()
                ))
                .toList();
    }

    private void writeLog(String repairId, String title, String description, String status, LocalDateTime now) {
        RepairLog log = new RepairLog();
        log.setId(UUID.randomUUID().toString());
        log.setRepairId(repairId);
        log.setTitle(title);
        log.setDescription(description);
        log.setStatus(status);
        log.setCreatedAt(now);
        repairLogMapper.insert(log);
    }

    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = count(Wrappers.<RepairRecord>lambdaQuery()
                .ge(RepairRecord::getCreatedAt, LocalDate.now().atStartOfDay()));
        return "BX" + date + String.format("%04d", count + 1);
    }

    private String serializeImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化图片URL失败", e);
        }
    }

    private List<String> deserializeImageUrls(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
