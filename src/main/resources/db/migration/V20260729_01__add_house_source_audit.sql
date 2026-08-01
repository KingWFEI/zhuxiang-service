ALTER TABLE house
    ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'PLATFORM'
        COMMENT '房源发布来源：LANDLORD房东发布，PLATFORM平台自营'
        AFTER landlord_id,
    ADD COLUMN created_by VARCHAR(36) NULL
        COMMENT '创建房源的用户ID；历史平台房源可能为空'
        AFTER source_type;

-- 房产证明材料只由房东端上传，可据此可靠识别已有的房东发布记录。
UPDATE house h
SET h.source_type = 'LANDLORD',
    h.created_by = h.landlord_id
WHERE EXISTS (
    SELECT 1
    FROM house_property_certificate certificate
    WHERE certificate.house_id = h.id
);

CREATE INDEX idx_house_source_type ON house (source_type);
