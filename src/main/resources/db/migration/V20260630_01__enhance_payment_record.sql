ALTER TABLE payment_record
    ADD COLUMN payment_no VARCHAR(32) COMMENT '支付编号' AFTER id,
    ADD COLUMN house_id VARCHAR(36) COMMENT '房源ID' AFTER user_id,
    ADD COLUMN house_name VARCHAR(255) COMMENT '房源名称' AFTER house_id,
    ADD COLUMN bill_id VARCHAR(36) COMMENT '关联账单ID' AFTER order_id,
    ADD COLUMN lease_id VARCHAR(36) COMMENT '关联租约ID' AFTER bill_id,
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'rent' COMMENT '支付类型：rent租金 deposit押金 service_fee服务费 refund退款' AFTER lease_id,
    ADD COLUMN remark VARCHAR(500) COMMENT '备注' AFTER callback_payload,
    ADD INDEX idx_payment_record_user (user_id),
    ADD INDEX idx_payment_record_lease (lease_id),
    ADD INDEX idx_payment_record_bill (bill_id);

-- 为已有记录生成支付编号（基于 ID 的哈希片段确保唯一性）
UPDATE payment_record SET payment_no = CONCAT('ZF', DATE_FORMAT(COALESCE(paid_at, created_at), '%Y%m%d'), SUBSTR(REPLACE(id, '-', ''), 1, 8)) WHERE payment_no IS NULL;

ALTER TABLE payment_record MODIFY payment_no VARCHAR(32) NOT NULL;
CREATE UNIQUE INDEX uk_payment_no ON payment_record(payment_no);
