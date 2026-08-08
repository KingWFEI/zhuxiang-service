ALTER TABLE house
    ADD COLUMN rent_mode VARCHAR(20) NULL COMMENT '出租方式：WHOLE_RENT整租，SHARED_RENT合租' AFTER description;

UPDATE house
SET rent_mode = CASE
        WHEN rent_type IN ('SHARED_RENT', 'shared', '合租') THEN 'SHARED_RENT'
        ELSE 'WHOLE_RENT'
    END,
    rent_type = CASE
        WHEN rent_type IN ('SHORT_RENT', 'short_rent') THEN 'SHORT_RENT'
        WHEN rent_type IN ('HOMESTAY', 'homestay') THEN 'HOMESTAY'
        ELSE 'LONG_RENT'
    END;

ALTER TABLE house
    MODIFY COLUMN rent_mode VARCHAR(20) NOT NULL COMMENT '出租方式：WHOLE_RENT整租，SHARED_RENT合租',
    MODIFY COLUMN rent_type VARCHAR(20) NOT NULL COMMENT '租赁类型：LONG_RENT长租，SHORT_RENT短租，HOMESTAY民宿';

DROP INDEX idx_house_status_type ON house;
CREATE INDEX idx_house_status_type_mode ON house (status, rent_type, rent_mode);
