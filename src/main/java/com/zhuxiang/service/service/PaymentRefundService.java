package com.zhuxiang.service.service;

import com.zhuxiang.service.entity.PaymentRecord;
import com.zhuxiang.service.entity.PaymentRefund;
import com.zhuxiang.service.entity.RentOrder;

public interface PaymentRefundService {
    PaymentRefund requestRefund(RentOrder order, PaymentRecord payment,
                                 String reason, String triggerType);

    void processRefund(String refundId);
}
