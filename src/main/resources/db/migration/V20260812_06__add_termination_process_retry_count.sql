ALTER TABLE lease_termination_applications
    ADD COLUMN process_retry_count INT NOT NULL DEFAULT 0
        COMMENT '退租异步流程连续重试次数'
        AFTER process_retry_at;
