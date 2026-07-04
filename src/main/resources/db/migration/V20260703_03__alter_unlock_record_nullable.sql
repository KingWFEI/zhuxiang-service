ALTER TABLE unlock_record
    MODIFY rssi INT NULL COMMENT '开锁时信号值',
    MODIFY stable_millis INT NULL COMMENT '信号稳定时长(ms)',
    MODIFY device_info VARCHAR(255) NULL COMMENT '设备及系统摘要',
    MODIFY app_version VARCHAR(64) NULL COMMENT 'App版本号';

DROP INDEX idx_auto_unlock_user_created ON unlock_record;
DROP INDEX idx_auto_unlock_lease_created ON unlock_record;
DROP INDEX idx_auto_unlock_lock_created ON unlock_record;

CREATE INDEX idx_unlock_user_time ON unlock_record (user_id, created_at DESC);
CREATE INDEX idx_unlock_lease ON unlock_record (lease_id);
