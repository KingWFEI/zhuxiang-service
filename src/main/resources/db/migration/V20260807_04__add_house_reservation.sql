ALTER TABLE house
    ADD COLUMN reserved_order_id VARCHAR(36) NULL
        COMMENT '当前锁房订单ID',
    ADD COLUMN reserved_until TIMESTAMP NULL
        COMMENT '当前锁房截止时间';

CREATE INDEX idx_house_reservation
    ON house(status, reserved_until);

-- 旧逻辑在合同预览阶段就锁房；部署新逻辑时先释放这类无效占用。
UPDATE house h
SET h.status = 'available',
    h.reserved_order_id = NULL,
    h.reserved_until = NULL,
    h.updated_at = CURRENT_TIMESTAMP
WHERE h.status = 'reserved'
  AND NOT EXISTS (
      SELECT 1
      FROM rent_order ro
      WHERE ro.house_id = h.id
        AND ro.status IN ('pendingTenantSign', 'pendingPayment', 'pendingLandlordSign')
  );

-- 为确实处于签署、支付或支付后阶段的存量订单补齐锁房归属。
UPDATE house h
JOIN rent_order ro ON ro.id = (
    SELECT ro2.id
    FROM rent_order ro2
    WHERE ro2.house_id = h.id
      AND ro2.status IN ('pendingTenantSign', 'pendingPayment', 'pendingLandlordSign')
    ORDER BY ro2.updated_at DESC
    LIMIT 1
)
SET h.reserved_order_id = ro.id,
    h.reserved_until = CASE
        WHEN ro.status = 'pendingPayment' THEN ro.payment_deadline_at
        WHEN ro.status = 'pendingTenantSign' THEN ro.pre_payment_deadline_at
        ELSE NULL
    END
WHERE h.status = 'reserved';
