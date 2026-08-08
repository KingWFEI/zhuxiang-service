package com.zhuxiang.service.realtime;

import com.zhuxiang.service.event.MessageRealtimeEvent;

public interface MessageEventBroker {

    void publish(MessageRealtimeEvent event);
}
