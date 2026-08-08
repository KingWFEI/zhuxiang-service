package com.zhuxiang.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    public record CreateRequest(
            @NotBlank(message = "houseId 不能为空") String houseId,
            OffsetDateTime appointmentStartAt,
            LocalDate appointmentDate,
            @Pattern(
                    regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$",
                    message = "timeSlot 格式必须为 HH:mm-HH:mm"
            ) String timeSlot,
            @NotBlank(message = "联系人姓名不能为空")
            @Size(max = 30, message = "联系人姓名不能超过 30 字") String contactName,
            @NotBlank(message = "联系人手机号不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "联系人手机号格式错误") String contactPhone,
            @Size(max = 500, message = "备注不能超过 500 字") String remark,
            Boolean testSlot
    ) {
    }

    public record CreateResult(
            String id,
            String houseId,
            String sourceType,
            String viewingMode,
            String status,
            boolean requiresConfirmation,
            OffsetDateTime appointmentStartAt,
            OffsetDateTime appointmentEndAt,
            OffsetDateTime confirmDeadlineAt
    ) {
    }

    public record ViewingSlot(
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            boolean available,
            OffsetDateTime accessValidFrom,
            OffsetDateTime accessValidTo,
            boolean testSlot
    ) {
    }

    public record ViewingSlotDate(LocalDate date, List<ViewingSlot> slots) {
    }

    public record ViewingSlotResult(
            String houseId,
            String viewingMode,
            boolean requiresConfirmation,
            List<ViewingSlotDate> dates
    ) {
    }

    public record HouseSummary(
            String id,
            String title,
            String coverImage,
            String address
    ) {
    }

    public record HostView(
            String userId,
            String name,
            String phoneMasked,
            boolean canContact
    ) {
    }

    public record Summary(
            String id,
            String houseId,
            String houseTitle,
            String coverImage,
            String sourceType,
            String sourceLabel,
            String viewingMode,
            String viewingModeLabel,
            String status,
            OffsetDateTime appointmentStartAt,
            OffsetDateTime appointmentEndAt,
            String contactName,
            String contactPhone,
            String landlordId,
            String landlordName,
            String accessStatus,
            List<String> availableActions
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Detail(
            String id,
            String userId,
            String status,
            String sourceType,
            String sourceLabel,
            String viewingMode,
            String viewingModeLabel,
            OffsetDateTime appointmentStartAt,
            OffsetDateTime appointmentEndAt,
            OffsetDateTime confirmDeadlineAt,
            OffsetDateTime proposedStartAt,
            OffsetDateTime proposedEndAt,
            String rescheduleReason,
            HouseSummary house,
            HostView host,
            String contactName,
            String contactPhone,
            String remark,
            String meetingPoint,
            String viewingInstruction,
            String rejectReason,
            String cancelReason,
            String checkinCode,
            String accessStatus,
            OffsetDateTime accessValidFrom,
            OffsetDateTime accessValidTo,
            List<String> availableActions,
            List<StatusLogView> statusLogs
    ) {
    }

    public record StatusLogView(
            String fromStatus,
            String toStatus,
            String operatorRole,
            String reason,
            OffsetDateTime createdAt
    ) {
    }

    public record ReasonRequest(
            @NotBlank(message = "原因不能为空")
            @Size(max = 500, message = "原因不能超过 500 字") String reason
    ) {
    }

    public record ConfirmRequest(
            @Size(max = 255, message = "见面地点不能超过 255 字") String meetingPoint,
            @Size(max = 500, message = "看房说明不能超过 500 字") String viewingInstruction,
            String hostUserId
    ) {
    }

    public record RescheduleRequest(
            OffsetDateTime proposedStartAt,
            @NotBlank(message = "改期原因不能为空")
            @Size(max = 500, message = "改期原因不能超过 500 字") String reason
    ) {
    }

    public record CheckinRequest(
            @NotBlank(message = "核验码不能为空")
            @Pattern(regexp = "^\\d{6}$", message = "核验码格式错误") String checkinCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AccessView(
            String accessStatus,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            BluetoothAccess bluetooth,
            PasscodeAccess passcode
    ) {
    }

    public record BluetoothAccess(
            boolean enabled,
            String lockMac,
            String lockData
    ) {
    }

    public record PasscodeAccess(
            boolean available,
            String value,
            OffsetDateTime validFrom,
            OffsetDateTime validTo
    ) {
    }

    public record UnlockAttemptRequest(
            @NotBlank(message = "开锁方式不能为空")
            @Pattern(regexp = "BLUETOOTH|PASSCODE", message = "不支持的开锁方式") String method,
            boolean success,
            String deviceId,
            String errorCode,
            @Size(max = 500, message = "错误信息不能超过 500 字") String errorMessage,
            OffsetDateTime occurredAt
    ) {
    }
}
