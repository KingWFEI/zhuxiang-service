CREATE TABLE deposit_record (
    id VARCHAR(36) PRIMARY KEY,
    lease_id VARCHAR(36) NOT NULL COMMENT '租约ID',
    user_id VARCHAR(36) NOT NULL COMMENT '租客ID',
    house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    amount INT NOT NULL COMMENT '押金总额，单位：分',
    withheld_amount INT NOT NULL DEFAULT 0 COMMENT '已扣款金额，单位：分',
    refunded_amount INT NOT NULL DEFAULT 0 COMMENT '已退款金额，单位：分',
    status VARCHAR(20) NOT NULL DEFAULT 'held' COMMENT 'held托管中/deducted已扣款/refunding退款中/refunded已退款',
    payment_record_id VARCHAR(36) COMMENT '关联的原始支付记录ID',
    refund_payment_record_id VARCHAR(36) COMMENT '退款支付记录ID',
    refund_channel VARCHAR(20) COMMENT '退款渠道',
    refund_trade_no VARCHAR(128) COMMENT '退款交易号',
    settlement_detail JSON COMMENT '结算明细JSON',
    terminated_at DATETIME COMMENT '退租完成时间',
    refunded_at DATETIME COMMENT '退款完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_deposit_lease (lease_id),
    INDEX idx_deposit_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='押金记录表';

CREATE TABLE deposit_deduction (
    id VARCHAR(36) PRIMARY KEY,
    deposit_record_id VARCHAR(36) NOT NULL COMMENT '押金记录ID',
    deduction_type VARCHAR(30) NOT NULL COMMENT '扣款类型：damage损坏/cleaning清洁/rent_arrears欠租/bill_arrears欠费/other其他',
    amount INT NOT NULL COMMENT '扣款金额，单位：分',
    description VARCHAR(500) COMMENT '扣款说明',
    evidence_urls JSON COMMENT '凭证图片URL列表',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_deposit_deduction_record (deposit_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='押金扣款明细表';
