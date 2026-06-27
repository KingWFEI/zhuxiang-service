package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "租房订单支付请求")
public record PayRequest(
        @NotBlank(message = "支付方式不能为空")
        @Schema(description = "订单付款方式，需与订单约定一致", example = "押一付三") String paymentMethod,
        @Schema(description = "支付渠道；不传时默认为 mock", example = "mock") String paymentChannel
) {
    public String paymentChannel() {
        return paymentChannel != null && !paymentChannel.isBlank() ? paymentChannel : "mock";
    }
}
