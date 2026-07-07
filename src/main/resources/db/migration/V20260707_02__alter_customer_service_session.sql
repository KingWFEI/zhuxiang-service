-- 客服会话表增加归档原因和最后消息时间字段
ALTER TABLE customer_service_session
    ADD COLUMN closed_reason VARCHAR(30) DEFAULT NULL COMMENT '关闭原因：TIMEOUT超时归档 USER_NEW_SESSION用户新建 USER_CLOSED用户关闭 SYSTEM_ERROR异常',
    ADD COLUMN last_message_at DATETIME DEFAULT NULL COMMENT '最后一条消息时间，用于判断会话超时';

-- 增加索引优化enter查询
ALTER TABLE customer_service_session
    ADD KEY idx_cs_session_user_active (user_id, status, last_message_at);
