package com.zhuxiang.service.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhuxiang.service.entity.CustomerServiceEnums;
import com.zhuxiang.service.entity.CustomerServiceMessage;
import com.zhuxiang.service.service.CustomerServiceMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 将服务异常或客户端断开后遗留的 STREAMING 消息恢复为 FAILED。 */
@Component
public class CustomerServiceMessageRecoveryJob {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceMessageRecoveryJob.class);
    private final CustomerServiceMessageService messageService;

    public CustomerServiceMessageRecoveryJob(CustomerServiceMessageService messageService) {
        this.messageService = messageService;
    }

    @Scheduled(fixedDelayString = "${app.customer-service.streaming-recovery-ms:60000}")
    public void recoverStaleStreamingMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(3);
        var staleMessages = messageService.list(Wrappers.<CustomerServiceMessage>lambdaQuery()
                .eq(CustomerServiceMessage::getRole, CustomerServiceEnums.MessageRole.ASSISTANT)
                .eq(CustomerServiceMessage::getStatus, CustomerServiceEnums.MessageStatus.STREAMING)
                .lt(CustomerServiceMessage::getUpdatedAt, cutoff));
        for (CustomerServiceMessage message : staleMessages) {
            messageService.markAssistantMessageFailed(
                    message.getId(), message.getContent(), "流式响应超时或客户端连接已断开");
        }
        if (!staleMessages.isEmpty()) {
            log.warn("恢复遗留客服 STREAMING 消息: count={}", staleMessages.size());
        }
    }
}
