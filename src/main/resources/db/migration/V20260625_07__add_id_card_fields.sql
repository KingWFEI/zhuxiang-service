CREATE TABLE file_record (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    url VARCHAR(500) NOT NULL,
    biz_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_file_record_user (user_id, biz_type)
);

ALTER TABLE rent_contract
    ADD COLUMN id_card_front_url VARCHAR(512) AFTER first_payment_amount,
    ADD COLUMN id_card_back_url VARCHAR(512) AFTER id_card_front_url;
