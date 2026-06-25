ALTER TABLE smart_locks
    ADD COLUMN last_ble_sync_time TIMESTAMP NULL COMMENT '最近一次蓝牙状态刷新时间' AFTER rssi,
    ADD COLUMN battery_source VARCHAR(32) NULL COMMENT '电量来源：BLE_LAST_SYNC最近一次蓝牙扫描，MANUAL手动录入，UNKNOWN未知' AFTER last_ble_sync_time;
