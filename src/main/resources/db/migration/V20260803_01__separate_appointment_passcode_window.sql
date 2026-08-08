ALTER TABLE appointment_access_grant
    ADD COLUMN passcode_valid_from DATETIME(3) NULL
        COMMENT 'TTLock期限密码整点生效时间' AFTER valid_to,
    ADD COLUMN passcode_valid_to DATETIME(3) NULL
        COMMENT 'TTLock期限密码整点失效时间' AFTER passcode_valid_from;

-- 旧预约密码使用了蓝牙前后缓冲时间，分秒不一定为零，不能继续视为可用。
-- 保留已下发的 eKey，让维护任务仅重新生成符合整点规则的期限密码。
UPDATE appointment_access_grant
SET passcode_status = 'PENDING',
    ttlock_keyboard_pwd_id = NULL,
    keyboard_pwd_ciphertext = NULL,
    keyboard_pwd_type = NULL,
    passcode_error_message = NULL,
    status = CASE WHEN ekey_status = 'ACTIVE' THEN 'PARTIAL' ELSE 'PENDING' END,
    next_retry_at = CURRENT_TIMESTAMP(3),
    updated_at = CURRENT_TIMESTAMP(3)
WHERE passcode_status = 'ACTIVE'
  AND valid_to >= CURRENT_TIMESTAMP(3);
