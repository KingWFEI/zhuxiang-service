ALTER TABLE rent_order ADD COLUMN payment_months INT NOT NULL DEFAULT 1 AFTER payment_method;

ALTER TABLE rent_contract
    ADD COLUMN payment_months INT NOT NULL DEFAULT 1 AFTER service_fee,
    ADD COLUMN first_payment_amount INT NOT NULL DEFAULT 0 AFTER payment_months;
