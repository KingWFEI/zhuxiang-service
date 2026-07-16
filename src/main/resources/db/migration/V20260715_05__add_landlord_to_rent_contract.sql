ALTER TABLE rent_contract
    ADD COLUMN landlord_name VARCHAR(100) NULL COMMENT '房东姓名' AFTER tenant_id_card,
    ADD COLUMN landlord_phone VARCHAR(32) NULL COMMENT '房东电话' AFTER landlord_name,
    ADD COLUMN landlord_id_card TEXT NULL COMMENT '房东身份证号' AFTER landlord_phone;
