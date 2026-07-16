-- 租约合同表增加 e签宝 V3 电子合同字段
ALTER TABLE rent_contract
    ADD COLUMN doc_template_id   VARCHAR(64)  NULL COMMENT 'e签宝模板ID' AFTER status,
    ADD COLUMN contract_file_id  VARCHAR(64)  NULL COMMENT 'e签宝合同文件ID' AFTER doc_template_id,
    ADD COLUMN sign_flow_id      VARCHAR(64)  NULL COMMENT 'e签宝签署流程ID' AFTER contract_file_id,
    ADD COLUMN contract_num      VARCHAR(64)  NULL COMMENT '已签合同编号' AFTER sign_flow_id,
    ADD COLUMN lessor_signed     TINYINT       NOT NULL DEFAULT 0 COMMENT '甲方是否已签' AFTER contract_num,
    ADD COLUMN tenant_signed     TINYINT       NOT NULL DEFAULT 0 COMMENT '乙方是否已签' AFTER lessor_signed,
    ADD COLUMN preview_url       VARCHAR(500)  NULL COMMENT '临时预览地址' AFTER tenant_signed,
    ADD COLUMN failure_code      VARCHAR(64)   NULL COMMENT '失败业务码' AFTER preview_url,
    ADD COLUMN failure_message   VARCHAR(500)  NULL COMMENT '失败原因' AFTER failure_code,
    ADD COLUMN version           INT           NOT NULL DEFAULT 0 COMMENT '乐观锁' AFTER failure_message;

-- 订单表增加房东用户ID
ALTER TABLE rent_order
    ADD COLUMN lessor_user_id    VARCHAR(36)   NULL COMMENT '房东对应用户ID' AFTER user_id;
