ALTER TABLE house_location
    ADD COLUMN address VARCHAR(500) NOT NULL DEFAULT '' COMMENT '高德返回的格式化地址' AFTER neighborhood;
