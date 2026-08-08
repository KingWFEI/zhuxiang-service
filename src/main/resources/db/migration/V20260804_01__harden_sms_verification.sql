ALTER TABLE sms_code
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0 COMMENT '验证码校验失败次数' AFTER used_at;
