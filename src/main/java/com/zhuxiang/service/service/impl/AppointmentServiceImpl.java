package com.zhuxiang.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhuxiang.service.common.AppointmentAccessStatus;
import com.zhuxiang.service.common.AppointmentStatus;
import com.zhuxiang.service.common.BusinessException;
import com.zhuxiang.service.common.HouseSourceType;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.common.ViewingMode;
import com.zhuxiang.service.dto.AppointmentDtos;
import com.zhuxiang.service.entity.Appointment;
import com.zhuxiang.service.entity.AppointmentAccessGrant;
import com.zhuxiang.service.entity.AppointmentStatusLog;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.HouseViewingConfig;
import com.zhuxiang.service.entity.SmartLock;
import com.zhuxiang.service.entity.User;
import com.zhuxiang.service.mapper.AppointmentAccessGrantMapper;
import com.zhuxiang.service.mapper.AppointmentMapper;
import com.zhuxiang.service.mapper.AppointmentStatusLogMapper;
import com.zhuxiang.service.mapper.HouseViewingConfigMapper;
import com.zhuxiang.service.mapper.SmartLockMapper;
import com.zhuxiang.service.security.AppointmentCheckinCodeService;
import com.zhuxiang.service.service.AppointmentService;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.MessageService;
import com.zhuxiang.service.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment>
        implements AppointmentService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ZoneOffset BUSINESS_OFFSET = ZoneOffset.ofHours(8);
    private static final int DEFAULT_DURATION_MINUTES = 60;
    private static final int DEFAULT_ADVANCE_MINUTES = 30;
    private static final int DEFAULT_ADVANCE_DAYS = 14;
    private static final int DEFAULT_CONFIRM_TIMEOUT_MINUTES = 120;
    private static final int DEFAULT_RESCHEDULE_TIMEOUT_MINUTES = 120;
    private static final List<LocalTime> DEFAULT_START_TIMES = List.of(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
            LocalTime.of(17, 0),
            LocalTime.of(18, 0)
    );
    private static final Set<AppointmentStatus> ACTIVE_SLOT_STATUSES = EnumSet.of(
            AppointmentStatus.PENDING_CONFIRMATION,
            AppointmentStatus.RESCHEDULE_PROPOSED,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.READY,
            AppointmentStatus.IN_PROGRESS
    );

    private final HouseService houseService;
    private final UserService userService;
    private final SmartLockMapper smartLockMapper;
    private final HouseViewingConfigMapper viewingConfigMapper;
    private final AppointmentAccessGrantMapper accessGrantMapper;
    private final AppointmentStatusLogMapper statusLogMapper;
    private final MessageService messageService;
    private final AppointmentCheckinCodeService checkinCodeService;

    public AppointmentServiceImpl(
            HouseService houseService,
            UserService userService,
            SmartLockMapper smartLockMapper,
            HouseViewingConfigMapper viewingConfigMapper,
            AppointmentAccessGrantMapper accessGrantMapper,
            AppointmentStatusLogMapper statusLogMapper,
            MessageService messageService,
            AppointmentCheckinCodeService checkinCodeService
    ) {
        this.houseService = houseService;
        this.userService = userService;
        this.smartLockMapper = smartLockMapper;
        this.viewingConfigMapper = viewingConfigMapper;
        this.accessGrantMapper = accessGrantMapper;
        this.statusLogMapper = statusLogMapper;
        this.messageService = messageService;
        this.checkinCodeService = checkinCodeService;
    }

    @Override
    @Transactional
    public AppointmentDtos.CreateResult createAppointment(
            String userId,
            String idempotencyKey,
            AppointmentDtos.CreateRequest request
    ) {
        User tenant = userService.requireActiveUser(userId);
        String normalizedIdempotencyKey = trimToNull(idempotencyKey);
        if (normalizedIdempotencyKey != null && normalizedIdempotencyKey.length() > 100) {
            throw BusinessException.badRequest("Idempotency-Key 不能超过 100 个字符");
        }
        if (normalizedIdempotencyKey != null) {
            Appointment existing = getOne(
                    Wrappers.<Appointment>lambdaQuery()
                            .eq(Appointment::getUserId, tenant.getId())
                            .eq(Appointment::getIdempotencyKey, normalizedIdempotencyKey)
                            .last("LIMIT 1")
            );
            if (existing != null) {
                return toCreateResult(existing);
            }
        }
        House house = houseService.requireAvailableHouse(request.houseId());
        HouseViewingConfig config = viewingConfigMapper.selectById(house.getId());
        requireViewingEnabled(config);
        ViewingMode mode = resolveViewingMode(house, config);
        TimeWindow window = resolveCreateWindow(request, config);
        validateWindow(window, config);

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID().toString());
        appointment.setUserId(tenant.getId());
        appointment.setHouseId(house.getId());
        appointment.setLandlordId(house.getLandlordId());
        appointment.setSourceType(normalizeSourceType(house.getSourceType()));
        appointment.setViewingMode(mode.name());
        appointment.setAppointmentDate(window.start().toLocalDate());
        appointment.setAppointmentStartAt(window.start());
        appointment.setAppointmentEndAt(window.end());
        appointment.setTimeSlot(formatTimeSlot(window));
        appointment.setContactName(request.contactName().trim());
        appointment.setContactPhone(request.contactPhone().trim());
        appointment.setRemark(trimToNull(request.remark()));
        appointment.setVersion(0);
        appointment.setCreatedAt(LocalDateTime.now());
        appointment.setUpdatedAt(LocalDateTime.now());
        appointment.setActiveSlotKey(buildActiveSlotKey(house.getId(), window));
        appointment.setIdempotencyKey(normalizedIdempotencyKey);

        if (mode == ViewingMode.SELF_SERVICE_LOCK) {
            appointment.setStatus(AppointmentStatus.CONFIRMED.name());
            appointment.setConfirmedBy("SYSTEM");
            appointment.setConfirmedAt(LocalDateTime.now());
        } else {
            appointment.setStatus(AppointmentStatus.PENDING_CONFIRMATION.name());
            appointment.setConfirmDeadlineAt(resolveDeadline(
                    window.start(),
                    configValue(config == null ? null : config.getConfirmationTimeoutMinutes(),
                            DEFAULT_CONFIRM_TIMEOUT_MINUTES)
            ));
        }

        try {
            save(appointment);
        } catch (DuplicateKeyException exception) {
            if (normalizedIdempotencyKey != null) {
                Appointment existing = getOne(
                        Wrappers.<Appointment>lambdaQuery()
                                .eq(Appointment::getUserId, tenant.getId())
                                .eq(Appointment::getIdempotencyKey, normalizedIdempotencyKey)
                                .last("LIMIT 1")
                );
                if (existing != null) {
                    return toCreateResult(existing);
                }
            }
            throw BusinessException.conflict("该看房时段已被预约，请选择其他时间");
        }
        writeStatusLog(
                appointment.getId(), null, appointment.getStatus(),
                userId, "TENANT", "创建预约"
        );
        notifyCreated(appointment, house);
        return toCreateResult(appointment);
    }

    @Override
    public AppointmentDtos.ViewingSlotResult getViewingSlots(
            String houseId,
            LocalDate startDate,
            int days
    ) {
        House house = houseService.requireAvailableHouse(houseId);
        HouseViewingConfig config = viewingConfigMapper.selectById(houseId);
        requireViewingEnabled(config);
        ViewingMode mode = resolveViewingMode(house, config);
        LocalDate firstDate = startDate == null ? LocalDate.now(BUSINESS_ZONE) : startDate;
        int safeDays = Math.max(1, Math.min(days, 14));
        int duration = configValue(
                config == null ? null : config.getDurationMinutes(),
                DEFAULT_DURATION_MINUTES
        );
        int advanceMinutes = configValue(
                config == null ? null : config.getAdvanceMinMinutes(),
                DEFAULT_ADVANCE_MINUTES
        );
        LocalDateTime earliest = LocalDateTime.now(BUSINESS_ZONE).plusMinutes(advanceMinutes);
        List<Appointment> active = list(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getHouseId, houseId)
                        .in(Appointment::getStatus, activeStatusNames())
                        .ge(Appointment::getAppointmentStartAt, firstDate.atStartOfDay())
                        .lt(Appointment::getAppointmentStartAt, firstDate.plusDays(safeDays).atStartOfDay())
        );

        List<AppointmentDtos.ViewingSlotDate> dates = new ArrayList<>();
        for (int dayIndex = 0; dayIndex < safeDays; dayIndex++) {
            LocalDate date = firstDate.plusDays(dayIndex);
            List<AppointmentDtos.ViewingSlot> slots = new ArrayList<>();
            for (LocalTime time : DEFAULT_START_TIMES) {
                LocalDateTime start = date.atTime(time);
                LocalDateTime end = start.plusMinutes(duration);
                boolean occupied = active.stream().anyMatch(item ->
                        start.equals(item.getAppointmentStartAt())
                                && end.equals(item.getAppointmentEndAt())
                );
                boolean available = start.isAfter(earliest)
                        && !occupied
                        && date.getDayOfWeek() != DayOfWeek.SUNDAY;
                slots.add(new AppointmentDtos.ViewingSlot(
                        toOffset(start), toOffset(end), available
                ));
            }
            dates.add(new AppointmentDtos.ViewingSlotDate(date, slots));
        }
        return new AppointmentDtos.ViewingSlotResult(
                houseId,
                mode.name(),
                mode != ViewingMode.SELF_SERVICE_LOCK,
                dates
        );
    }

    @Override
    public PageData<AppointmentDtos.Summary> listMyAppointments(
            String userId, String status, long page, long pageSize
    ) {
        return listAppointments(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getUserId, userId)
                        .eq(StringUtils.hasText(status), Appointment::getStatus, normalizeStatusFilter(status))
                        .orderByDesc(Appointment::getAppointmentStartAt),
                page, pageSize, Perspective.TENANT
        );
    }

    @Override
    public AppointmentDtos.Detail getMyAppointment(String userId, String appointmentId) {
        return toDetail(requireOwnedAppointment(userId, appointmentId), Perspective.TENANT);
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail cancelMyAppointment(
            String userId, String appointmentId, String reason
    ) {
        Appointment appointment = requireOwnedAppointment(userId, appointmentId);
        AppointmentStatus current = AppointmentStatus.from(appointment.getStatus());
        if (current.isTerminal() || current == AppointmentStatus.IN_PROGRESS) {
            throw BusinessException.conflict("当前预约状态不能取消");
        }
        appointment.setCancelReason(trimToNull(reason));
        appointment.setCancelledBy(userId);
        appointment.setCancelledAt(LocalDateTime.now());
        transition(appointment, AppointmentStatus.CANCELLED, userId, "TENANT", reason);
        notifyHost(appointment, "租客已取消看房预约", "租客取消了预约，请查看详情");
        return toDetail(appointment, Perspective.TENANT);
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail acceptMyReschedule(String userId, String appointmentId) {
        Appointment appointment = requireOwnedAppointment(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.RESCHEDULE_PROPOSED);
        if (appointment.getRescheduleDeadlineAt() != null
                && appointment.getRescheduleDeadlineAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.conflict("改期建议已失效");
        }
        TimeWindow proposed = requireProposedWindow(appointment);
        appointment.setAppointmentStartAt(proposed.start());
        appointment.setAppointmentEndAt(proposed.end());
        appointment.setAppointmentDate(proposed.start().toLocalDate());
        appointment.setTimeSlot(formatTimeSlot(proposed));
        appointment.setActiveSlotKey(buildActiveSlotKey(appointment.getHouseId(), proposed));
        appointment.setProposedStartAt(null);
        appointment.setProposedEndAt(null);
        appointment.setRescheduleDeadlineAt(null);
        transition(
                appointment, AppointmentStatus.CONFIRMED,
                userId, "TENANT", "接受改期"
        );
        notifyHost(appointment, "租客已接受改期", "租客已接受新的预约时间");
        return toDetail(appointment, Perspective.TENANT);
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail rejectMyReschedule(
            String userId, String appointmentId, String reason
    ) {
        Appointment appointment = requireOwnedAppointment(userId, appointmentId);
        requireStatus(appointment, AppointmentStatus.RESCHEDULE_PROPOSED);
        appointment.setCancelReason(trimToNull(reason));
        appointment.setCancelledBy(userId);
        appointment.setCancelledAt(LocalDateTime.now());
        transition(
                appointment, AppointmentStatus.CANCELLED,
                userId, "TENANT", reason
        );
        notifyHost(appointment, "租客未接受改期", "租客拒绝了新的预约时间");
        return toDetail(appointment, Perspective.TENANT);
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail recordUnlockAttempt(
            String userId,
            String appointmentId,
            AppointmentDtos.UnlockAttemptRequest request
    ) {
        Appointment appointment = requireOwnedAppointment(userId, appointmentId);
        if (!ViewingMode.SELF_SERVICE_LOCK.name().equals(appointment.getViewingMode())) {
            throw BusinessException.forbidden("该预约不是智能锁自助看房");
        }
        AppointmentStatus current = AppointmentStatus.from(appointment.getStatus());
        if (current != AppointmentStatus.READY && current != AppointmentStatus.IN_PROGRESS) {
            throw BusinessException.conflict("当前预约状态不能上报开锁结果");
        }
        if (request.success() && current == AppointmentStatus.READY) {
            appointment.setCheckedInAt(LocalDateTime.now());
            transition(
                    appointment,
                    AppointmentStatus.IN_PROGRESS,
                    userId,
                    "TENANT",
                    request.method() + "开锁成功"
            );
        }
        return toDetail(appointment, Perspective.TENANT);
    }

    @Override
    public PageData<AppointmentDtos.Summary> listLandlordAppointments(
            String landlordId, String status, long page, long pageSize
    ) {
        return listAppointments(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getLandlordId, landlordId)
                        .eq(Appointment::getViewingMode, ViewingMode.LANDLORD_HOSTED.name())
                        .eq(StringUtils.hasText(status), Appointment::getStatus, normalizeStatusFilter(status))
                        .orderByDesc(Appointment::getAppointmentStartAt),
                page, pageSize, Perspective.LANDLORD
        );
    }

    @Override
    public AppointmentDtos.Detail getLandlordAppointment(
            String landlordId, String appointmentId
    ) {
        return toDetail(
                requireLandlordAppointment(landlordId, appointmentId),
                Perspective.LANDLORD
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail confirmLandlordAppointment(
            String landlordId,
            String appointmentId,
            AppointmentDtos.ConfirmRequest request
    ) {
        Appointment appointment = requireLandlordAppointment(landlordId, appointmentId);
        return confirmHosted(
                appointment, landlordId, "LANDLORD",
                request == null ? null : request.meetingPoint(),
                request == null ? null : request.viewingInstruction(),
                landlordId, Perspective.LANDLORD
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail rejectLandlordAppointment(
            String landlordId, String appointmentId, String reason
    ) {
        Appointment appointment = requireLandlordAppointment(landlordId, appointmentId);
        return rejectHosted(
                appointment, landlordId, "LANDLORD", reason, Perspective.LANDLORD
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail rescheduleLandlordAppointment(
            String landlordId,
            String appointmentId,
            AppointmentDtos.RescheduleRequest request
    ) {
        Appointment appointment = requireLandlordAppointment(landlordId, appointmentId);
        return proposeReschedule(
                appointment, landlordId, "LANDLORD", request, Perspective.LANDLORD
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail checkInLandlordAppointment(
            String landlordId, String appointmentId, String checkinCode
    ) {
        Appointment appointment = requireLandlordAppointment(landlordId, appointmentId);
        if (!checkinCodeService.matches(appointment.getId(), checkinCode)) {
            throw BusinessException.badRequest("预约核验码错误");
        }
        AppointmentStatus current = AppointmentStatus.from(appointment.getStatus());
        if (current != AppointmentStatus.CONFIRMED && current != AppointmentStatus.READY) {
            throw BusinessException.conflict("当前预约状态不能签到");
        }
        appointment.setCheckedInAt(LocalDateTime.now());
        transition(
                appointment, AppointmentStatus.IN_PROGRESS,
                landlordId, "LANDLORD", "房东核验到场"
        );
        return toDetail(appointment, Perspective.LANDLORD);
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail completeLandlordAppointment(
            String landlordId, String appointmentId
    ) {
        Appointment appointment = requireLandlordAppointment(landlordId, appointmentId);
        return complete(
                appointment, landlordId, "LANDLORD", Perspective.LANDLORD
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail markLandlordNoShow(
            String landlordId, String appointmentId
    ) {
        Appointment appointment = requireLandlordAppointment(landlordId, appointmentId);
        return noShow(
                appointment, landlordId, "LANDLORD", Perspective.LANDLORD
        );
    }

    @Override
    public PageData<AppointmentDtos.Summary> listAdminAppointments(
            String appointmentId,
            String houseId,
            String tenantKeyword,
            String landlordId,
            String status,
            String sourceType,
            String viewingMode,
            LocalDate startDate,
            LocalDate endDate,
            long page,
            long pageSize
    ) {
        LambdaQueryWrapper<Appointment> query = Wrappers.<Appointment>lambdaQuery()
                .eq(StringUtils.hasText(appointmentId), Appointment::getId, appointmentId)
                .eq(StringUtils.hasText(houseId), Appointment::getHouseId, houseId)
                .eq(StringUtils.hasText(landlordId), Appointment::getLandlordId, landlordId)
                .and(StringUtils.hasText(tenantKeyword), item -> item
                        .like(Appointment::getContactName, tenantKeyword)
                        .or()
                        .like(Appointment::getContactPhone, tenantKeyword))
                .eq(StringUtils.hasText(status), Appointment::getStatus, normalizeStatusFilter(status))
                .eq(StringUtils.hasText(sourceType), Appointment::getSourceType,
                        sourceType == null ? null : sourceType.toUpperCase(Locale.ROOT))
                .eq(StringUtils.hasText(viewingMode), Appointment::getViewingMode,
                        viewingMode == null ? null : viewingMode.toUpperCase(Locale.ROOT))
                .ge(startDate != null, Appointment::getAppointmentStartAt,
                        startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, Appointment::getAppointmentStartAt,
                        endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(Appointment::getAppointmentStartAt);
        return listAppointments(query, page, pageSize, Perspective.ADMIN);
    }

    @Override
    public AppointmentDtos.Detail getAdminAppointment(String appointmentId) {
        return toDetail(requireAppointment(appointmentId), Perspective.ADMIN);
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail confirmAdminAppointment(
            String operatorId,
            String appointmentId,
            AppointmentDtos.ConfirmRequest request
    ) {
        Appointment appointment = requirePlatformHostedAppointment(appointmentId);
        String hostUserId = request != null && StringUtils.hasText(request.hostUserId())
                ? request.hostUserId()
                : operatorId;
        User host = userService.requireActiveUser(hostUserId);
        if (!Set.of("ADMIN", "HOUSEKEEPER").contains(host.getRole())) {
            throw BusinessException.badRequest("接待人必须是平台管理员或管家");
        }
        return confirmHosted(
                appointment, operatorId, "ADMIN",
                request == null ? null : request.meetingPoint(),
                request == null ? null : request.viewingInstruction(),
                hostUserId, Perspective.ADMIN
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail rejectAdminAppointment(
            String operatorId, String appointmentId, String reason
    ) {
        return rejectHosted(
                requirePlatformHostedAppointment(appointmentId),
                operatorId, "ADMIN", reason, Perspective.ADMIN
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail rescheduleAdminAppointment(
            String operatorId,
            String appointmentId,
            AppointmentDtos.RescheduleRequest request
    ) {
        return proposeReschedule(
                requirePlatformHostedAppointment(appointmentId),
                operatorId, "ADMIN", request, Perspective.ADMIN
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail completeAdminAppointment(
            String operatorId, String appointmentId
    ) {
        return complete(
                requirePlatformAppointment(appointmentId),
                operatorId, "ADMIN", Perspective.ADMIN
        );
    }

    @Override
    @Transactional
    public AppointmentDtos.Detail markAdminNoShow(
            String operatorId, String appointmentId
    ) {
        return noShow(
                requirePlatformAppointment(appointmentId),
                operatorId, "ADMIN", Perspective.ADMIN
        );
    }

    private AppointmentDtos.Detail confirmHosted(
            Appointment appointment,
            String operatorId,
            String operatorRole,
            String meetingPoint,
            String instruction,
            String hostUserId,
            Perspective perspective
    ) {
        requireStatus(appointment, AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setConfirmedBy(operatorId);
        appointment.setConfirmedAt(LocalDateTime.now());
        appointment.setHostUserId(hostUserId);
        appointment.setMeetingPoint(trimToNull(meetingPoint));
        appointment.setViewingInstruction(trimToNull(instruction));
        transition(
                appointment, AppointmentStatus.CONFIRMED,
                operatorId, operatorRole, "确认预约"
        );
        notifyTenant(appointment, "预约已确认", "您的看房预约已确认，请按时到场");
        return toDetail(appointment, perspective);
    }

    private AppointmentDtos.Detail rejectHosted(
            Appointment appointment,
            String operatorId,
            String operatorRole,
            String reason,
            Perspective perspective
    ) {
        requireStatus(appointment, AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setRejectReason(trimToNull(reason));
        transition(
                appointment, AppointmentStatus.REJECTED,
                operatorId, operatorRole, reason
        );
        notifyTenant(appointment, "预约未通过", "看房预约未通过，请查看详情");
        return toDetail(appointment, perspective);
    }

    private AppointmentDtos.Detail proposeReschedule(
            Appointment appointment,
            String operatorId,
            String operatorRole,
            AppointmentDtos.RescheduleRequest request,
            Perspective perspective
    ) {
        AppointmentStatus current = AppointmentStatus.from(appointment.getStatus());
        if (current != AppointmentStatus.PENDING_CONFIRMATION
                && current != AppointmentStatus.CONFIRMED) {
            throw BusinessException.conflict("当前预约状态不能改期");
        }
        if (request == null || request.proposedStartAt() == null) {
            throw BusinessException.badRequest("建议改期时间不能为空");
        }
        HouseViewingConfig config = viewingConfigMapper.selectById(appointment.getHouseId());
        LocalDateTime proposedStart = toBusinessTime(request.proposedStartAt());
        int duration = configValue(
                config == null ? null : config.getDurationMinutes(),
                DEFAULT_DURATION_MINUTES
        );
        TimeWindow proposed = new TimeWindow(proposedStart, proposedStart.plusMinutes(duration));
        validateWindow(proposed, config);
        ensureSlotAvailable(appointment.getHouseId(), appointment.getId(), proposed);
        appointment.setProposedStartAt(proposed.start());
        appointment.setProposedEndAt(proposed.end());
        appointment.setActiveSlotKey(buildActiveSlotKey(appointment.getHouseId(), proposed));
        appointment.setRescheduleReason(trimToNull(request.reason()));
        appointment.setRescheduleDeadlineAt(resolveDeadline(
                proposed.start(),
                configValue(
                        config == null ? null : config.getRescheduleTimeoutMinutes(),
                        DEFAULT_RESCHEDULE_TIMEOUT_MINUTES
                )
        ));
        transition(
                appointment, AppointmentStatus.RESCHEDULE_PROPOSED,
                operatorId, operatorRole, request.reason()
        );
        notifyTenant(appointment, "预约时间需要调整", "接待方提出了新的看房时间，请及时确认");
        return toDetail(appointment, perspective);
    }

    private AppointmentDtos.Detail complete(
            Appointment appointment,
            String operatorId,
            String operatorRole,
            Perspective perspective
    ) {
        AppointmentStatus current = AppointmentStatus.from(appointment.getStatus());
        if (current != AppointmentStatus.CONFIRMED
                && current != AppointmentStatus.READY
                && current != AppointmentStatus.IN_PROGRESS) {
            throw BusinessException.conflict("当前预约状态不能标记完成");
        }
        appointment.setCompletedAt(LocalDateTime.now());
        transition(
                appointment, AppointmentStatus.COMPLETED,
                operatorId, operatorRole, "完成看房"
        );
        notifyTenant(appointment, "看房已完成", "本次看房已完成，您可以继续提交租房申请");
        return toDetail(appointment, perspective);
    }

    private AppointmentDtos.Detail noShow(
            Appointment appointment,
            String operatorId,
            String operatorRole,
            Perspective perspective
    ) {
        AppointmentStatus current = AppointmentStatus.from(appointment.getStatus());
        if (current != AppointmentStatus.CONFIRMED && current != AppointmentStatus.READY) {
            throw BusinessException.conflict("当前预约状态不能标记爽约");
        }
        transition(
                appointment, AppointmentStatus.NO_SHOW,
                operatorId, operatorRole, "租客未到场"
        );
        notifyTenant(appointment, "预约已结束", "本次预约被标记为未到场");
        return toDetail(appointment, perspective);
    }

    private PageData<AppointmentDtos.Summary> listAppointments(
            LambdaQueryWrapper<Appointment> query,
            long page,
            long pageSize,
            Perspective perspective
    ) {
        long safePage = Math.max(1, page);
        long safePageSize = Math.max(1, Math.min(pageSize, 100));
        Page<Appointment> result = page(new Page<>(safePage, safePageSize), query);
        List<AppointmentDtos.Summary> items = result.getRecords().stream()
                .map(item -> toSummary(item, perspective))
                .toList();
        return PageData.of(items, safePage, safePageSize, result.getTotal());
    }

    private AppointmentDtos.Summary toSummary(
            Appointment appointment,
            Perspective perspective
    ) {
        House house = houseService.getById(appointment.getHouseId());
        User landlord = StringUtils.hasText(appointment.getLandlordId())
                ? userService.getById(appointment.getLandlordId())
                : null;
        ViewingMode mode = ViewingMode.valueOf(appointment.getViewingMode());
        return new AppointmentDtos.Summary(
                appointment.getId(),
                appointment.getHouseId(),
                house == null ? "房源已删除" : house.getTitle(),
                house == null ? null : house.getCoverImage(),
                appointment.getSourceType(),
                sourceLabel(appointment.getSourceType()),
                mode.name(),
                mode.label(),
                appointment.getStatus(),
                toOffset(appointment.getAppointmentStartAt()),
                toOffset(appointment.getAppointmentEndAt()),
                appointment.getContactName(),
                perspective == Perspective.ADMIN
                        ? appointment.getContactPhone()
                        : maskPhoneForPerspective(appointment, perspective),
                appointment.getLandlordId(),
                landlord == null ? null : displayName(landlord),
                accessStatus(appointment),
                availableActions(appointment, perspective)
        );
    }

    private AppointmentDtos.Detail toDetail(
            Appointment appointment,
            Perspective perspective
    ) {
        House house = houseService.getById(appointment.getHouseId());
        User host = resolveHost(appointment);
        AppointmentAccessGrant access = findAccess(appointment.getId());
        ViewingMode mode = ViewingMode.valueOf(appointment.getViewingMode());
        boolean tenantView = perspective == Perspective.TENANT;
        boolean showCheckinCode = tenantView
                && mode != ViewingMode.SELF_SERVICE_LOCK
                && Set.of(
                        AppointmentStatus.CONFIRMED.name(),
                        AppointmentStatus.READY.name(),
                        AppointmentStatus.IN_PROGRESS.name()
                ).contains(appointment.getStatus());
        List<AppointmentDtos.StatusLogView> logs = statusLogMapper.selectList(
                        Wrappers.<AppointmentStatusLog>lambdaQuery()
                                .eq(AppointmentStatusLog::getAppointmentId, appointment.getId())
                                .orderByAsc(AppointmentStatusLog::getCreatedAt)
                ).stream()
                .map(log -> new AppointmentDtos.StatusLogView(
                        log.getFromStatus(),
                        log.getToStatus(),
                        log.getOperatorRole(),
                        log.getReason(),
                        toOffset(log.getCreatedAt())
                ))
                .toList();
        return new AppointmentDtos.Detail(
                appointment.getId(),
                appointment.getUserId(),
                appointment.getStatus(),
                appointment.getSourceType(),
                sourceLabel(appointment.getSourceType()),
                mode.name(),
                mode.label(),
                toOffset(appointment.getAppointmentStartAt()),
                toOffset(appointment.getAppointmentEndAt()),
                toOffset(appointment.getConfirmDeadlineAt()),
                toOffset(appointment.getProposedStartAt()),
                toOffset(appointment.getProposedEndAt()),
                appointment.getRescheduleReason(),
                new AppointmentDtos.HouseSummary(
                        appointment.getHouseId(),
                        house == null ? "房源已删除" : house.getTitle(),
                        house == null ? null : house.getCoverImage(),
                        house == null ? null : house.getAddress()
                ),
                host == null ? null : new AppointmentDtos.HostView(
                        host.getId(),
                        displayName(host),
                        canContact(appointment)
                                ? host.getPhone()
                                : maskPhone(host.getPhone()),
                        canContact(appointment)
                ),
                appointment.getContactName(),
                perspective == Perspective.ADMIN
                        ? appointment.getContactPhone()
                        : maskPhoneForPerspective(appointment, perspective),
                appointment.getRemark(),
                appointment.getMeetingPoint(),
                appointment.getViewingInstruction(),
                appointment.getRejectReason(),
                appointment.getCancelReason(),
                showCheckinCode ? checkinCodeService.codeFor(appointment.getId()) : null,
                access == null ? accessStatus(appointment) : access.getStatus(),
                access == null ? null : toOffset(access.getValidFrom()),
                access == null ? null : toOffset(access.getValidTo()),
                availableActions(appointment, perspective),
                logs
        );
    }

    private String maskPhoneForPerspective(
            Appointment appointment,
            Perspective perspective
    ) {
        if (perspective == Perspective.TENANT) {
            return appointment.getContactPhone();
        }
        return canContact(appointment)
                ? appointment.getContactPhone()
                : maskPhone(appointment.getContactPhone());
    }

    private List<String> availableActions(
            Appointment appointment,
            Perspective perspective
    ) {
        AppointmentStatus status = AppointmentStatus.from(appointment.getStatus());
        ViewingMode mode = ViewingMode.valueOf(appointment.getViewingMode());
        List<String> actions = new ArrayList<>();
        if (perspective == Perspective.TENANT) {
            if (!status.isTerminal() && status != AppointmentStatus.IN_PROGRESS) {
                actions.add("CANCEL");
            }
            if (status == AppointmentStatus.RESCHEDULE_PROPOSED) {
                actions.add("ACCEPT_RESCHEDULE");
                actions.add("REJECT_RESCHEDULE");
            }
            if (mode == ViewingMode.SELF_SERVICE_LOCK
                    && (status == AppointmentStatus.READY
                    || status == AppointmentStatus.IN_PROGRESS)) {
                actions.add("UNLOCK");
                actions.add("VIEW_PASSCODE");
            }
            if (mode != ViewingMode.SELF_SERVICE_LOCK
                    && (status == AppointmentStatus.CONFIRMED
                    || status == AppointmentStatus.READY
                    || status == AppointmentStatus.IN_PROGRESS)) {
                actions.add("CONTACT_HOST");
                actions.add("NAVIGATE");
            }
        } else {
            if (status == AppointmentStatus.PENDING_CONFIRMATION) {
                actions.add("CONFIRM");
                actions.add("REJECT");
                actions.add("RESCHEDULE");
            }
            if (status == AppointmentStatus.CONFIRMED
                    || status == AppointmentStatus.READY) {
                actions.add("CHECK_IN");
                actions.add("COMPLETE");
                actions.add("NO_SHOW");
                actions.add("RESCHEDULE");
            }
            if (status == AppointmentStatus.IN_PROGRESS) {
                actions.add("COMPLETE");
            }
        }
        return List.copyOf(actions);
    }

    private void transition(
            Appointment appointment,
            AppointmentStatus target,
            String operatorId,
            String operatorRole,
            String reason
    ) {
        String from = appointment.getStatus();
        int expectedVersion = appointment.getVersion() == null
                ? 0
                : appointment.getVersion();
        appointment.setStatus(target.name());
        appointment.setUpdatedAt(LocalDateTime.now());
        appointment.setVersion(expectedVersion + 1);
        if (target.isTerminal()) {
            appointment.setActiveSlotKey(null);
        }
        try {
            int updated = baseMapper.updateWithStatusAndVersion(
                    appointment, from, expectedVersion
            );
            if (updated == 0) {
                throw BusinessException.conflict("预约状态已变化，请刷新后重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("该看房时段已被预约，请选择其他时间");
        }
        writeStatusLog(
                appointment.getId(), from, target.name(),
                operatorId, operatorRole, reason
        );
    }

    private void writeStatusLog(
            String appointmentId,
            String from,
            String to,
            String operatorId,
            String operatorRole,
            String reason
    ) {
        AppointmentStatusLog log = new AppointmentStatusLog();
        log.setId(UUID.randomUUID().toString());
        log.setAppointmentId(appointmentId);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setReason(trimToNull(reason));
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
    }

    private Appointment requireOwnedAppointment(String userId, String appointmentId) {
        Appointment appointment = getOne(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getId, appointmentId)
                        .eq(Appointment::getUserId, userId)
        );
        if (appointment == null) {
            throw BusinessException.notFound("预约不存在");
        }
        return appointment;
    }

    private Appointment requireLandlordAppointment(
            String landlordId, String appointmentId
    ) {
        Appointment appointment = getOne(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getId, appointmentId)
                        .eq(Appointment::getLandlordId, landlordId)
                        .eq(Appointment::getViewingMode, ViewingMode.LANDLORD_HOSTED.name())
        );
        if (appointment == null) {
            throw BusinessException.notFound("预约不存在");
        }
        House house = houseService.getById(appointment.getHouseId());
        if (house == null || !landlordId.equals(house.getLandlordId())) {
            throw BusinessException.notFound("预约不存在");
        }
        return appointment;
    }

    private Appointment requirePlatformHostedAppointment(String appointmentId) {
        Appointment appointment = requirePlatformAppointment(appointmentId);
        if (!ViewingMode.PLATFORM_HOSTED.name().equals(appointment.getViewingMode())) {
            throw BusinessException.forbidden("只能处理平台陪同看房预约");
        }
        return appointment;
    }

    private Appointment requirePlatformAppointment(String appointmentId) {
        Appointment appointment = requireAppointment(appointmentId);
        if (!HouseSourceType.PLATFORM.name().equalsIgnoreCase(appointment.getSourceType())) {
            throw BusinessException.forbidden("只能处理平台房源预约");
        }
        return appointment;
    }

    private Appointment requireAppointment(String appointmentId) {
        Appointment appointment = getById(appointmentId);
        if (appointment == null) {
            throw BusinessException.notFound("预约不存在");
        }
        return appointment;
    }

    private ViewingMode resolveViewingMode(House house, HouseViewingConfig config) {
        String configured = config == null ? null : config.getViewingMode();
        if (StringUtils.hasText(configured)) {
            try {
                ViewingMode requested = ViewingMode.valueOf(configured.toUpperCase(Locale.ROOT));
                boolean platform = HouseSourceType.PLATFORM.name()
                        .equalsIgnoreCase(house.getSourceType());
                if (requested == ViewingMode.SELF_SERVICE_LOCK
                        && hasUsableSmartLock(house)) {
                    return requested;
                }
                if (platform && requested == ViewingMode.PLATFORM_HOSTED) {
                    return requested;
                }
                if (!platform && requested == ViewingMode.LANDLORD_HOSTED) {
                    return requested;
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid stored configuration falls back to safe server-side defaults.
            }
        }
        if (HouseSourceType.PLATFORM.name().equalsIgnoreCase(house.getSourceType())) {
            return hasUsableSmartLock(house)
                    ? ViewingMode.SELF_SERVICE_LOCK
                    : ViewingMode.PLATFORM_HOSTED;
        }
        return ViewingMode.LANDLORD_HOSTED;
    }

    private boolean hasUsableSmartLock(House house) {
        if (!Integer.valueOf(1).equals(house.getIsSmartLockSupported())
                || !Integer.valueOf(1).equals(house.getIsSelfViewingSupported())
                || !StringUtils.hasText(house.getSmartLockId())
                || !"BOUND".equalsIgnoreCase(house.getLockBindStatus())) {
            return false;
        }
        SmartLock smartLock = smartLockMapper.selectById(house.getSmartLockId());
        return smartLock != null
                && house.getId().equals(smartLock.getHouseId())
                && "BOUND".equalsIgnoreCase(smartLock.getStatus())
                && smartLock.getLockId() != null
                && StringUtils.hasText(smartLock.getLockData());
    }

    private TimeWindow resolveCreateWindow(
            AppointmentDtos.CreateRequest request,
            HouseViewingConfig config
    ) {
        int duration = configValue(
                config == null ? null : config.getDurationMinutes(),
                DEFAULT_DURATION_MINUTES
        );
        if (request.appointmentStartAt() != null) {
            LocalDateTime start = toBusinessTime(request.appointmentStartAt());
            return new TimeWindow(start, start.plusMinutes(duration));
        }
        if (request.appointmentDate() == null || !StringUtils.hasText(request.timeSlot())) {
            throw BusinessException.badRequest("预约开始时间不能为空");
        }
        try {
            String[] parts = request.timeSlot().split("-", 2);
            LocalTime startTime = LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endTime = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm"));
            return new TimeWindow(
                    request.appointmentDate().atTime(startTime),
                    request.appointmentDate().atTime(endTime)
            );
        } catch (DateTimeParseException | ArrayIndexOutOfBoundsException exception) {
            throw BusinessException.badRequest("预约时间段格式错误");
        }
    }

    private void validateWindow(TimeWindow window, HouseViewingConfig config) {
        if (!window.end().isAfter(window.start())) {
            throw BusinessException.badRequest("预约结束时间必须晚于开始时间");
        }
        int advanceMinutes = configValue(
                config == null ? null : config.getAdvanceMinMinutes(),
                DEFAULT_ADVANCE_MINUTES
        );
        int advanceDays = configValue(
                config == null ? null : config.getAdvanceMaxDays(),
                DEFAULT_ADVANCE_DAYS
        );
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        if (window.start().isBefore(now.plusMinutes(advanceMinutes))) {
            throw BusinessException.badRequest("预约时间过早或已经过去");
        }
        if (window.start().isAfter(now.plusDays(advanceDays))) {
            throw BusinessException.badRequest("预约时间超出可预约范围");
        }
        if (!DEFAULT_START_TIMES.contains(window.start().toLocalTime())) {
            throw BusinessException.badRequest("请选择系统提供的预约时段");
        }
        int duration = configValue(
                config == null ? null : config.getDurationMinutes(),
                DEFAULT_DURATION_MINUTES
        );
        if (!window.end().equals(window.start().plusMinutes(duration))) {
            throw BusinessException.badRequest("预约时长不符合房源配置");
        }
        if (window.start().getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw BusinessException.badRequest("该日期暂不开放预约");
        }
    }

    private void ensureSlotAvailable(
            String houseId, String excludedAppointmentId, TimeWindow window
    ) {
        long count = count(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getHouseId, houseId)
                        .eq(Appointment::getAppointmentStartAt, window.start())
                        .eq(Appointment::getAppointmentEndAt, window.end())
                        .in(Appointment::getStatus, activeStatusNames())
                        .ne(StringUtils.hasText(excludedAppointmentId),
                                Appointment::getId, excludedAppointmentId)
        );
        if (count > 0) {
            throw BusinessException.conflict("该看房时段已被预约，请选择其他时间");
        }
    }

    private TimeWindow requireProposedWindow(Appointment appointment) {
        if (appointment.getProposedStartAt() == null || appointment.getProposedEndAt() == null) {
            throw BusinessException.conflict("预约不存在有效的改期时间");
        }
        return new TimeWindow(
                appointment.getProposedStartAt(),
                appointment.getProposedEndAt()
        );
    }

    private LocalDateTime resolveDeadline(LocalDateTime appointmentStart, int timeoutMinutes) {
        LocalDateTime byTimeout = LocalDateTime.now(BUSINESS_ZONE).plusMinutes(timeoutMinutes);
        LocalDateTime beforeStart = appointmentStart.minusMinutes(15);
        return byTimeout.isBefore(beforeStart) ? byTimeout : beforeStart;
    }

    private String buildActiveSlotKey(String houseId, TimeWindow window) {
        return houseId + "|" + window.start() + "|" + window.end();
    }

    private String formatTimeSlot(TimeWindow window) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return formatter.format(window.start()) + "-" + formatter.format(window.end());
    }

    private void requireViewingEnabled(HouseViewingConfig config) {
        if (config != null && Integer.valueOf(0).equals(config.getEnabled())) {
            throw BusinessException.conflict("该房源暂未开放预约看房");
        }
    }

    private void requireStatus(Appointment appointment, AppointmentStatus expected) {
        if (!expected.name().equalsIgnoreCase(appointment.getStatus())) {
            throw BusinessException.conflict("当前预约状态不允许执行该操作");
        }
    }

    private AppointmentAccessGrant findAccess(String appointmentId) {
        return accessGrantMapper.selectOne(
                Wrappers.<AppointmentAccessGrant>lambdaQuery()
                        .eq(AppointmentAccessGrant::getAppointmentId, appointmentId)
                        .last("LIMIT 1")
        );
    }

    private String accessStatus(Appointment appointment) {
        if (!ViewingMode.SELF_SERVICE_LOCK.name().equals(appointment.getViewingMode())) {
            return AppointmentAccessStatus.NOT_REQUIRED.name();
        }
        AppointmentAccessGrant access = findAccess(appointment.getId());
        return access == null ? AppointmentAccessStatus.PENDING.name() : access.getStatus();
    }

    private User resolveHost(Appointment appointment) {
        String hostId = StringUtils.hasText(appointment.getHostUserId())
                ? appointment.getHostUserId()
                : ViewingMode.LANDLORD_HOSTED.name().equals(
                        appointment.getViewingMode()
                ) ? appointment.getLandlordId() : null;
        return StringUtils.hasText(hostId) ? userService.getById(hostId) : null;
    }

    private boolean canContact(Appointment appointment) {
        return Set.of(
                AppointmentStatus.CONFIRMED.name(),
                AppointmentStatus.READY.name(),
                AppointmentStatus.IN_PROGRESS.name()
        ).contains(appointment.getStatus());
    }

    private void notifyCreated(Appointment appointment, House house) {
        if (ViewingMode.LANDLORD_HOSTED.name().equals(appointment.getViewingMode())
                && StringUtils.hasText(appointment.getLandlordId())) {
            messageService.sendMessage(
                    appointment.getLandlordId(),
                    "appointment",
                    "新的看房预约",
                    "租客预约查看“" + house.getTitle() + "”，请及时处理",
                    "appointment",
                    appointment.getId()
            );
        }
        if (ViewingMode.SELF_SERVICE_LOCK.name().equals(appointment.getViewingMode())) {
            notifyTenant(
                    appointment,
                    "预约已确认",
                    "自助看房预约已确认，开门凭证将在预约开始前准备"
            );
        } else if (ViewingMode.PLATFORM_HOSTED.name().equals(
                appointment.getViewingMode()
        )) {
            notifyPlatformStaff(
                    appointment,
                    "新的平台看房预约",
                    "平台房源收到新的陪同看房预约，请及时处理"
            );
        }
    }

    private void notifyTenant(Appointment appointment, String title, String content) {
        messageService.sendMessage(
                appointment.getUserId(),
                "appointment",
                title,
                content,
                "appointment",
                appointment.getId()
        );
    }

    private void notifyHost(Appointment appointment, String title, String content) {
        if (StringUtils.hasText(appointment.getLandlordId())
                && ViewingMode.LANDLORD_HOSTED.name().equals(appointment.getViewingMode())) {
            messageService.sendMessage(
                    appointment.getLandlordId(),
                    "appointment",
                    title,
                    content,
                    "appointment",
                    appointment.getId()
            );
        } else if (StringUtils.hasText(appointment.getHostUserId())) {
            messageService.sendMessage(
                    appointment.getHostUserId(),
                    "appointment",
                    title,
                    content,
                    "appointment",
                    appointment.getId()
            );
        } else if (ViewingMode.PLATFORM_HOSTED.name().equals(
                appointment.getViewingMode()
        )) {
            notifyPlatformStaff(appointment, title, content);
        }
    }

    private void notifyPlatformStaff(
            Appointment appointment,
            String title,
            String content
    ) {
        userService.list(
                Wrappers.<User>lambdaQuery()
                        .in(User::getRole, "ADMIN", "HOUSEKEEPER")
                        .eq(User::getStatus, "active")
        ).forEach(user -> messageService.sendMessage(
                user.getId(),
                "appointment",
                title,
                content,
                "appointment",
                appointment.getId()
        ));
    }

    private AppointmentDtos.CreateResult toCreateResult(Appointment appointment) {
        ViewingMode mode = ViewingMode.valueOf(appointment.getViewingMode());
        return new AppointmentDtos.CreateResult(
                appointment.getId(),
                appointment.getHouseId(),
                appointment.getSourceType(),
                mode.name(),
                appointment.getStatus(),
                mode != ViewingMode.SELF_SERVICE_LOCK,
                toOffset(appointment.getAppointmentStartAt()),
                toOffset(appointment.getAppointmentEndAt()),
                toOffset(appointment.getConfirmDeadlineAt())
        );
    }

    private String normalizeSourceType(String sourceType) {
        return HouseSourceType.PLATFORM.name().equalsIgnoreCase(sourceType)
                ? HouseSourceType.PLATFORM.name()
                : HouseSourceType.LANDLORD.name();
    }

    private String sourceLabel(String sourceType) {
        return HouseSourceType.PLATFORM.name().equalsIgnoreCase(sourceType)
                ? "平台自营"
                : "个人房源";
    }

    private String normalizeStatusFilter(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return AppointmentStatus.from(status).name();
    }

    private List<String> activeStatusNames() {
        return ACTIVE_SLOT_STATUSES.stream().map(Enum::name).toList();
    }

    private int configValue(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private LocalDateTime toBusinessTime(OffsetDateTime value) {
        return value.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value == null ? null : value.atOffset(BUSINESS_OFFSET);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String displayName(User user) {
        return StringUtils.hasText(user.getNickname())
                ? user.getNickname()
                : maskPhone(user.getPhone());
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private record TimeWindow(LocalDateTime start, LocalDateTime end) {
    }

    private enum Perspective {
        TENANT,
        LANDLORD,
        ADMIN
    }
}
