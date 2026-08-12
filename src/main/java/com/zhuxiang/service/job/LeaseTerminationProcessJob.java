package com.zhuxiang.service.job;

import com.zhuxiang.service.mapper.LeaseTerminationApplicationMapper;
import com.zhuxiang.service.service.LeaseTerminationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LeaseTerminationProcessJob {
    private static final Logger log = LoggerFactory.getLogger(LeaseTerminationProcessJob.class);
    private final LeaseTerminationApplicationMapper mapper;
    private final LeaseTerminationService service;

    public LeaseTerminationProcessJob(LeaseTerminationApplicationMapper mapper,
                                      LeaseTerminationService service) {
        this.mapper = mapper;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.lease-termination.scan-ms:30000}",
            initialDelayString = "${app.lease-termination.scan-ms:30000}")
    public void advancePendingFlows() {
        for (String id : mapper.selectProcessableIds(LocalDateTime.now(), 100)) {
            try {
                service.processPendingFlow(id);
            } catch (Exception exception) {
                log.error("退租流程补偿任务失败 applicationId={}", id, exception);
            }
        }
    }
}
