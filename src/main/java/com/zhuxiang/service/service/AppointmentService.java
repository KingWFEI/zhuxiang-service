package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.AppointmentDtos;

import java.time.LocalDate;

/**
* @author king-wang
* @description 针对表【appointment(预约看房表)】的数据库操作Service
* @createDate 2026-06-12 19:56:26
*/
public interface AppointmentService extends IService<Appointment> {

    /**
     * 创建预约看房记录。
     */
    AppointmentDtos.CreateResult createAppointment(
            String userId,
            String idempotencyKey,
            AppointmentDtos.CreateRequest request
    );

    AppointmentDtos.ViewingSlotResult getViewingSlots(
            String houseId, LocalDate startDate, int days
    );

    PageData<AppointmentDtos.Summary> listMyAppointments(
            String userId, String status, long page, long pageSize
    );

    AppointmentDtos.Detail getMyAppointment(String userId, String appointmentId);

    AppointmentDtos.Detail cancelMyAppointment(
            String userId, String appointmentId, String reason
    );

    AppointmentDtos.Detail acceptMyReschedule(String userId, String appointmentId);

    AppointmentDtos.Detail rejectMyReschedule(
            String userId, String appointmentId, String reason
    );

    AppointmentDtos.Detail recordUnlockAttempt(
            String userId,
            String appointmentId,
            AppointmentDtos.UnlockAttemptRequest request
    );

    PageData<AppointmentDtos.Summary> listLandlordAppointments(
            String landlordId, String status, long page, long pageSize
    );

    AppointmentDtos.Detail getLandlordAppointment(String landlordId, String appointmentId);

    AppointmentDtos.Detail confirmLandlordAppointment(
            String landlordId, String appointmentId, AppointmentDtos.ConfirmRequest request
    );

    AppointmentDtos.Detail rejectLandlordAppointment(
            String landlordId, String appointmentId, String reason
    );

    AppointmentDtos.Detail rescheduleLandlordAppointment(
            String landlordId, String appointmentId, AppointmentDtos.RescheduleRequest request
    );

    AppointmentDtos.Detail checkInLandlordAppointment(
            String landlordId, String appointmentId, String checkinCode
    );

    AppointmentDtos.Detail completeLandlordAppointment(String landlordId, String appointmentId);

    AppointmentDtos.Detail markLandlordNoShow(String landlordId, String appointmentId);

    PageData<AppointmentDtos.Summary> listAdminAppointments(
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
    );

    AppointmentDtos.Detail getAdminAppointment(String appointmentId);

    AppointmentDtos.Detail confirmAdminAppointment(
            String operatorId, String appointmentId, AppointmentDtos.ConfirmRequest request
    );

    AppointmentDtos.Detail rejectAdminAppointment(
            String operatorId, String appointmentId, String reason
    );

    AppointmentDtos.Detail rescheduleAdminAppointment(
            String operatorId, String appointmentId, AppointmentDtos.RescheduleRequest request
    );

    AppointmentDtos.Detail completeAdminAppointment(String operatorId, String appointmentId);

    AppointmentDtos.Detail markAdminNoShow(String operatorId, String appointmentId);
}
