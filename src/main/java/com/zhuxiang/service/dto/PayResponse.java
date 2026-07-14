package com.zhuxiang.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "支付下单响应")
public record PayResponse(
        @Schema(description = "租房订单 ID") String orderId,
        @Schema(description = "支付记录 ID") String paymentRecordId,
        @Schema(description = "支付类型：app(唤起支付宝APP) / h5(H5页面支付) / null(mock直接确认)")
        String payType,
        @Schema(description = "H5 支付页面 URL，payType=h5 时使用") String paymentUrl,
        @Schema(description = "订单当前状态") String orderStatus,
        @Schema(description = "支付编号") String paymentNo,
        @Schema(description = "支付金额，单位：分") Integer amount
) {}
