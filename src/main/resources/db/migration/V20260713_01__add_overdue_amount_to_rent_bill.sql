ALTER TABLE rent_bill
    ADD COLUMN overdue_amount INT NOT NULL DEFAULT 0 COMMENT '滞纳金，单位：分' AFTER amount_paid;
