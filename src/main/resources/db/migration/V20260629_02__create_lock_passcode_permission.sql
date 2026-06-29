ALTER TABLE smart_locks
    ADD COLUMN keyboard_pwd_version INT NULL COMMENT 'TTLock键盘密码版本：0至4' AFTER key_id,
    ADD COLUMN timezone_raw_offset BIGINT NULL COMMENT '门锁时区相对UTC的毫秒偏移量' AFTER keyboard_pwd_version;

CREATE TABLE lock_passcode_permission (
    id VARCHAR(36) NOT NULL COMMENT '密码权限记录ID',
    lease_id VARCHAR(36) NOT NULL COMMENT '关联租约ID',
    tenant_id VARCHAR(36) NOT NULL COMMENT '租客用户ID',
    smart_lock_id VARCHAR(36) NOT NULL COMMENT '本地智能门锁ID',
    ttlock_lock_id BIGINT NOT NULL COMMENT 'TTLock平台门锁ID',
    ttlock_keyboard_pwd_id BIGINT NULL COMMENT 'TTLock平台键盘密码ID',
    keyboard_pwd_ciphertext VARCHAR(1024) NULL COMMENT 'AES-GCM密文封装，包含密钥版本、nonce和认证标签',
    keyboard_pwd_type INT NOT NULL COMMENT '键盘密码类型，本期固定为3期限密码',
    keyboard_pwd_version INT NULL COMMENT 'TTLock键盘密码版本，本期仅支持4',
    start_time TIMESTAMP(3) NULL COMMENT '密码有效期开始时刻，能力校验成功后写入',
    end_time TIMESTAMP(3) NULL COMMENT '密码有效期结束时刻，能力校验成功后写入',
    status VARCHAR(20) NOT NULL COMMENT '权限状态：ACTIVE生效、FAILED失败、REVOKED撤销、EXPIRED过期',
    device_sync_status VARCHAR(32) NOT NULL COMMENT '设备同步状态：NOT_REQUIRED无需同步、PENDING_BLUETOOTH_DELETE待蓝牙删除、DELETED已删除',
    error_message VARCHAR(500) NULL COMMENT '脱敏后的最近一次失败原因',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_lock_passcode_lease_lock (lease_id, smart_lock_id),
    KEY idx_lock_passcode_tenant_status (tenant_id, status),
    KEY idx_lock_passcode_end_status (end_time, status),
    KEY idx_lock_passcode_ttlock_pwd (ttlock_keyboard_pwd_id)
) COMMENT='租约TTLock期限密码权限表';
