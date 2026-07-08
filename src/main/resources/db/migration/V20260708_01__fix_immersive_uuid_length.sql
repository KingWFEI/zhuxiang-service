-- 修复沉浸式看房所有表：UUID字段从VARCHAR(32)改为VARCHAR(36)
-- UUID标准格式含4个连字符，共36位

ALTER TABLE immersive_tour MODIFY COLUMN id VARCHAR(36) NOT NULL COMMENT '项目ID';
ALTER TABLE immersive_tour MODIFY COLUMN house_id VARCHAR(36) NOT NULL COMMENT '房源ID';
ALTER TABLE immersive_tour MODIFY COLUMN entry_scene_id VARCHAR(36) NULL COMMENT '入口房间ID';

ALTER TABLE immersive_scene MODIFY COLUMN id VARCHAR(36) NOT NULL COMMENT '场景ID';
ALTER TABLE immersive_scene MODIFY COLUMN tour_id VARCHAR(36) NOT NULL COMMENT '所属项目ID';
ALTER TABLE immersive_scene MODIFY COLUMN entry_image_id VARCHAR(36) NULL COMMENT '入口图片ID';

ALTER TABLE immersive_scene_image MODIFY COLUMN id VARCHAR(36) NOT NULL COMMENT '图片ID';
ALTER TABLE immersive_scene_image MODIFY COLUMN scene_id VARCHAR(36) NOT NULL COMMENT '所属场景ID';

ALTER TABLE immersive_image_hotspot MODIFY COLUMN id VARCHAR(36) NOT NULL COMMENT '热点ID';
ALTER TABLE immersive_image_hotspot MODIFY COLUMN source_image_id VARCHAR(36) NOT NULL COMMENT '源图片ID';
ALTER TABLE immersive_image_hotspot MODIFY COLUMN target_scene_id VARCHAR(36) NOT NULL COMMENT '目标房间ID';
ALTER TABLE immersive_image_hotspot MODIFY COLUMN target_image_id VARCHAR(36) NULL COMMENT '目标图片ID';
