ALTER TABLE lease
    ADD COLUMN lease_months INT NOT NULL DEFAULT 1 AFTER end_date,
    ADD COLUMN payment_method VARCHAR(30) AFTER lease_months,
    ADD COLUMN payment_months INT NOT NULL DEFAULT 1 AFTER payment_method,
    ADD COLUMN service_fee INT NOT NULL DEFAULT 0 AFTER deposit,
    ADD COLUMN first_payment_amount INT NOT NULL DEFAULT 0 AFTER service_fee;
