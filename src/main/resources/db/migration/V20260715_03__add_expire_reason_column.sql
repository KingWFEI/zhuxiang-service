ALTER TABLE user_real_name_auth
    ADD COLUMN expire_reason VARCHAR(64) NULL COMMENT '过期原因：USER_RESTARTED等' AFTER failure_message;
