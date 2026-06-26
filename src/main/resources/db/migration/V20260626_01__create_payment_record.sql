CREATE TABLE payment_record (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    amount INT NOT NULL COMMENT '支付金额，单位：分',
    payment_channel VARCHAR(20) NOT NULL COMMENT '支付渠道：wechat/alipay/mock',
    channel_trade_no VARCHAR(128) COMMENT '渠道交易号',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/success/failed/refunded',
    fee_breakdown JSON COMMENT '费用明细：[{"type":"rent|deposit|service_fee","amount":,"description":""}]',
    paid_at TIMESTAMP NULL,
    callback_time TIMESTAMP NULL,
    callback_payload TEXT COMMENT '回调原始报文',
    refund_to_record_id VARCHAR(36) COMMENT '退款关联的原支付记录ID',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_payment_record_order (order_id),
    INDEX idx_payment_record_channel_trade_no (channel_trade_no)
);
