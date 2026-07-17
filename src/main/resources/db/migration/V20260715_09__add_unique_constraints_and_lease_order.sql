-- ============================================================
-- 1. 租约表增加 order_id，建立与订单的直接关联
-- ============================================================
ALTER TABLE lease
    ADD COLUMN order_id VARCHAR(36) NULL COMMENT '关联订单ID' AFTER contract_id;

-- 从已有合同记录回填 order_id
UPDATE lease l
    INNER JOIN rent_contract c ON l.contract_id = c.id
SET l.order_id = c.order_id
WHERE l.order_id IS NULL AND c.order_id IS NOT NULL;

-- ============================================================
-- 2. 唯一约束：一个订单只有一份合同、一个签署流程、一个租约
-- ============================================================
ALTER TABLE rent_contract
    ADD UNIQUE INDEX uk_rent_contract_order (order_id),
    ADD UNIQUE INDEX uk_rent_contract_sign_flow (sign_flow_id);

-- lease.order_id 唯一（先不回填的保持 NULL，NULL 在唯一索引中不冲突）
ALTER TABLE lease
    ADD UNIQUE INDEX uk_lease_order (order_id);
