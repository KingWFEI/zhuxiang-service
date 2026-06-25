package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;

public record PayRequest(
        @NotBlank(message = "支付方式不能为空") String paymentMethod
) {
}
