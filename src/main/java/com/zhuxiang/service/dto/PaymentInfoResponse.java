package com.zhuxiang.service.dto;

import java.util.List;

public record PaymentInfoResponse(
        String orderId,
        Integer amount,
        Integer monthlyRent,
        Integer deposit,
        Integer serviceFee,
        List<String> paymentMethods
) {
}
