package com.zhuxiang.service.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record AppointmentRequest(
            @NotBlank(message = "houseId 不能为空") String houseId,
            @NotNull(message = "预约日期不能为空")
            @FutureOrPresent(message = "预约日期不能早于今天") LocalDate appointmentDate,
            @NotBlank(message = "预约时间段不能为空")
            @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$", message = "预约时间段格式错误")
            String timeSlot,
            @NotBlank(message = "联系人姓名不能为空")
            @Size(min = 1, max = 30, message = "联系人姓名长度应为 1-30 位") String contactName,
            @NotBlank(message = "联系电话不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式错误") String contactPhone,
            @Size(max = 500, message = "备注不能超过 500 字") String remark
    ) {
    }

    public record AppointmentResult(String id, String houseId, String status) {
    }

    public record ConversationRequest(
            @NotBlank(message = "会话来源不能为空")
            @Pattern(
                    regexp = "house_detail|profile|customer_service",
                    message = "会话来源不支持"
            ) String source,
            String houseId,
            String landlordId
    ) {
    }

    public record ConversationResult(String conversationId) {
    }
}
