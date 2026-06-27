ALTER TABLE lock_permission
    CHANGE COLUMN user_id tenant_id VARCHAR(36) NOT NULL COMMENT '租客用户ID',
    CHANGE COLUMN lock_id smart_lock_id VARCHAR(36) NOT NULL COMMENT '本地智能门锁ID',
    CHANGE COLUMN valid_from start_time TIMESTAMP NOT NULL COMMENT 'eKey有效期开始时间',
    CHANGE COLUMN valid_to end_time TIMESTAMP NOT NULL COMMENT 'eKey有效期结束时间',
    ADD COLUMN house_id VARCHAR(36) NULL COMMENT '房源ID，当前作为房间唯一标识' AFTER tenant_id,
    ADD COLUMN ttlock_lock_id BIGINT NULL COMMENT 'TTLock平台门锁ID' AFTER smart_lock_id,
    ADD COLUMN ttlock_key_id BIGINT NULL COMMENT 'TTLock平台租客eKey ID' AFTER ttlock_lock_id,
    ADD COLUMN receiver_username VARCHAR(100) NULL COMMENT 'TTLock接收账号，手机号或邮箱' AFTER ttlock_key_id,
    ADD COLUMN permission_type VARCHAR(20) NOT NULL DEFAULT 'EKEY' COMMENT '权限类型' AFTER receiver_username,
    ADD COLUMN error_message VARCHAR(500) NULL COMMENT '最近一次下发失败原因' AFTER status;

UPDATE lock_permission
SET status = UPPER(status),
    permission_type = 'EKEY';

UPDATE lock_permission permission
JOIN lease lease_record ON lease_record.id = permission.lease_id
SET permission.house_id = lease_record.house_id
WHERE permission.house_id IS NULL;

ALTER TABLE lock_permission
    MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID，当前作为房间唯一标识',
    ADD UNIQUE KEY uk_lock_permission_lease_tenant_lock_type
        (lease_id, tenant_id, smart_lock_id, permission_type),
    ADD KEY idx_lock_permission_tenant_status (tenant_id, status),
    ADD KEY idx_lock_permission_ttlock_key (ttlock_key_id);
