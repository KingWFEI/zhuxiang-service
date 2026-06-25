CREATE TABLE smart_locks (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    house_id VARCHAR(36) NOT NULL COMMENT '绑定的房源ID',
    room_id VARCHAR(36) NULL COMMENT '绑定的房间ID，可为空',
    lock_id BIGINT NOT NULL COMMENT '通通锁开放平台返回的门锁ID',
    key_id BIGINT NOT NULL COMMENT '通通锁开放平台返回的管理员eKey ID',
    lock_name VARCHAR(100) NOT NULL COMMENT '门锁名称',
    lock_mac VARCHAR(64) NOT NULL COMMENT '门锁MAC地址',
    lock_data TEXT NOT NULL COMMENT '通通锁SDK生成的门锁控制数据',
    status VARCHAR(32) NOT NULL DEFAULT 'BOUND' COMMENT '门锁绑定状态：BOUND已绑定，DISABLED已禁用，UNBOUND已解绑',
    battery INT NULL COMMENT '门锁电量',
    rssi INT NULL COMMENT '初始化扫描时的蓝牙信号强度',
    bind_time TIMESTAMP NOT NULL COMMENT '绑定时间',
    created_by VARCHAR(36) NULL COMMENT '初始化绑定操作人ID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_smart_locks_lock_id (lock_id),
    UNIQUE KEY uk_smart_locks_lock_mac (lock_mac),
    KEY idx_smart_locks_house_id (house_id),
    KEY idx_smart_locks_room_id (room_id)
) COMMENT='智能门锁绑定表';

ALTER TABLE house
    ADD COLUMN smart_lock_id VARCHAR(36) NULL COMMENT '当前绑定的智能门锁ID' AFTER is_self_viewing_supported,
    ADD COLUMN lock_bind_status VARCHAR(32) NOT NULL DEFAULT 'UNBOUND' COMMENT '门锁绑定状态：UNBOUND未绑定，BOUND已绑定' AFTER smart_lock_id;
