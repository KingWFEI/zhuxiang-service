CREATE TABLE house_location (
    id VARCHAR(36) PRIMARY KEY COMMENT '主键ID',
    house_id VARCHAR(36) NOT NULL COMMENT '房源ID',
    longitude DECIMAL(10, 7) NOT NULL COMMENT '经度',
    latitude DECIMAL(10, 7) NOT NULL COMMENT '纬度',
    province VARCHAR(50) NOT NULL DEFAULT '' COMMENT '省份/直辖市',
    city VARCHAR(50) NOT NULL DEFAULT '' COMMENT '城市',
    district VARCHAR(50) NOT NULL DEFAULT '' COMMENT '区县',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_house_location_house UNIQUE (house_id)
) COMMENT='房源位置信息表';
