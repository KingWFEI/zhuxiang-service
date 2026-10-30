ALTER TABLE `region`
    MODIFY COLUMN `level` varchar(30) NOT NULL
        COMMENT '层级：province省份/city城市/district区县/business_area商圈';
