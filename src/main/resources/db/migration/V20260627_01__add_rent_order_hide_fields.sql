ALTER TABLE rent_order
    ADD COLUMN user_hidden TINYINT NOT NULL DEFAULT 0 AFTER cancelled_at,
    ADD COLUMN hidden_at TIMESTAMP NULL AFTER user_hidden;
