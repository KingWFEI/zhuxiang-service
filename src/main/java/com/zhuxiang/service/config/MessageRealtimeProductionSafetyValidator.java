package com.zhuxiang.service.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class MessageRealtimeProductionSafetyValidator implements InitializingBean {

    private final MessageRealtimeProperties properties;

    public MessageRealtimeProductionSafetyValidator(MessageRealtimeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.isEnabled()
                && !"redis".equalsIgnoreCase(properties.getBroker())) {
            throw new IllegalStateException("生产环境实时消息必须使用Redis事件总线");
        }
    }
}
