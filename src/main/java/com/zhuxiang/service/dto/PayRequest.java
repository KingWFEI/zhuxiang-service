package com.zhuxiang.service.dto;

import jakarta.validation.constraints.NotBlank;

public record PayRequest(
        @NotBlank(message = "支付方式不能为空") String paymentMethod,
        String paymentChannel
) {
    public String paymentChannel() {
        return paymentChannel != null && !paymentChannel.isBlank() ? paymentChannel : "mock";
    }
}
