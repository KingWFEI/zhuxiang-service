ALTER TABLE community
    ADD COLUMN normalized_name VARCHAR(100) COMMENT '标准化名称（去空格去括号，用于去重）',
    ADD COLUMN province VARCHAR(50) COMMENT '省/直辖市',
    ADD COLUMN city VARCHAR(50) COMMENT '市',
    ADD COLUMN district VARCHAR(50) COMMENT '区/县',
    ADD COLUMN ad_code VARCHAR(20) COMMENT '高德行政区划代码',
    ADD COLUMN coordinate_system VARCHAR(20) DEFAULT 'GCJ02' COMMENT '坐标系：GCJ02/WGS84',
    ADD COLUMN map_provider VARCHAR(20) COMMENT '地图供应商：amap/tencent/baidu',
    ADD COLUMN external_poi_id VARCHAR(64) COMMENT '地图平台POI ID',
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'approved' COMMENT 'pending待审核/approved已通过/merged已合并',
    ADD UNIQUE INDEX uk_provider_poi (map_provider, external_poi_id),
    ADD INDEX idx_community_normalized (normalized_name, ad_code);

-- 补全已有小区的省市区和标准化名称
UPDATE community c
    JOIN region r ON c.region_id = r.id
SET c.district = r.name
WHERE c.district IS NULL;

UPDATE community
SET normalized_name = REPLACE(REPLACE(REPLACE(name, ' ', ''), '（', '('), '）', ')')
WHERE normalized_name IS NULL;
