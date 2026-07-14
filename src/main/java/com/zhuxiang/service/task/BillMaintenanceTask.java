package com.zhuxiang.service.task;

import com.zhuxiang.service.service.BillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日凌晨 2 点执行账单状态维护：
 * 1. scheduled 账单到期 → 激活为 pending
 * 2. pending 账单逾期 → 标记为 overdue 并计算滞纳金
 */
@Component
public class BillMaintenanceTask {

    private static final Logger log = LoggerFactory.getLogger(BillMaintenanceTask.class);

    private final BillService billService;

    public BillMaintenanceTask(BillService billService) {
        this.billService = billService;
    }

    @Scheduled(cron = "0 2 0 * * *")
    public void run() {
        log.info("开始账单状态维护");
        try {
            int activated = billService.activateScheduledBills();
            if (activated > 0) log.info("激活 {} 条到期账单", activated);

            int overdue = billService.markOverdueBills();
            if (overdue > 0) log.info("标记 {} 条逾期账单", overdue);
        } catch (Exception e) {
            log.error("账单状态维护异常", e);
        }
    }
}
