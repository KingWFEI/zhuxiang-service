ALTER TABLE appointment
    ADD COLUMN landlord_id VARCHAR(36) NULL COMMENT '预约创建时的房东ID快照' AFTER house_id,
    ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'LANDLORD' COMMENT '房源来源快照：LANDLORD/PLATFORM' AFTER landlord_id,
    ADD COLUMN viewing_mode VARCHAR(30) NOT NULL DEFAULT 'LANDLORD_HOSTED' COMMENT '看房方式' AFTER source_type,
    ADD COLUMN appointment_start_at DATETIME(3) NULL COMMENT '预约开始时间' AFTER appointment_date,
    ADD COLUMN appointment_end_at DATETIME(3) NULL COMMENT '预约结束时间' AFTER appointment_start_at,
    ADD COLUMN confirm_deadline_at DATETIME(3) NULL COMMENT '接待方确认截止时间',
    ADD COLUMN confirmed_by VARCHAR(36) NULL COMMENT '确认人ID',
    ADD COLUMN confirmed_at DATETIME(3) NULL COMMENT '确认时间',
    ADD COLUMN host_user_id VARCHAR(36) NULL COMMENT '实际接待人ID',
    ADD COLUMN meeting_point VARCHAR(255) NULL COMMENT '见面地点',
    ADD COLUMN viewing_instruction VARCHAR(500) NULL COMMENT '看房说明',
    ADD COLUMN reject_reason VARCHAR(500) NULL COMMENT '拒绝原因',
    ADD COLUMN cancel_reason VARCHAR(500) NULL COMMENT '取消原因',
    ADD COLUMN cancelled_by VARCHAR(36) NULL COMMENT '取消人ID',
    ADD COLUMN cancelled_at DATETIME(3) NULL COMMENT '取消时间',
    ADD COLUMN proposed_start_at DATETIME(3) NULL COMMENT '建议改期开始时间',
    ADD COLUMN proposed_end_at DATETIME(3) NULL COMMENT '建议改期结束时间',
    ADD COLUMN reschedule_reason VARCHAR(500) NULL COMMENT '改期原因',
    ADD COLUMN reschedule_deadline_at DATETIME(3) NULL COMMENT '改期确认截止时间',
    ADD COLUMN checkin_code_ciphertext VARCHAR(1024) NULL COMMENT '陪同看房核验码AES-GCM密文',
    ADD COLUMN checked_in_at DATETIME(3) NULL COMMENT '签到时间',
    ADD COLUMN completed_at DATETIME(3) NULL COMMENT '完成时间',
    ADD COLUMN active_slot_key VARCHAR(160) NULL COMMENT '活跃预约时段唯一键',
    ADD COLUMN idempotency_key VARCHAR(100) NULL COMMENT '创建预约幂等键',
    ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    ADD UNIQUE KEY uk_appointment_active_slot (active_slot_key),
    ADD UNIQUE KEY uk_appointment_user_idempotency (user_id, idempotency_key),
    ADD KEY idx_appointment_user_start (user_id, appointment_start_at),
    ADD KEY idx_appointment_landlord_status (landlord_id, status),
    ADD KEY idx_appointment_source_status (source_type, status),
    ADD KEY idx_appointment_confirm_deadline (status, confirm_deadline_at),
    ADD KEY idx_appointment_start_status (status, appointment_start_at);

UPDATE appointment
SET status = CASE LOWER(status)
    WHEN 'pending' THEN 'PENDING_CONFIRMATION'
    WHEN 'confirmed' THEN 'CONFIRMED'
    WHEN 'cancelled' THEN 'CANCELLED'
    WHEN 'completed' THEN 'COMPLETED'
    WHEN 'no_show' THEN 'NO_SHOW'
    ELSE UPPER(status)
END;

