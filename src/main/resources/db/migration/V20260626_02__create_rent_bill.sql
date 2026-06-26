CREATE TABLE rent_bill (
    id VARCHAR(36) PRIMARY KEY,
    lease_id VARCHAR(36) NOT NULL,
    period_no INT NOT NULL COMMENT '第几期，从1开始',
    amount_due INT NOT NULL COMMENT '应缴金额，单位：分',
    amount_paid INT NOT NULL DEFAULT 0 COMMENT '已缴金额，单位：分',
    due_date DATE NOT NULL COMMENT '应缴日期',
    paid_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/paid/overdue/cancelled',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_rent_bill_lease (lease_id),
    INDEX idx_rent_bill_due (lease_id, status, due_date)
);
