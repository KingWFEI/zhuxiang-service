ALTER TABLE customer_service_message
    ADD COLUMN error_message VARCHAR(1000) NULL COMMENT 'AI 消息失败原因' AFTER status;
