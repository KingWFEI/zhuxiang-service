package com.zhuxiang.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhuxiang.service.common.PageData;
import com.zhuxiang.service.dto.PaymentDtos.PaymentDetail;
import com.zhuxiang.service.dto.PaymentDtos.PaymentItem;
import com.zhuxiang.service.entity.PaymentRecord;

public interface PaymentRecordService extends IService<PaymentRecord> {

    PageData<PaymentItem> listMyPayments(String userId, String status, String type, long page, long pageSize);

    PaymentDetail getPaymentDetail(String userId, String paymentId);

    String generatePaymentNo();
}
