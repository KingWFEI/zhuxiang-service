package com.zhuxiang.service.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.common.AppointmentStatus;
import com.zhuxiang.service.common.ViewingMode;
import com.zhuxiang.service.entity.Appointment;
import com.zhuxiang.service.entity.AppointmentStatusLog;
import com.zhuxiang.service.mapper.AppointmentMapper;
import com.zhuxiang.service.mapper.AppointmentStatusLogMapper;
import com.zhuxiang.service.service.AppointmentAccessGrantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class AppointmentMaintenanceJob {

    private static final Logger log = LoggerFactory.getLogger(AppointmentMaintenanceJob.class);

    private final AppointmentMapper appointmentMapper;
    private final AppointmentStatusLogMapper statusLogMapper;
    private final AppointmentAccessGrantService accessGrantService;

    public AppointmentMaintenanceJob(
            AppointmentMapper appointmentMapper,
            AppointmentStatusLogMapper statusLogMapper,
            AppointmentAccessGrantService accessGrantService
    ) {
        this.appointmentMapper = appointmentMapper;
        this.statusLogMapper = statusLogMapper;
        this.accessGrantService = accessGrantService;
    }

    @Scheduled(fixedDelayString = "${app.appointment.maintenance-ms:60000}")
    public void maintainAppointments() {
        LocalDateTime now = LocalDateTime.now();
        expireUnconfirmed(now);
        expireRescheduleProposals(now);
        prepareSelfServiceAccess(now);
        openReadyWindows(now);
        finishElapsedAppointments(now);
        revokeTerminalAccess();
    }

    private void expireUnconfirmed(LocalDateTime now) {
        List<Appointment> appointments = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getStatus, AppointmentStatus.PENDING_CONFIRMATION.name())
                        .isNotNull(Appointment::getConfirmDeadlineAt)
                        .lt(Appointment::getConfirmDeadlineAt, now)
                        .last("LIMIT 100")
        );
        appointments.forEach(item -> transition(
                item, AppointmentStatus.EXPIRED, "预约确认超时"
        ));
    }

    private void expireRescheduleProposals(LocalDateTime now) {
        List<Appointment> appointments = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getStatus, AppointmentStatus.RESCHEDULE_PROPOSED.name())
                        .isNotNull(Appointment::getRescheduleDeadlineAt)
                        .lt(Appointment::getRescheduleDeadlineAt, now)
                        .last("LIMIT 100")
        );
        appointments.forEach(item -> transition(
                item, AppointmentStatus.EXPIRED, "改期确认超时"
        ));
    }

    private void prepareSelfServiceAccess(LocalDateTime now) {
        List<Appointment> appointments = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getViewingMode, ViewingMode.SELF_SERVICE_LOCK.name())
                        .in(Appointment::getStatus,
                                AppointmentStatus.CONFIRMED.name(),
                                AppointmentStatus.READY.name())
                        .le(Appointment::getAppointmentStartAt, now.plusMinutes(15))
                        .gt(Appointment::getAppointmentEndAt, now)
                        .last("LIMIT 50")
        );
        appointments.forEach(item -> {
            try {
                accessGrantService.grantForAppointment(item.getId());
            } catch (RuntimeException exception) {
                log.warn(
                        "预约门锁授权未完成 appointmentId={}, exceptionType={}",
                        item.getId(),
                        exception.getClass().getSimpleName()
                );
            }
        });
    }

    private void openReadyWindows(LocalDateTime now) {
        List<Appointment> appointments = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getStatus, AppointmentStatus.CONFIRMED.name())
                        .le(Appointment::getAppointmentStartAt, now.plusMinutes(10))
                        .gt(Appointment::getAppointmentEndAt, now)
                        .last("LIMIT 100")
        );
        appointments.forEach(item -> transition(
                item, AppointmentStatus.READY, "进入预约看房时间窗口"
        ));
    }

    private void finishElapsedAppointments(LocalDateTime now) {
        List<Appointment> ready = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getStatus, AppointmentStatus.READY.name())
                        .lt(Appointment::getAppointmentEndAt, now.minusMinutes(10))
                        .last("LIMIT 100")
        );
        ready.forEach(item -> transition(
                item, AppointmentStatus.NO_SHOW, "预约结束未签到"
        ));
        List<Appointment> inProgress = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getStatus, AppointmentStatus.IN_PROGRESS.name())
                        .lt(Appointment::getAppointmentEndAt, now.minusMinutes(10))
                        .last("LIMIT 100")
        );
        inProgress.forEach(item -> transition(
                item, AppointmentStatus.COMPLETED, "预约时间结束自动完成"
        ));
    }

    private void revokeTerminalAccess() {
        List<Appointment> terminal = appointmentMapper.selectList(
                Wrappers.<Appointment>lambdaQuery()
                        .eq(Appointment::getViewingMode, ViewingMode.SELF_SERVICE_LOCK.name())
                        .in(Appointment::getStatus,
                                AppointmentStatus.CANCELLED.name(),
                                AppointmentStatus.REJECTED.name(),
                                AppointmentStatus.EXPIRED.name(),
                                AppointmentStatus.NO_SHOW.name(),
                                AppointmentStatus.COMPLETED.name())
                        .orderByDesc(Appointment::getUpdatedAt)
                        .last("LIMIT 100")
        );
        terminal.forEach(item -> {
            try {
                if (item.getAppointmentEndAt() != null
                        && item.getAppointmentEndAt().isBefore(LocalDateTime.now())) {
                    accessGrantService.expire(item.getId());
                } else {
                    accessGrantService.revoke(item.getId());
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "预约门锁权限回收未完成 appointmentId={}, exceptionType={}",
                        item.getId(),
                        exception.getClass().getSimpleName()
                );
            }
        });
    }

    private void transition(
            Appointment appointment,
            AppointmentStatus target,
            String reason
    ) {
        String from = appointment.getStatus();
        int updated = appointmentMapper.update(
                null,
                Wrappers.<Appointment>lambdaUpdate()
                        .set(Appointment::getStatus, target.name())
                        .set(target.isTerminal(), Appointment::getActiveSlotKey, null)
                        .set(Appointment::getCompletedAt,
                                target == AppointmentStatus.COMPLETED
                                        ? LocalDateTime.now()
                                        : appointment.getCompletedAt())
                        .set(Appointment::getUpdatedAt, LocalDateTime.now())
                        .set(Appointment::getVersion,
                                appointment.getVersion() == null
                                        ? 1
                                        : appointment.getVersion() + 1)
                        .eq(Appointment::getId, appointment.getId())
                        .eq(Appointment::getStatus, from)
        );
        if (updated == 0) {
            return;
        }
        AppointmentStatusLog statusLog = new AppointmentStatusLog();
        statusLog.setId(UUID.randomUUID().toString());
        statusLog.setAppointmentId(appointment.getId());
        statusLog.setFromStatus(from);
        statusLog.setToStatus(target.name());
        statusLog.setOperatorId("SYSTEM");
        statusLog.setOperatorRole("SYSTEM");
        statusLog.setReason(reason);
        statusLog.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(statusLog);
    }
}
