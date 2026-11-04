package com.zhuxiang.service.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 非本地环境的实时消息门禁。测试环境也会加载，用于提前暴露生产配置问题。
 */
@Component
@Profile("!dev")
public class MessageRealtimeProductionSafetyValidator implements InitializingBean {

    private final MessageRealtimeProperties properties;

    public MessageRealtimeProductionSafetyValidator(MessageRealtimeProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() {
        if (properties.isEnabled()
                && !"redis".equalsIgnoreCase(properties.getBroker())) {
            throw new IllegalStateException("非本地环境实时消息必须使用Redis事件总线");
        }
    }
}
