package com.zhuxiang.service.job;

import com.zhuxiang.service.mapper.RentOrderMapper;
import com.zhuxiang.service.service.RentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付前流程超时订单自动关闭并释放房源。
 * <p>
 * pendingRealName / pendingContract / pendingTenantSign 每个阶段超过 5 分钟未推进即关闭。
 * pendingPayment 使用独立的 15 分钟支付超时任务；已支付的 pendingLandlordSign 不在此处处理。
 */
@Component
public class ReservedHouseExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ReservedHouseExpirationJob.class);

    private static final int BATCH_SIZE = 100;

    private final RentOrderService rentOrderService;
    private final RentOrderMapper rentOrderMapper;

    public ReservedHouseExpirationJob(RentOrderService rentOrderService,
                                      RentOrderMapper rentOrderMapper) {
        this.rentOrderService = rentOrderService;
        this.rentOrderMapper = rentOrderMapper;
    }

    @Scheduled(
            fixedDelayString = "${app.reserved-house.expiration-scan-ms:10000}",
            initialDelayString = "${app.reserved-house.expiration-scan-ms:10000}"
    )
    public void releaseExpiredReservedHouses() {
        log.debug("开始扫描支付前流程超时订单");
        int released = 0;
        try {
            LocalDateTime now = LocalDateTime.now();
            List<String> orderIds = rentOrderMapper.selectExpiredPrePaymentOrderIds(
                    now, BATCH_SIZE);
            for (String orderId : orderIds) {
                try {
                    rentOrderService.processPrePaymentTimeout(orderId, now);
                    released++;
                } catch (Exception e) {
                    log.warn("关闭支付前超时订单失败: orderId={}", orderId, e);
                }
            }
        } catch (Exception e) {
            log.error("扫描支付前流程超时订单异常", e);
        }
        if (released > 0) {
            log.info("本次处理 {} 个支付前流程超时订单", released);
        }
    }
}
