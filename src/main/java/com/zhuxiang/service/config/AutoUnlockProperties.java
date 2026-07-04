package com.zhuxiang.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 租客无感开锁下发配置。
 */
@Component
@ConfigurationProperties(prefix = "app.auto-unlock")
public class AutoUnlockProperties {

    /** 是否允许客户端使用无感开锁。 */
    private boolean enabled = true;

    /** 采样窗口内平均 RSSI 下限。 */
    private int minRssi = -60;

    /** 信号持续稳定时间。 */
    private int stableMillis = 2000;

    /** 成功开锁后的冷却秒数。 */
    private int cooldownSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinRssi() {
        return minRssi;
    }

    public void setMinRssi(int minRssi) {
        this.minRssi = minRssi;
    }

    public int getStableMillis() {
        return stableMillis;
    }

    public void setStableMillis(int stableMillis) {
        this.stableMillis = stableMillis;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(int cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }
}
