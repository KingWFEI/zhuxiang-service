package com.zhuxiang.service.dto;

public record PaymentInfoResponse(
        String orderId,
        String status,
        Integer monthlyRent,
        Integer deposit,
        Integer serviceFee,
        Integer firstPaymentAmount,
        String paymentMethod,
        Integer paymentMonths
) {
}
