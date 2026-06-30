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
import java.util.Map;
import java.util.UUID;

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
        record.setCompletedTime(now);
        record.setUpdatedAt(now);
        updateById(record);

        writeLog(repairId, "已完成", "用户评价完成", "completed", now);
    }

    @Override
    public PageData<AdminRepairItem> listAdminRepairs(String keyword, String status, long page, long pageSize) {
        var query = Wrappers.<RepairRecord>lambdaQuery()
                .isNull(RepairRecord::getDeletedAt)
                .orderByDesc(RepairRecord::getCreatedAt);

        if (status != null && !status.isBlank()) {
            query.eq(RepairRecord::getStatus, status);
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
        List<AdminRepairItem> items = result.getRecords().stream()
                .map(this::toAdminItem)
                .toList();

        return PageData.of(items, page, pageSize, result.getTotal());
    }

    private AdminRepairItem toAdminItem(RepairRecord r) {
        User user = userService.getById(r.getUserId());
        House house = houseService.getById(r.getHouseId());

        return new AdminRepairItem(
                r.getId(),
                r.getOrderNo(),
                r.getHouseId(),
                r.getHouseName(),
                house != null ? house.getAddress() : null,
                r.getRoomName(),
                r.getUserId(),
                user != null ? user.getNickname() : r.getContactName(),
                user != null ? user.getPhone() : r.getContactPhone(),
                r.getRepairType(),
                r.getDescription(),
                r.getStatus(),
                r.getAssignee(),
                r.getRepairmanName(),
                r.getHousekeeperName(),
                r.getExpectedVisitTime(),
                r.getCompletedTime(),
                r.getRating(),
                r.getReviewContent(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
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
