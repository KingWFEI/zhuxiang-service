ALTER TABLE smart_locks
    MODIFY COLUMN lock_id BIGINT NULL COMMENT '通通锁开放平台返回的门锁ID',
    MODIFY COLUMN key_id BIGINT NULL COMMENT '通通锁开放平台返回的管理员eKey ID',
    MODIFY COLUMN status VARCHAR(32) NOT NULL DEFAULT 'INIT_LOCAL_SUCCESS' COMMENT '门锁初始化/绑定状态：INIT_LOCAL_SUCCESS本地初始化成功，PLATFORM_BINDING同步中，PLATFORM_BOUND平台已同步，BOUND已绑定，PLATFORM_FAILED平台同步失败，DISABLED已禁用，UNBOUND已解绑',
    ADD COLUMN platform_error_code VARCHAR(64) NULL COMMENT '开放平台同步失败错误码' AFTER status,
    ADD COLUMN platform_error_message VARCHAR(500) NULL COMMENT '开放平台同步失败错误信息' AFTER platform_error_code,
    ADD COLUMN last_sync_time TIMESTAMP NULL COMMENT '最近一次同步开放平台时间' AFTER platform_error_message;
