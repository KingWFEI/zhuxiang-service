ALTER TABLE house_location
    ADD COLUMN township VARCHAR(50) NOT NULL DEFAULT '' COMMENT '街道/镇' AFTER district,
    ADD COLUMN neighborhood VARCHAR(100) NOT NULL DEFAULT '' COMMENT '社区/小区名称' AFTER township;