UPDATE appointment
SET appointment_start_at = STR_TO_DATE(
        CONCAT(appointment_date, ' ', SUBSTRING_INDEX(time_slot, '-', 1)),
        '%Y-%m-%d %H:%i'
    ),
    appointment_end_at = STR_TO_DATE(
        CONCAT(appointment_date, ' ', SUBSTRING_INDEX(time_slot, '-', -1)),
        '%Y-%m-%d %H:%i'
    )
WHERE appointment_start_at IS NULL
  AND appointment_date IS NOT NULL
  AND time_slot REGEXP '^[0-9]{2}:[0-9]{2}-[0-9]{2}:[0-9]{2}$';

UPDATE appointment a
JOIN house h ON h.id = a.house_id
SET a.landlord_id = h.landlord_id,
    a.source_type = COALESCE(NULLIF(h.source_type, ''), 'LANDLORD'),
    a.viewing_mode = CASE
        WHEN UPPER(COALESCE(h.source_type, 'LANDLORD')) = 'PLATFORM'
            AND h.is_self_viewing_supported = 1
            AND h.smart_lock_id IS NOT NULL
            AND UPPER(COALESCE(h.lock_bind_status, '')) = 'BOUND'
        THEN 'SELF_SERVICE_LOCK'
        WHEN UPPER(COALESCE(h.source_type, 'LANDLORD')) = 'PLATFORM'
        THEN 'PLATFORM_HOSTED'
        ELSE 'LANDLORD_HOSTED'
    END
WHERE a.landlord_id IS NULL;

CREATE TABLE appointment_access_grant (
    id VARCHAR(36) NOT NULL,
    appointment_id VARCHAR(36) NOT NULL,
    tenant_id VARCHAR(36) NOT NULL,
    house_id VARCHAR(36) NOT NULL,
    smart_lock_id VARCHAR(36) NOT NULL,
    ttlock_lock_id BIGINT NOT NULL,
    valid_from DATETIME(3) NOT NULL,
    valid_to DATETIME(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ekey_status VARCHAR(20) NOT NULL,
    ttlock_key_id BIGINT NULL,
    receiver_username VARCHAR(100) NULL,
    ekey_error_message VARCHAR(500) NULL,
    passcode_status VARCHAR(20) NOT NULL,
    ttlock_keyboard_pwd_id BIGINT NULL,
    keyboard_pwd_ciphertext VARCHAR(1024) NULL,
    keyboard_pwd_type INT NULL,
    passcode_error_message VARCHAR(500) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(3) NULL,
    granted_at DATETIME(3) NULL,
    revoked_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_appointment_access (appointment_id),
    KEY idx_appointment_access_retry (status, next_retry_at),
    KEY idx_appointment_access_tenant (tenant_id, status),
    KEY idx_appointment_access_end (valid_to, status)
) COMMENT='预约看房短期门锁权限';

CREATE TABLE appointment_status_log (
    id VARCHAR(36) NOT NULL,
    appointment_id VARCHAR(36) NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    operator_id VARCHAR(36) NULL,
    operator_role VARCHAR(30) NOT NULL,
    reason VARCHAR(500) NULL,
    metadata_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_appointment_status_log (appointment_id, created_at)
) COMMENT='预约状态变更日志';

CREATE TABLE house_viewing_config (
    house_id VARCHAR(36) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    viewing_mode VARCHAR(30) NULL COMMENT '为空时按房源来源和门锁状态自动判定',
    duration_minutes INT NOT NULL DEFAULT 60,
    advance_min_minutes INT NOT NULL DEFAULT 30,
    advance_max_days INT NOT NULL DEFAULT 14,
    confirmation_timeout_minutes INT NOT NULL DEFAULT 120,
    reschedule_timeout_minutes INT NOT NULL DEFAULT 120,
    lock_grant_lead_minutes INT NOT NULL DEFAULT 15,
    lock_valid_before_minutes INT NOT NULL DEFAULT 10,
    lock_valid_after_minutes INT NOT NULL DEFAULT 10,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Shanghai',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (house_id)
) COMMENT='房源预约看房配置';
