package com.zhuxiang.service.job;

import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.RentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 每分钟处理租客签署后超过 15 分钟仍未支付的租房订单。 */
@Component
public class PaymentTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutJob.class);
    private static final int BATCH_SIZE = 100;

    private final RentOrderMapper rentOrderMapper;
    private final RentOrderService rentOrderService;

    public PaymentTimeoutJob(RentOrderMapper rentOrderMapper, RentOrderService rentOrderService) {
        this.rentOrderMapper = rentOrderMapper;
        this.rentOrderService = rentOrderService;
    }

    @Scheduled(
            fixedDelayString = "${app.rent-order.payment-timeout-scan-ms:60000}",
            initialDelayString = "${app.rent-order.payment-timeout-scan-ms:60000}"
    )
    public void processExpiredPayments() {
        List<String> orderIds = rentOrderMapper.selectExpiredPaymentOrderIds(
                LocalDateTime.now(), BATCH_SIZE);
        for (String orderId : orderIds) {
            try {
                rentOrderService.processPaymentTimeout(orderId);
            } catch (Exception ex) {
                log.error("处理租客签署后支付超时订单失败: orderId={}", orderId, ex);
            }
        }
        if (!orderIds.isEmpty()) {
            log.info("本次扫描到 {} 个租客签署后支付超时订单", orderIds.size());
        }
    }
}
