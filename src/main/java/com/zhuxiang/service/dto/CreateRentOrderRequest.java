package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "租房订单创建请求")
public record CreateRentOrderRequest(
        @NotBlank(message = "houseId 不能为空")
        @Schema(description = "房源 ID", example = "house_001") String houseId,

        @NotNull(message = "起租日期不能为空")
        @FutureOrPresent(message = "起租日期不能早于今天")
        @Schema(description = "起租日期，不能早于今天", example = "2026-07-01") LocalDate startDate,

        @NotNull(message = "租期不能为空")
        @Min(value = 1, message = "租期不能少于 1 个月")
        @Max(value = 120, message = "租期不能超过 120 个月")
        @Schema(description = "租期月数，范围 1-120", example = "12") Integer leaseMonths,

        @NotBlank(message = "付款方式不能为空")
        @Schema(description = "租金付款方式，如押一付一、押一付三", example = "押一付三") String paymentMethod,

        @NotNull(message = "租住人数不能为空")
        @Min(value = 1, message = "租住人数不能少于 1 人")
        @Max(value = 20, message = "租住人数不能超过 20 人")
        @Schema(description = "租住人数，范围 1-20", example = "2") Integer tenantCount
) {
}
