ALTER TABLE appointment
    ADD COLUMN active_user_house_key VARCHAR(100) NULL
        COMMENT '同一用户同一房源的活跃预约唯一键' AFTER active_slot_key,
    ADD UNIQUE KEY uk_appointment_active_user_house (active_user_house_key);

