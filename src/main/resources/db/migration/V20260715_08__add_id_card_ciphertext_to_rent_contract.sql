-- 合同表：身份证密文列（替代旧明文字段）
-- 旧明文字段 tenant_id_card / landlord_id_card 在迁移完成后应通过后续脚本清空
ALTER TABLE rent_contract
    ADD COLUMN tenant_id_card_ciphertext   TEXT NULL COMMENT '租户身份证号密文（AES-256-GCM）' AFTER tenant_id_card,
    ADD COLUMN landlord_id_card_ciphertext TEXT NULL COMMENT '房东身份证号密文（AES-256-GCM）' AFTER landlord_id_card;
