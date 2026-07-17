package com.zhuxiang.service.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.entity.House;
import com.zhuxiang.service.entity.RentOrder;
import com.zhuxiang.service.service.HouseService;
import com.zhuxiang.service.service.RentOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付的预定房源自动释放。
 * <p>
 * 扫描 status = 'reserved' 的房源，如果关联的待支付订单（pendingContract / pendingRealName）
 * 创建超过 30 分钟，自动取消订单并释放房源回 available。
 * <p>
 * pendingPayment / pendingSign 订单不处理（用户可能正在支付或已签约）。
 */
@Component
public class ReservedHouseExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ReservedHouseExpirationJob.class);

    /** 订单创建后超过此分钟数未支付则自动取消 */
    private static final int EXPIRE_MINUTES = 30;

    private final RentOrderService rentOrderService;
    private final HouseService houseService;

    public ReservedHouseExpirationJob(RentOrderService rentOrderService, HouseService houseService) {
        this.rentOrderService = rentOrderService;
        this.houseService = houseService;
    }

    @Scheduled(
            fixedDelayString = "${app.reserved-house.expiration-scan-ms:300000}",
            initialDelayString = "${app.reserved-house.expiration-scan-ms:300000}"
    )
    public void releaseExpiredReservedHouses() {
        log.debug("开始扫描超时未支付的预定房源");
        int released = 0;
        try {
            // 查询所有 reserved 的房源
            List<House> reservedHouses = houseService.list(
                    Wrappers.<House>lambdaQuery().eq(House::getStatus, "reserved"));
            LocalDateTime deadline = LocalDateTime.now().minusMinutes(EXPIRE_MINUTES);

            for (House house : reservedHouses) {
                // 查找该房源下超时的待支付订单
                RentOrder expiredOrder = rentOrderService.getOne(
                        Wrappers.<RentOrder>lambdaQuery()
                                .eq(RentOrder::getHouseId, house.getId())
                                .in(RentOrder::getStatus, "pendingRealName", "pendingContract")
                                .lt(RentOrder::getCreatedAt, deadline)
                                .last("LIMIT 1"),
                        false
                );
                if (expiredOrder != null) {
                    try {
                        rentOrderService.cancelOrder(expiredOrder.getUserId(), expiredOrder.getId());
                        released++;
                        log.info("超时释放预定房源: houseId={}, orderId={}, userId={}",
                                house.getId(), expiredOrder.getId(), expiredOrder.getUserId());
                    } catch (Exception e) {
                        log.warn("取消超时订单失败: orderId={}, houseId={}",
                                expiredOrder.getId(), house.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("扫描超时预定房源异常", e);
        }
        if (released > 0) {
            log.info("本次释放 {} 套超时预定房源", released);
        }
    }
}
