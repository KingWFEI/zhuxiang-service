ALTER TABLE rent_order
    ADD COLUMN landlord_sign_deadline_at DATETIME NULL
        COMMENT '支付成功后房东签约截止时间' AFTER payment_deadline_at,
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'pendingRealName'
        COMMENT 'pendingRealName/pendingContract/pendingTenantSign/pendingPayment/pendingLandlordSign/paymentExpired/refundPending/refunded/refundFailed/completed/cancelled';

UPDATE rent_order
SET landlord_sign_deadline_at = DATE_ADD(COALESCE(paid_at, updated_at, created_at), INTERVAL 1 DAY)
WHERE status = 'pendingLandlordSign'
  AND landlord_sign_deadline_at IS NULL;

CREATE TABLE payment_refund (
    id                  VARCHAR(36)  NOT NULL,
    refund_no           VARCHAR(64)  NOT NULL COMMENT '商户退款请求号(out_request_no)，重试保持不变',
    payment_record_id   VARCHAR(36)  NOT NULL COMMENT '原支付记录ID',
    order_id            VARCHAR(36)  NOT NULL COMMENT '租房订单ID',
    user_id             VARCHAR(36)  NOT NULL COMMENT '付款用户ID',
    refund_amount       INT          NOT NULL COMMENT '退款金额，单位分',
    refund_reason       VARCHAR(500) NOT NULL,
    trigger_type        VARCHAR(32)  NOT NULL COMMENT 'USER_CANCEL/LANDLORD_SIGN_TIMEOUT/LATE_PAYMENT',
    status              VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/success/failed',
    channel_trade_no    VARCHAR(128) NULL COMMENT '支付宝交易号',
    attempt_count       INT          NOT NULL DEFAULT 0,
    last_error          VARCHAR(500) NULL,
    next_retry_at       DATETIME     NULL,
    refunded_at         DATETIME     NULL,
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_refund_no (refund_no),
    UNIQUE KEY uk_payment_refund_payment (payment_record_id),
    KEY idx_payment_refund_retry (status, next_retry_at),
    KEY idx_payment_refund_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付原路退款单';
