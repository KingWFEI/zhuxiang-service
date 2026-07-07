-- 修复沉浸式表 user ID 列宽：zhuxiang-service 使用 36 位 UUID
ALTER TABLE immersive_tour MODIFY COLUMN created_by VARCHAR(36) NOT NULL COMMENT '创建人ID';
ALTER TABLE immersive_tour MODIFY COLUMN updated_by VARCHAR(36) NULL COMMENT '更新人ID';
