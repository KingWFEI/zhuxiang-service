ALTER TABLE rent_order
    ADD COLUMN pre_payment_deadline_at TIMESTAMP NULL
        COMMENT '支付前当前办理阶段截止时间';

CREATE INDEX idx_rent_order_pre_payment_deadline
    ON rent_order(status, pre_payment_deadline_at);

UPDATE rent_order
SET pre_payment_deadline_at = DATE_ADD(COALESCE(updated_at, created_at), INTERVAL 5 MINUTE)
WHERE status IN ('pendingRealName', 'pendingContract', 'pendingTenantSign')
  AND paid_at IS NULL
  AND pre_payment_deadline_at IS NULL;
