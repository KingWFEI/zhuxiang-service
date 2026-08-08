package com.zhuxiang.service.event;

import com.zhuxiang.service.realtime.MessageEventBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(
        name = "app.message.realtime.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MessageRealtimeEventRelay {

    private static final Logger log = LoggerFactory.getLogger(MessageRealtimeEventRelay.class);

    private final MessageEventBroker eventBroker;

    public MessageRealtimeEventRelay(MessageEventBroker eventBroker) {
        this.eventBroker = eventBroker;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void afterCommit(MessageRealtimeEvent event) {
        try {
            eventBroker.publish(event);
        } catch (RuntimeException exception) {
            // 站内消息已经提交，实时通道失败不能反向影响原业务接口；客户端会通过REST补偿。
            log.warn("实时消息发布失败: eventId={}, userId={}, reason={}",
                    event.eventId(), event.userId(), exception.getMessage());
        }
    }
}
