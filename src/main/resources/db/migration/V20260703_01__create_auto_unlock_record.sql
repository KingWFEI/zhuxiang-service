CREATE TABLE auto_unlock_record (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(36) NOT NULL COMMENT '触发自动开锁的用户ID',
    lease_id VARCHAR(36) NOT NULL COMMENT '关联租约ID',
    smart_lock_id VARCHAR(36) NOT NULL COMMENT '本地智能门锁ID',
    ttlock_lock_id BIGINT NOT NULL COMMENT 'TTLock平台门锁ID',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发方式，当前固定AUTO_NEARBY',
    rssi INT NOT NULL COMMENT '触发时稳定窗口平均RSSI',
    stable_millis INT NOT NULL COMMENT '信号稳定持续毫秒数',
    result VARCHAR(16) NOT NULL COMMENT 'SUCCESS或FAILED',
    failure_reason VARCHAR(64) NULL COMMENT '脱敏后的失败错误码',
    device_info VARCHAR(255) NOT NULL COMMENT '客户端设备与系统摘要',
    app_version VARCHAR(64) NOT NULL COMMENT 'App版本',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_auto_unlock_user_created (user_id, created_at),
    KEY idx_auto_unlock_lease_created (lease_id, created_at),
    KEY idx_auto_unlock_lock_created (smart_lock_id, created_at)
) COMMENT='无感开锁结果审计记录';
