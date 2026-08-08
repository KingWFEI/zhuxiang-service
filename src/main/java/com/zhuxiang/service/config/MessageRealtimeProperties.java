package com.zhuxiang.service.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@ConfigurationProperties(prefix = "app.message.realtime")
public class MessageRealtimeProperties {

    private boolean enabled = true;

    @Pattern(regexp = "memory|redis")
    private String broker = "memory";

    @Min(5_000)
    private long heartbeatMs = 20_000;

    @Min(60_000)
    private long connectionTimeoutMs = 1_800_000;

    @Min(1)
    private int maxConnectionsPerUser = 3;

    @Min(0)
    @Max(100)
    private int rolloutPercent = 100;

    @NotBlank
    private String redisChannel = "wuyou:message-events";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public long getHeartbeatMs() {
        return heartbeatMs;
    }

    public void setHeartbeatMs(long heartbeatMs) {
        this.heartbeatMs = heartbeatMs;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getMaxConnectionsPerUser() {
        return maxConnectionsPerUser;
    }

    public int getRolloutPercent() {
        return rolloutPercent;
    }

    public void setRolloutPercent(int rolloutPercent) {
        this.rolloutPercent = rolloutPercent;
    }

    public void setMaxConnectionsPerUser(int maxConnectionsPerUser) {
        this.maxConnectionsPerUser = maxConnectionsPerUser;
    }

    public String getRedisChannel() {
        return redisChannel;
    }

    public void setRedisChannel(String redisChannel) {
        this.redisChannel = redisChannel;
    }
}
