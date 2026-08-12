package com.zhuxiang.service.job;

import com.zhuxiang.service.mapper.PaymentRefundMapper;
import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.PaymentRefundService;
import com.zhuxiang.service.service.RentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentRefundJob {
    private static final Logger log = LoggerFactory.getLogger(PaymentRefundJob.class);
    private static final int BATCH_SIZE = 100;

    private final PaymentRefundMapper refundMapper;
    private final RentOrderMapper orderMapper;
    private final PaymentRefundService refundService;
    private final RentOrderService orderService;

    public PaymentRefundJob(PaymentRefundMapper refundMapper,
                            RentOrderMapper orderMapper,
                            PaymentRefundService refundService,
                            RentOrderService orderService) {
        this.refundMapper = refundMapper;
        this.orderMapper = orderMapper;
        this.refundService = refundService;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.payment-refund.scan-ms:15000}",
            initialDelayString = "${app.payment-refund.scan-ms:15000}")
    public void processRefundsAndLandlordTimeouts() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredOrders = orderMapper.selectExpiredLandlordSignOrderIds(now, BATCH_SIZE);
        for (String orderId : expiredOrders) {
            try {
                orderService.processLandlordSignTimeout(orderId);
            } catch (Exception ex) {
                log.error("处理房东签约超时退款失败 orderId={}", orderId, ex);
            }
        }

        List<String> refundIds = refundMapper.selectRetryableIds(now, BATCH_SIZE);
        for (String refundId : refundIds) {
            try {
                refundService.processRefund(refundId);
            } catch (Exception ex) {
                log.error("处理支付退款任务失败 refundId={}", refundId, ex);
            }
        }
    }
}
