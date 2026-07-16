ALTER TABLE landlord
    ADD COLUMN id_card_ciphertext TEXT NULL COMMENT '身份证号密文（AES-256-GCM）' AFTER phone;
