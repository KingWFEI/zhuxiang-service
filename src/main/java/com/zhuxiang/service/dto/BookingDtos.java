package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class BookingDtos {

    private BookingDtos() {
    }

    @Schema(description = "预约看房请求")
    public record AppointmentRequest(
            @NotBlank(message = "houseId 不能为空")
            @Schema(description = "预约房源 ID", example = "house_001") String houseId,
            @NotNull(message = "预约日期不能为空")
            @FutureOrPresent(message = "预约日期不能早于今天")
            @Schema(description = "预约日期，不能早于今天", example = "2026-07-01") LocalDate appointmentDate,
            @NotBlank(message = "预约时间段不能为空")
            @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$", message = "预约时间段格式错误")
            @Schema(description = "预约时间段，格式 HH:mm-HH:mm", example = "09:00-11:00") String timeSlot,
            @NotBlank(message = "联系人姓名不能为空")
            @Size(min = 1, max = 30, message = "联系人姓名长度应为 1-30 位")
            @Schema(description = "联系人姓名", example = "张三") String contactName,
            @NotBlank(message = "联系电话不能为空")
            @Pattern(regexp = "^1\\d{10}$", message = "联系电话格式错误")
            @Schema(description = "联系人手机号", example = "13800138000") String contactPhone,
            @Size(max = 500, message = "备注不能超过 500 字")
            @Schema(description = "预约备注，最多 500 字", example = "希望提前电话联系") String remark
    ) {
    }

    public record AppointmentResult(String id, String houseId, String status) {
    }

    @Schema(description = "咨询会话创建请求")
    public record ConversationRequest(
            @NotBlank(message = "会话来源不能为空")
            @Pattern(
                    regexp = "house_detail|profile|customer_service",
                    message = "会话来源不支持"
            )
            @Schema(description = "会话来源", allowableValues = {"house_detail", "profile", "customer_service"}, example = "house_detail") String source,
            @Schema(description = "关联房源 ID；从房源详情发起时填写", example = "house_001") String houseId,
            @Schema(description = "关联房东 ID；咨询房东时填写", example = "user_001") String landlordId
    ) {
    }

    public record ConversationResult(String conversationId) {
    }
}
