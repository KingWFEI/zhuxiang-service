-- house.landlord_id 已统一为 user.id，修复历史租单与合同中的房东关联和实名快照。

UPDATE rent_order ro
INNER JOIN house h ON CAST(h.id AS BINARY) = CAST(ro.house_id AS BINARY)
SET ro.lessor_user_id = h.landlord_id,
    ro.updated_at = CURRENT_TIMESTAMP
WHERE ro.lessor_user_id IS NULL
   OR CAST(ro.lessor_user_id AS BINARY) <> CAST(h.landlord_id AS BINARY);

UPDATE rent_contract rc
INNER JOIN rent_order ro ON CAST(ro.id AS BINARY) = CAST(rc.order_id AS BINARY)
INNER JOIN user_real_name_auth auth
        ON CAST(auth.user_id AS BINARY) = CAST(ro.lessor_user_id AS BINARY)
       AND auth.auth_status = 'VERIFIED'
SET rc.landlord_name = auth.real_name,
    rc.landlord_phone = auth.account_mobile,
    rc.landlord_id_card_ciphertext = auth.id_card_ciphertext,
    rc.updated_at = CURRENT_TIMESTAMP
WHERE rc.landlord_name IS NULL OR rc.landlord_name = ''
   OR rc.landlord_phone IS NULL OR rc.landlord_phone = ''
   OR rc.landlord_id_card_ciphertext IS NULL OR rc.landlord_id_card_ciphertext = '';
