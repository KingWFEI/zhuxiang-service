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

    /** 进入该半径后才启动门锁蓝牙扫描，单位米。 */
    private int geofenceRadiusMeters = 150;

    /** 超出该半径并持续一段时间后，才重新布防下一次自动开锁。 */
    private int exitRadiusMeters = 200;

    /** 离开房屋范围的最短持续时间，单位秒。 */
    private int exitDwellSeconds = 90;

    /** 返回房屋范围后的蓝牙扫描窗口，单位秒。 */
    private int scanWindowSeconds = 900;

    /** 达到 RSSI 阈值所需的最少稳定采样数。 */
    private int minRssiSamples = 5;

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

    public int getGeofenceRadiusMeters() {
        return geofenceRadiusMeters;
    }

    public void setGeofenceRadiusMeters(int geofenceRadiusMeters) {
        this.geofenceRadiusMeters = geofenceRadiusMeters;
    }

    public int getExitRadiusMeters() {
        return exitRadiusMeters;
    }

    public void setExitRadiusMeters(int exitRadiusMeters) {
        this.exitRadiusMeters = exitRadiusMeters;
    }

    public int getExitDwellSeconds() {
        return exitDwellSeconds;
    }

    public void setExitDwellSeconds(int exitDwellSeconds) {
        this.exitDwellSeconds = exitDwellSeconds;
    }

    public int getScanWindowSeconds() {
        return scanWindowSeconds;
    }

    public void setScanWindowSeconds(int scanWindowSeconds) {
        this.scanWindowSeconds = scanWindowSeconds;
    }

    public int getMinRssiSamples() {
        return minRssiSamples;
    }

    public void setMinRssiSamples(int minRssiSamples) {
        this.minRssiSamples = minRssiSamples;
    }
}
