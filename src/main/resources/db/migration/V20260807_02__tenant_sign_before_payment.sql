ALTER TABLE rent_order
    ADD COLUMN payment_deadline_at TIMESTAMP NULL
        COMMENT '租客签署完成后的支付截止时间',
    ADD COLUMN cancel_reason VARCHAR(64) NULL
        COMMENT '订单取消或失效原因';

CREATE INDEX idx_rent_order_payment_deadline
    ON rent_order(status, payment_deadline_at);

-- 旧版本 pendingEsign 订单统一回到租客签署阶段；服务会根据 paid_at 自动兼容已支付订单。
UPDATE rent_order
SET status = 'pendingTenantSign', updated_at = CURRENT_TIMESTAMP
WHERE status = 'pendingEsign';

ALTER TABLE rent_order
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'pendingRealName'
        COMMENT '订单状态：pendingRealName待实名/pendingContract待确认合同/pendingTenantSign待租客签署/pendingPayment待支付/pendingLandlordSign待房东签署/paymentExpired支付超时/completed已完成/cancelled已取消';
