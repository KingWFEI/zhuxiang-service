package com.zhuxiang.service.event;

import com.zhuxiang.service.service.LeaseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 处理自然到期租约并下架对应房源。 */
@Component
public class LeaseExpirationJob {

    private final LeaseService leaseService;

    public LeaseExpirationJob(LeaseService leaseService) {
        this.leaseService = leaseService;
    }

    @Scheduled(
            fixedDelayString = "${app.lease.expiration-scan-ms:300000}",
            initialDelayString = "${app.lease.expiration-scan-ms:300000}"
    )
    public void expireDueLeases() {
        leaseService.expireDueLeases();
    }
}
